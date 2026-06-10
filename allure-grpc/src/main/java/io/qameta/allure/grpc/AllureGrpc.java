/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import io.qameta.allure.http.HttpExchangeSerializer;
import io.qameta.allure.http.HttpExchangeStream;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Captures gRPC client calls as Allure attachments.
 *
 * <p>Attach this interceptor to a gRPC channel or stub to record request messages, response messages, metadata,
 * and call status as a structured HTTP exchange attachment.</p>
 */
@SuppressWarnings(
    {
            "checkstyle:ClassFanOutComplexity",
            "checkstyle:AnonInnerLength",
            "checkstyle:JavaNCSS",
            "PMD.GodClass"
    }
)
public class AllureGrpc implements ClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureGrpc.class);
    private static final String ATTACHMENT_NAME = "gRPC exchange";
    private static final String GRPC_CONTENT_TYPE = "application/grpc";
    private static final String GRPC_JSON_CONTENT_TYPE = "application/grpc+json";
    private static final String GRPC_STATUS = "grpc-status";
    private static final String GRPC_MESSAGE = "grpc-message";
    private static final String CONTENT_TYPE_HEADER = "content-type";
    private static final String HTTP_METHOD = "POST";
    private static final String HTTP_VERSION = "HTTP/2";
    private static final String PATH_SEPARATOR = "/";
    private static final String UNKNOWN = "unknown";
    private static final JsonFormat.Printer GRPC_TO_JSON_PRINTER = JsonFormat.printer();

    private final AllureLifecycle lifecycle;
    private final boolean markStepFailedOnNonZeroCode;
    private final boolean interceptResponseMetadata;
    private final Consumer<HttpExchange.Builder> exchangeCustomizer;

    /**
     * Creates an Allure grpc with default configuration.
     */
    public AllureGrpc() {
        this(Allure.getLifecycle(), true, false);
    }

    /**
     * Creates an Allure grpc with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     * @param markStepFailedOnNonZeroCode the mark step failed on non zero code
     * @param interceptResponseMetadata the intercept response metadata
     */
    public AllureGrpc(
                      final AllureLifecycle lifecycle,
                      final boolean markStepFailedOnNonZeroCode,
                      final boolean interceptResponseMetadata) {
        this(lifecycle, markStepFailedOnNonZeroCode, interceptResponseMetadata, builder -> {
        });
    }

    /**
     * Creates an Allure grpc with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     * @param markStepFailedOnNonZeroCode the mark step failed on non zero code
     * @param interceptResponseMetadata the intercept response metadata
     * @param exchangeCustomizer the HTTP exchange builder customizer
     */
    public AllureGrpc(
                      final AllureLifecycle lifecycle,
                      final boolean markStepFailedOnNonZeroCode,
                      final boolean interceptResponseMetadata,
                      final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.lifecycle = lifecycle;
        this.markStepFailedOnNonZeroCode = markStepFailedOnNonZeroCode;
        this.interceptResponseMetadata = interceptResponseMetadata;
        this.exchangeCustomizer = exchangeCustomizer == null ? builder -> {
        } : exchangeCustomizer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> ClientCall<T, R> interceptCall(
                                                 final MethodDescriptor<T, R> methodDescriptor,
                                                 final CallOptions callOptions,
                                                 final Channel nextChannel) {
        final Channel channel = Objects.requireNonNull(nextChannel, "nextChannel must not be null");
        final AllureLifecycle current = lifecycle;
        final String parent = current.getCurrentTestCase().orElse(null);
        final String stepUuid = UUID.randomUUID().toString();
        final long start = System.currentTimeMillis();
        final List<String> clientMessages = new ArrayList<>();
        final List<String> serverMessages = new ArrayList<>();
        final Map<String, String> initialHeaders = new LinkedHashMap<>();
        final Map<String, String> trailers = new LinkedHashMap<>();
        final String authority = channel.authority();

        final String stepName = buildStepName(channel, methodDescriptor);
        if (parent != null) {
            current.startStep(parent, stepUuid, new StepResult().setName(stepName));
        } else {
            current.startStep(stepUuid, new StepResult().setName(stepName));
        }

        final StepContext<T, R> stepContext = new StepContext<>(
                stepUuid, methodDescriptor, current, clientMessages,
                serverMessages, initialHeaders, trailers, authority, start
        );

        return new ForwardingClientCall.SimpleForwardingClientCall<T, R>(
                channel.newCall(methodDescriptor, callOptions)
        ) {
            @Override
            public void start(final Listener<R> responseListener, final Metadata requestHeaders) {
                final Listener<R> forwardingListener = new ForwardingClientCallListener<R>() {
                    @Override
                    protected Listener<R> delegate() {
                        return responseListener;
                    }

                    @Override
                    public void onHeaders(final Metadata headers) {
                        handleHeaders(headers, stepContext.getInitialHeaders());
                        super.onHeaders(headers);
                    }

                    @Override
                    public void onMessage(final R message) {
                        handleServerMessage(message, stepContext.getServerMessages());
                        super.onMessage(message);
                    }

                    @Override
                    public void onClose(final io.grpc.Status status, final Metadata responseTrailers) {
                        handleClose(status, responseTrailers, stepContext);
                        super.onClose(status, responseTrailers);
                    }
                };
                super.start(forwardingListener, requestHeaders);
            }

            @Override
            public void sendMessage(final T message) {
                handleClientMessage(message, stepContext.getClientMessages());
                super.sendMessage(message);
            }
        };
    }

    private void handleClose(
                             final io.grpc.Status status,
                             final Metadata responseTrailers,
                             final StepContext<?, ?> stepContext) {
        try {
            if (interceptResponseMetadata && responseTrailers != null) {
                copyAsciiResponseMetadata(responseTrailers, stepContext.getTrailers());
            }
            attachExchange(stepContext, status);
            stepContext.getLifecycle().updateStep(
                    stepContext.getStepUuid(),
                    step -> step.setStatus(convertStatus(status))
            );
        } catch (Throwable throwable) {
            LOGGER.error("Failed to finalize Allure step for gRPC call", throwable);
            stepContext.getLifecycle().updateStep(
                    stepContext.getStepUuid(),
                    step -> step.setStatus(Status.BROKEN)
            );
        } finally {
            stopStepSafely(stepContext.getLifecycle(), stepContext.getStepUuid());
        }
    }

    private void handleHeaders(final Metadata headers, final Map<String, String> destination) {
        try {
            if (interceptResponseMetadata && headers != null) {
                copyAsciiResponseMetadata(headers, destination);
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to capture response headers", throwable);
        }
    }

    private <T> void handleClientMessage(final T message, final List<String> destination) {
        try {
            destination.add(GRPC_TO_JSON_PRINTER.print((MessageOrBuilder) message));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Could not serialize gRPC request message to JSON", e);
        } catch (Throwable throwable) {
            LOGGER.error("Unexpected error while serializing gRPC request message", throwable);
        }
    }

    private <R> void handleServerMessage(final R message, final List<String> destination) {
        try {
            destination.add(GRPC_TO_JSON_PRINTER.print((MessageOrBuilder) message));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Could not serialize gRPC response message to JSON", e);
        } catch (Throwable throwable) {
            LOGGER.error("Unexpected error while serializing gRPC response message", throwable);
        }
    }

    private void attachExchange(final StepContext<?, ?> stepContext, final io.grpc.Status status) {
        final HttpExchangeRequest request = buildRequest(
                stepContext.getMethodDescriptor(),
                stepContext.getClientMessages(),
                stepContext.getAuthority()
        );
        final HttpExchangeResponse response = buildResponse(
                stepContext.getMethodDescriptor(),
                stepContext.getServerMessages(),
                status,
                stepContext.getInitialHeaders(),
                stepContext.getTrailers()
        );
        final HttpExchange exchange = exchangeBuilder(request)
                .setResponse(response)
                .setStart(stepContext.getStart())
                .setStop(System.currentTimeMillis())
                .build();
        addHttpExchangeToStep(stepContext.getStepUuid(), ATTACHMENT_NAME, exchange, stepContext.getLifecycle());
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    private HttpExchangeRequest buildRequest(
                                             final MethodDescriptor<?, ?> methodDescriptor,
                                             final List<String> clientMessages,
                                             final String authority) {
        final HttpExchangeRequest.Builder builder = HttpExchangeRequest.builder(
                HTTP_METHOD,
                PATH_SEPARATOR + methodDescriptor.getFullMethodName()
        )
                .setHttpVersion(HTTP_VERSION)
                .addHeader(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE)
                .addHeader("te", "trailers");
        if (authority != null) {
            builder.addHeader(":authority", authority);
        }
        return builder
                .setBody(toHttpBody(clientMessages, isRequestStreaming(methodDescriptor.getType())))
                .build();
    }

    private HttpExchangeResponse buildResponse(
                                               final MethodDescriptor<?, ?> methodDescriptor,
                                               final List<String> serverMessages,
                                               final io.grpc.Status status,
                                               final Map<String, String> initialHeaders,
                                               final Map<String, String> trailers) {
        final Map<String, String> responseHeaders = new LinkedHashMap<>();
        responseHeaders.put(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);
        if (interceptResponseMetadata) {
            responseHeaders.putAll(initialHeaders);
        }

        final Map<String, String> responseTrailers = new LinkedHashMap<>();
        if (interceptResponseMetadata) {
            responseTrailers.putAll(trailers);
        }
        responseTrailers.putIfAbsent(GRPC_STATUS, String.valueOf(status.getCode().value()));
        responseTrailers.putIfAbsent(GRPC_MESSAGE, status.getDescription() == null ? "" : status.getDescription());

        final HttpExchangeResponse.Builder builder = HttpExchangeResponse.builder()
                .setStatus(200)
                .setHttpVersion(HTTP_VERSION)
                .addHeaders(toNameValues(responseHeaders))
                .setBody(toHttpBody(serverMessages, isResponseStreaming(methodDescriptor.getType())));
        responseTrailers.forEach(builder::addTrailer);
        return builder.build();
    }

    private void addHttpExchangeToStep(
                                       final String stepUuid,
                                       final String attachmentName,
                                       final HttpExchange exchange,
                                       final AllureLifecycle lifecycle) {
        final String source = UUID.randomUUID() + HttpExchange.FILE_EXTENSION;
        lifecycle.updateStep(
                stepUuid,
                step -> step.getAttachments().add(
                        new Attachment()
                                .setName(attachmentName)
                                .setSource(source)
                                .setType(HttpExchange.CONTENT_TYPE)
                )
        );
        lifecycle.writeAttachment(
                source,
                new ByteArrayInputStream(HttpExchangeSerializer.toJsonBytes(exchange))
        );
    }

    private void stopStepSafely(final AllureLifecycle lifecycle, final String stepUuid) {
        try {
            lifecycle.stopStep(stepUuid);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to stop Allure step {}", stepUuid, throwable);
        }
    }

    private Status convertStatus(final io.grpc.Status grpcStatus) {
        if (grpcStatus.isOk() || !markStepFailedOnNonZeroCode) {
            return Status.PASSED;
        }
        return Status.FAILED;
    }

    private static String buildStepName(
                                        final Channel channel,
                                        final MethodDescriptor<?, ?> methodDescriptor) {
        final String authority = channel != null ? channel.authority() : null;
        final String safeAuthority = authority != null ? authority : UNKNOWN;
        final String type = toSnakeCase(methodDescriptor.getType());
        return "Send " + type + " gRPC request to "
                + safeAuthority + PATH_SEPARATOR + methodDescriptor.getFullMethodName();
    }

    private static String toSnakeCase(final MethodDescriptor.MethodType methodType) {
        if (methodType == null) {
            return UNKNOWN;
        }
        return methodType.name().toLowerCase(Locale.ROOT);
    }

    private static String toJsonBody(final List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        final String joined = String.join(",\n", items);
        return "[" + joined + "]";
    }

    private static HttpExchangeBody toHttpBody(final List<String> messages, final boolean streamingMethod) {
        final String body = toJsonBody(messages);
        final boolean stream = streamingMethod || messages != null && messages.size() > 1;
        if (body == null && !stream) {
            return null;
        }
        final Long size = body == null ? null : (long) body.getBytes(StandardCharsets.UTF_8).length;
        final HttpExchangeStream streamMetadata = stream
                ? new HttpExchangeStream("grpc", true, messages == null ? 0L : (long) messages.size())
                : null;
        return new HttpExchangeBody(
                GRPC_JSON_CONTENT_TYPE,
                "utf8",
                body,
                size,
                false,
                null,
                null,
                streamMetadata
        );
    }

    private static List<HttpExchangeNameValue> toNameValues(final Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> new HttpExchangeNameValue(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static boolean isRequestStreaming(final MethodDescriptor.MethodType methodType) {
        return methodType == MethodDescriptor.MethodType.CLIENT_STREAMING
                || methodType == MethodDescriptor.MethodType.BIDI_STREAMING;
    }

    private static boolean isResponseStreaming(final MethodDescriptor.MethodType methodType) {
        return methodType == MethodDescriptor.MethodType.SERVER_STREAMING
                || methodType == MethodDescriptor.MethodType.BIDI_STREAMING;
    }

    private static void copyAsciiResponseMetadata(
                                                  final Metadata source,
                                                  final Map<String, String> target) {
        for (String key : source.keys()) {
            if (key == null) {
                continue;
            }
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                continue;
            }
            final Metadata.Key<String> keyAscii = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            final String value = source.get(keyAscii);
            if (value != null) {
                target.put(key, value);
            }
        }
    }

    private static final class StepContext<T, R> {
        private final String stepUuid;
        private final MethodDescriptor<T, R> methodDescriptor;
        private final AllureLifecycle lifecycle;
        private final List<String> clientMessages;
        private final List<String> serverMessages;
        private final Map<String, String> initialHeaders;
        private final Map<String, String> trailers;
        private final String authority;
        private final long start;

        StepContext(
                    final String stepUuid,
                    final MethodDescriptor<T, R> methodDescriptor,
                    final AllureLifecycle lifecycle,
                    final List<String> clientMessages,
                    final List<String> serverMessages,
                    final Map<String, String> initialHeaders,
                    final Map<String, String> trailers,
                    final String authority,
                    final long start) {
            this.stepUuid = stepUuid;
            this.methodDescriptor = methodDescriptor;
            this.lifecycle = lifecycle;
            this.clientMessages = clientMessages;
            this.serverMessages = serverMessages;
            this.initialHeaders = initialHeaders;
            this.trailers = trailers;
            this.authority = authority;
            this.start = start;
        }

        String getStepUuid() {
            return stepUuid;
        }

        MethodDescriptor<T, R> getMethodDescriptor() {
            return methodDescriptor;
        }

        AllureLifecycle getLifecycle() {
            return lifecycle;
        }

        List<String> getClientMessages() {
            return clientMessages;
        }

        List<String> getServerMessages() {
            return serverMessages;
        }

        Map<String, String> getInitialHeaders() {
            return initialHeaders;
        }

        Map<String, String> getTrailers() {
            return trailers;
        }

        String getAuthority() {
            return authority;
        }

        long getStart() {
            return start;
        }
    }
}
