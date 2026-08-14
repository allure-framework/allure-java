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
import io.grpc.Attributes;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientStreamTracer;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeCookie;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import io.qameta.allure.http.HttpExchangeSerializer;
import io.qameta.allure.http.HttpExchangeStream;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
            "PMD.GodClass",
            "PMD.TooManyMethods"
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
    private static final String COOKIE_HEADER = "cookie";
    private static final String SET_COOKIE_HEADER = "set-cookie";
    private static final String TE_HEADER = "te";
    private static final String AUTHORITY_HEADER = ":authority";
    private static final String COOKIE_SEPARATOR = ";";
    private static final String HTTP_METHOD = "POST";
    private static final String HTTP_VERSION = "HTTP/2";
    private static final String PATH_SEPARATOR = "/";
    private static final String UNKNOWN = "unknown";
    private static final JsonFormat.Printer GRPC_TO_JSON_PRINTER = JsonFormat.printer();

    private final AllureLifecycle lifecycle;
    private final boolean markStepFailedOnNonZeroCode;
    private final boolean captureRequestMetadata;
    private final boolean captureResponseMetadata;
    private final Consumer<HttpExchange.Builder> exchangeCustomizer;

    /**
     * Creates an Allure gRPC interceptor that captures request and response metadata.
     */
    public AllureGrpc() {
        this(builder());
    }

    /**
     * Creates an Allure gRPC builder.
     *
     * @return a builder configured to capture request and response metadata
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an Allure grpc with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     * @param markStepFailedOnNonZeroCode the mark step failed on non zero code
     * @param interceptResponseMetadata whether to capture response metadata; request metadata is captured
     */
    public AllureGrpc(
                      final AllureLifecycle lifecycle,
                      final boolean markStepFailedOnNonZeroCode,
                      final boolean interceptResponseMetadata) {
        this(lifecycle, markStepFailedOnNonZeroCode, true, interceptResponseMetadata, builder -> {
        });
    }

    /**
     * Creates an Allure grpc with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     * @param markStepFailedOnNonZeroCode the mark step failed on non zero code
     * @param interceptResponseMetadata whether to capture response metadata; request metadata is captured
     * @param exchangeCustomizer the HTTP exchange builder customizer
     */
    public AllureGrpc(
                      final AllureLifecycle lifecycle,
                      final boolean markStepFailedOnNonZeroCode,
                      final boolean interceptResponseMetadata,
                      final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this(lifecycle, markStepFailedOnNonZeroCode, true, interceptResponseMetadata, exchangeCustomizer);
    }

    private AllureGrpc(final Builder builder) {
        this(
                builder.lifecycle,
                builder.markStepFailedOnNonZeroCode,
                builder.captureRequestMetadata,
                builder.captureResponseMetadata,
                builder.exchangeCustomizer
        );
    }

    private AllureGrpc(
                       final AllureLifecycle lifecycle,
                       final boolean markStepFailedOnNonZeroCode,
                       final boolean captureRequestMetadata,
                       final boolean captureResponseMetadata,
                       final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.lifecycle = lifecycle;
        this.markStepFailedOnNonZeroCode = markStepFailedOnNonZeroCode;
        this.captureRequestMetadata = captureRequestMetadata;
        this.captureResponseMetadata = captureResponseMetadata;
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
        final Optional<AllureExternalKey> parent = current.getCurrentExecutableKey();
        if (parent.isEmpty()) {
            // no Allure executable is running on this thread — nothing to attach the call to
            return channel.newCall(methodDescriptor, callOptions);
        }
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureGrpc.class);
        final long start = System.currentTimeMillis();
        final List<String> clientMessages = new ArrayList<>();
        final List<String> serverMessages = new ArrayList<>();
        final List<HttpExchangeNameValue> initialHeaders = new ArrayList<>();
        final List<HttpExchangeNameValue> trailers = new ArrayList<>();
        final String authority = channel.authority();

        final String stepName = buildStepName(channel, methodDescriptor);
        // pure manual linkage under the captured parent: it does not bind the current thread, so the step can be
        // finalized by key from the gRPC onClose callback on any thread
        current.startStep(parent.get(), stepKey, new StepResult().setName(stepName));

        final StepContext<T, R> stepContext = new StepContext<>(
                stepKey, methodDescriptor, current, clientMessages,
                serverMessages, initialHeaders, trailers, authority, start
        );

        final CallOptions effectiveCallOptions = captureRequestMetadata
                ? callOptions.withStreamTracerFactory(requestMetadataTracer(stepContext))
                : callOptions;

        return new ForwardingClientCall.SimpleForwardingClientCall<T, R>(
                channel.newCall(methodDescriptor, effectiveCallOptions)
        ) {
            @Override
            public void start(final Listener<R> responseListener, final Metadata requestHeaders) {
                handleRequestHeaders(requestHeaders, stepContext);
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
            if (captureResponseMetadata && responseTrailers != null) {
                stepContext.getTrailers().addAll(copyMetadata(responseTrailers));
            }
            attachExchange(stepContext, status);
            stepContext.getLifecycle().updateStep(
                    stepContext.getStepKey(),
                    step -> step.setStatus(convertStatus(status))
            );
        } catch (Throwable throwable) {
            LOGGER.error("Failed to finalize Allure step for gRPC call", throwable);
            stepContext.getLifecycle().updateStep(
                    stepContext.getStepKey(),
                    step -> step.setStatus(Status.BROKEN)
            );
        } finally {
            stopStepSafely(stepContext.getLifecycle(), stepContext.getStepKey());
        }
    }

    private ClientStreamTracer.Factory requestMetadataTracer(final StepContext<?, ?> stepContext) {
        return new ClientStreamTracer.Factory() {
            @Override
            public ClientStreamTracer newClientStreamTracer(
                                                            final ClientStreamTracer.StreamInfo info,
                                                            final Metadata headers) {
                handleRequestHeaders(headers, stepContext);
                return new ClientStreamTracer() {
                    @Override
                    public void streamCreated(final Attributes transportAttrs, final Metadata streamHeaders) {
                        handleRequestHeaders(streamHeaders, stepContext);
                    }

                    @Override
                    public void streamClosed(final io.grpc.Status status) {
                        handleRequestHeaders(headers, stepContext);
                    }
                };
            }
        };
    }

    private void handleRequestHeaders(final Metadata headers, final StepContext<?, ?> stepContext) {
        try {
            if (captureRequestMetadata && headers != null) {
                stepContext.setRequestHeaders(copyMetadata(headers));
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to capture request metadata", throwable);
        }
    }

    private void handleHeaders(final Metadata headers, final List<HttpExchangeNameValue> destination) {
        try {
            if (captureResponseMetadata && headers != null) {
                destination.addAll(copyMetadata(headers));
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to capture response metadata", throwable);
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
                stepContext.getRequestHeaders(),
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
        addHttpExchangeToStep(stepContext.getStepKey(), ATTACHMENT_NAME, exchange, stepContext.getLifecycle());
    }

    private HttpExchange.Builder exchangeBuilder(final HttpExchangeRequest request) {
        final HttpExchange.Builder builder = HttpExchange.builder(request);
        exchangeCustomizer.accept(builder);
        return builder;
    }

    private HttpExchangeRequest buildRequest(
                                             final MethodDescriptor<?, ?> methodDescriptor,
                                             final List<String> clientMessages,
                                             final List<HttpExchangeNameValue> requestHeaders,
                                             final String authority) {
        final HttpExchangeRequest.Builder builder = HttpExchangeRequest.builder(
                HTTP_METHOD,
                PATH_SEPARATOR + methodDescriptor.getFullMethodName()
        )
                .setHttpVersion(HTTP_VERSION);
        if (!containsName(requestHeaders, CONTENT_TYPE_HEADER)) {
            builder.addHeader(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);
        }
        if (!containsName(requestHeaders, TE_HEADER)) {
            builder.addHeader(TE_HEADER, "trailers");
        }
        if (authority != null && !containsName(requestHeaders, AUTHORITY_HEADER)) {
            builder.addHeader(AUTHORITY_HEADER, authority);
        }
        if (captureRequestMetadata) {
            builder.addHeaders(withoutName(requestHeaders, COOKIE_HEADER));
            builder.addCookies(extractRequestCookies(requestHeaders));
        }
        return builder
                .setBody(toHttpBody(clientMessages, isRequestStreaming(methodDescriptor.getType())))
                .build();
    }

    private HttpExchangeResponse buildResponse(
                                               final MethodDescriptor<?, ?> methodDescriptor,
                                               final List<String> serverMessages,
                                               final io.grpc.Status status,
                                               final List<HttpExchangeNameValue> initialHeaders,
                                               final List<HttpExchangeNameValue> trailers) {
        final HttpExchangeResponse.Builder builder = HttpExchangeResponse.builder()
                .setStatus(200)
                .setHttpVersion(HTTP_VERSION)
                .setBody(toHttpBody(serverMessages, isResponseStreaming(methodDescriptor.getType())));
        if (!containsName(initialHeaders, CONTENT_TYPE_HEADER)) {
            builder.addHeader(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);
        }
        if (captureResponseMetadata) {
            final List<HttpExchangeCookie> cookies = new ArrayList<>(extractResponseCookies(initialHeaders));
            cookies.addAll(extractResponseCookies(trailers));
            builder.addCookies(cookies);
            builder.addHeaders(withoutName(initialHeaders, SET_COOKIE_HEADER));
            withoutName(trailers, SET_COOKIE_HEADER)
                    .forEach(trailer -> builder.addTrailer(trailer.name(), trailer.value()));
        }
        if (!containsName(trailers, GRPC_STATUS)) {
            builder.addTrailer(GRPC_STATUS, String.valueOf(status.getCode().value()));
        }
        if (!containsName(trailers, GRPC_MESSAGE)) {
            builder.addTrailer(GRPC_MESSAGE, status.getDescription() == null ? "" : status.getDescription());
        }
        return builder.build();
    }

    private void addHttpExchangeToStep(
                                       final AllureExternalKey stepKey,
                                       final String attachmentName,
                                       final HttpExchange exchange,
                                       final AllureLifecycle lifecycle) {
        lifecycle.addAttachment(
                stepKey,
                attachmentName,
                HttpExchange.CONTENT_TYPE,
                new ByteArrayInputStream(HttpExchangeSerializer.toJsonBytes(exchange)),
                AttachmentOptions.empty()
        );
    }

    private void stopStepSafely(final AllureLifecycle lifecycle, final AllureExternalKey stepKey) {
        try {
            lifecycle.stopStep(stepKey);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to stop Allure step {}", stepKey, throwable);
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

    private static boolean isRequestStreaming(final MethodDescriptor.MethodType methodType) {
        return methodType == MethodDescriptor.MethodType.CLIENT_STREAMING
                || methodType == MethodDescriptor.MethodType.BIDI_STREAMING;
    }

    private static boolean isResponseStreaming(final MethodDescriptor.MethodType methodType) {
        return methodType == MethodDescriptor.MethodType.SERVER_STREAMING
                || methodType == MethodDescriptor.MethodType.BIDI_STREAMING;
    }

    private static List<HttpExchangeNameValue> copyMetadata(final Metadata source) {
        final List<HttpExchangeNameValue> result = new ArrayList<>();
        for (String key : source.keys()) {
            if (key == null) {
                continue;
            }
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                copyBinaryMetadataValues(source, key, result);
            } else {
                copyAsciiMetadataValues(source, key, result);
            }
        }
        return List.copyOf(result);
    }

    private static void copyAsciiMetadataValues(
                                                final Metadata source,
                                                final String key,
                                                final List<HttpExchangeNameValue> destination) {
        try {
            final Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            final Iterable<String> values = source.getAll(metadataKey);
            if (values != null) {
                values.forEach(value -> destination.add(new HttpExchangeNameValue(key, value)));
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to capture ASCII gRPC metadata entry {}", key, throwable);
        }
    }

    private static void copyBinaryMetadataValues(
                                                 final Metadata source,
                                                 final String key,
                                                 final List<HttpExchangeNameValue> destination) {
        try {
            final Metadata.Key<byte[]> metadataKey = Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER);
            final Iterable<byte[]> values = source.getAll(metadataKey);
            if (values != null) {
                values.forEach(
                        value -> destination.add(
                                new HttpExchangeNameValue(key, Base64.getEncoder().encodeToString(value))
                        )
                );
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to capture binary gRPC metadata entry {}", key, throwable);
        }
    }

    private static boolean containsName(final List<HttpExchangeNameValue> values, final String name) {
        return values.stream().anyMatch(value -> name.equalsIgnoreCase(value.name()));
    }

    private static List<HttpExchangeNameValue> withoutName(
                                                           final List<HttpExchangeNameValue> metadata,
                                                           final String name) {
        return metadata.stream()
                .filter(value -> !name.equalsIgnoreCase(value.name()))
                .toList();
    }

    private static List<HttpExchangeCookie> extractRequestCookies(final List<HttpExchangeNameValue> metadata) {
        final List<HttpExchangeCookie> result = new ArrayList<>();
        for (HttpExchangeNameValue header : metadata) {
            if (!COOKIE_HEADER.equalsIgnoreCase(header.name())) {
                continue;
            }
            for (String value : header.value().split(COOKIE_SEPARATOR, -1)) {
                parseCookiePair(value)
                        .map(AllureGrpc::toCookie)
                        .ifPresent(result::add);
            }
        }
        return List.copyOf(result);
    }

    private static List<HttpExchangeCookie> extractResponseCookies(final List<HttpExchangeNameValue> metadata) {
        final List<HttpExchangeCookie> result = new ArrayList<>();
        for (HttpExchangeNameValue header : metadata) {
            if (SET_COOKIE_HEADER.equalsIgnoreCase(header.name())) {
                parseResponseCookie(header.value()).ifPresent(result::add);
            }
        }
        return List.copyOf(result);
    }

    private static Optional<HttpExchangeNameValue> parseCookiePair(final String value) {
        final int separator = value.indexOf('=');
        if (separator <= 0) {
            return Optional.empty();
        }
        final String name = value.substring(0, separator).trim();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new HttpExchangeNameValue(name, value.substring(separator + 1).trim()));
    }

    private static HttpExchangeCookie toCookie(final HttpExchangeNameValue value) {
        return new HttpExchangeCookie(value.name(), value.value());
    }

    private static Optional<HttpExchangeCookie> parseResponseCookie(final String value) {
        final String[] parts = value.split(COOKIE_SEPARATOR, -1);
        final Optional<HttpExchangeNameValue> cookie = parseCookiePair(parts[0]);
        if (cookie.isEmpty()) {
            return Optional.empty();
        }

        String path = null;
        String domain = null;
        String expires = null;
        Boolean httpOnly = null;
        Boolean secure = null;
        String sameSite = null;
        for (int index = 1; index < parts.length; index++) {
            final String attribute = parts[index].trim();
            final int separator = attribute.indexOf('=');
            final String name = (separator < 0 ? attribute : attribute.substring(0, separator))
                    .trim()
                    .toLowerCase(Locale.ROOT);
            final String attributeValue = separator < 0 ? null : attribute.substring(separator + 1).trim();
            switch (name) {
                case "path" -> path = attributeValue;
                case "domain" -> domain = attributeValue;
                case "expires" -> expires = attributeValue;
                case "httponly" -> httpOnly = true;
                case "secure" -> secure = true;
                case "samesite" -> sameSite = attributeValue;
                default -> {
                    // The HTTP exchange cookie schema does not model this Set-Cookie attribute.
                }
            }
        }

        return Optional.of(
                new HttpExchangeCookie(
                        cookie.get().name(),
                        cookie.get().value(),
                        path,
                        domain,
                        expires,
                        httpOnly,
                        secure,
                        sameSite
                )
        );
    }

    /**
     * Builder for {@link AllureGrpc} capture configuration.
     */
    public static final class Builder {
        private AllureLifecycle lifecycle = Allure.getLifecycle();
        private boolean markStepFailedOnNonZeroCode = true;
        private boolean captureRequestMetadata = true;
        private boolean captureResponseMetadata = true;
        private Consumer<HttpExchange.Builder> exchangeCustomizer = exchange -> {
        };

        private Builder() {
        }

        /**
         * Sets the Allure lifecycle used to report calls.
         *
         * @param lifecycle the lifecycle to use
         * @return this builder
         */
        public Builder lifecycle(final AllureLifecycle lifecycle) {
            this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
            return this;
        }

        /**
         * Configures whether a non-zero gRPC status fails the reported step.
         *
         * @param enabled whether a non-zero status fails the step
         * @return this builder
         */
        public Builder markStepFailedOnNonZeroCode(final boolean enabled) {
            this.markStepFailedOnNonZeroCode = enabled;
            return this;
        }

        /**
         * Configures request metadata capture.
         *
         * @param enabled whether to capture request metadata
         * @return this builder
         */
        public Builder captureRequestMetadata(final boolean enabled) {
            this.captureRequestMetadata = enabled;
            return this;
        }

        /**
         * Configures response header and trailer capture.
         *
         * @param enabled whether to capture response metadata
         * @return this builder
         */
        public Builder captureResponseMetadata(final boolean enabled) {
            this.captureResponseMetadata = enabled;
            return this;
        }

        /**
         * Adds a case-insensitive metadata name to the redaction policy.
         *
         * @param name the metadata name to redact
         * @return this builder
         */
        public Builder redactHeader(final String name) {
            return configureExchange(exchange -> exchange.redactHeader(name));
        }

        /**
         * Adds a case-insensitive cookie name to the redaction policy.
         *
         * @param name the cookie name to redact
         * @return this builder
         */
        public Builder redactCookie(final String name) {
            return configureExchange(exchange -> exchange.redactCookie(name));
        }

        /**
         * Adds an HTTP exchange capture customizer.
         *
         * @param customizer the customizer to apply when an exchange is built
         * @return this builder
         */
        public Builder configureExchange(final Consumer<HttpExchange.Builder> customizer) {
            exchangeCustomizer = exchangeCustomizer.andThen(
                    Objects.requireNonNull(customizer, "customizer must not be null")
            );
            return this;
        }

        /**
         * Builds the configured interceptor.
         *
         * @return the configured interceptor
         */
        public AllureGrpc build() {
            return new AllureGrpc(this);
        }
    }

    /**
     * Per-call mutable state of a reported gRPC call: the Allure step identity, the call metadata, and the
     * client/server messages accumulated while the call is in flight.
     */
    private static final class StepContext<T, R> {
        private final AllureExternalKey stepKey;
        private final MethodDescriptor<T, R> methodDescriptor;
        private final AllureLifecycle lifecycle;
        private final List<String> clientMessages;
        private final List<String> serverMessages;
        private final AtomicReference<List<HttpExchangeNameValue>> requestHeaders = new AtomicReference<>(List.of());
        private final List<HttpExchangeNameValue> initialHeaders;
        private final List<HttpExchangeNameValue> trailers;
        private final String authority;
        private final long start;

        StepContext(
                    final AllureExternalKey stepKey,
                    final MethodDescriptor<T, R> methodDescriptor,
                    final AllureLifecycle lifecycle,
                    final List<String> clientMessages,
                    final List<String> serverMessages,
                    final List<HttpExchangeNameValue> initialHeaders,
                    final List<HttpExchangeNameValue> trailers,
                    final String authority,
                    final long start) {
            this.stepKey = stepKey;
            this.methodDescriptor = methodDescriptor;
            this.lifecycle = lifecycle;
            this.clientMessages = clientMessages;
            this.serverMessages = serverMessages;
            this.initialHeaders = initialHeaders;
            this.trailers = trailers;
            this.authority = authority;
            this.start = start;
        }

        AllureExternalKey getStepKey() {
            return stepKey;
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

        List<HttpExchangeNameValue> getRequestHeaders() {
            return requestHeaders.get();
        }

        void setRequestHeaders(final List<HttpExchangeNameValue> requestHeaders) {
            this.requestHeaders.set(requestHeaders);
        }

        List<HttpExchangeNameValue> getInitialHeaders() {
            return initialHeaders;
        }

        List<HttpExchangeNameValue> getTrailers() {
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
