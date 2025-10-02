/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentRenderer;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("all")
public class AllureGrpc implements ClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureGrpc.class);
    private static final String UNKNOWN = "unknown";
    private static final JsonFormat.Printer GRPC_TO_JSON_PRINTER = JsonFormat.printer();
    private final AllureLifecycle lifecycle;
    private final boolean markStepFailedOnNonZeroCode;
    private final boolean interceptResponseMetadata;
    private final String requestTemplatePath;
    private final String responseTemplatePath;

    public AllureGrpc() {
        this(Allure.getLifecycle(), true, false,
            "grpc-request.ftl", "grpc-response.ftl");
    }

    public AllureGrpc(AllureLifecycle lifecycle,
        boolean markStepFailedOnNonZeroCode,
        boolean interceptResponseMetadata,
        String requestTemplatePath,
        String responseTemplatePath) {
        this.lifecycle = lifecycle;
        this.markStepFailedOnNonZeroCode = markStepFailedOnNonZeroCode;
        this.interceptResponseMetadata = interceptResponseMetadata;
        this.requestTemplatePath = requestTemplatePath;
        this.responseTemplatePath = responseTemplatePath;
    }

    @Override
    public <T, R> ClientCall<T, R> interceptCall(
        MethodDescriptor<T, R> methodDescriptor,
        CallOptions callOptions,
        Channel nextChannel
    ) {
        final AllureLifecycle current = lifecycle;
        final String parent = current.getCurrentTestCaseOrStep().orElse(null);
        final String stepUuid = UUID.randomUUID().toString();
        final List<String> clientMessages = new ArrayList<>();
        final List<String> serverMessages = new ArrayList<>();
        final Map<String, String> initialHeaders = new LinkedHashMap<>();
        final Map<String, String> trailers = new LinkedHashMap<>();

        final String stepName = buildStepName(nextChannel, methodDescriptor);
        if (parent != null) current.startStep(parent, stepUuid, new StepResult().setName(stepName));
        else current.startStep(stepUuid, new StepResult().setName(stepName));

        final StepContext<T, R> stepContext = new StepContext<>(
            stepUuid, methodDescriptor, current, clientMessages, serverMessages, initialHeaders, trailers
        );

        return new ForwardingClientCall.SimpleForwardingClientCall<T, R>(
            nextChannel.newCall(methodDescriptor, callOptions)
        ) {
            @Override
            public void start(final Listener<R> responseListener, final Metadata requestHeaders) {
                final Listener<R> forwardingListener = new ForwardingClientCallListener<R>() {
                    @Override protected Listener<R> delegate() { return responseListener; }
                    @Override public void onHeaders(final Metadata headers) {
                        handleHeaders(headers, stepContext.initialHeaders);
                        super.onHeaders(headers);
                    }
                    @Override public void onMessage(final R message) {
                        handleServerMessage(message, stepContext.serverMessages);
                        super.onMessage(message);
                    }
                    @Override public void onClose(final io.grpc.Status status, final Metadata responseTrailers) {
                        handleClose(status, responseTrailers, stepContext);
                        super.onClose(status, responseTrailers);
                    }
                };
                super.start(forwardingListener, requestHeaders);
            }
            @Override
            public void sendMessage(final T message) {
                handleClientMessage(message, stepContext.clientMessages);
                super.sendMessage(message);
            }
        };
    }

    private static final class StepContext<T, R> {
        final String stepUuid;
        final MethodDescriptor<T, R> methodDescriptor;
        final AllureLifecycle lifecycle;
        final List<String> clientMessages;
        final List<String> serverMessages;
        final Map<String, String> initialHeaders;
        final Map<String, String> trailers;
        StepContext(String stepUuid,
            MethodDescriptor<T, R> methodDescriptor,
            AllureLifecycle lifecycle,
            List<String> clientMessages,
            List<String> serverMessages,
            Map<String, String> initialHeaders,
            Map<String, String> trailers) {
            this.stepUuid = stepUuid;
            this.methodDescriptor = methodDescriptor;
            this.lifecycle = lifecycle;
            this.clientMessages = clientMessages;
            this.serverMessages = serverMessages;
            this.initialHeaders = initialHeaders;
            this.trailers = trailers;
        }
    }

    private void handleClose(
        final io.grpc.Status status,
        final Metadata responseTrailers,
        final StepContext<?, ?> stepContext
    ) {
        try {
            if (interceptResponseMetadata && responseTrailers != null) {
                copyAsciiResponseMetadata(responseTrailers, stepContext.trailers);
            }
            attachRequestIfPresent(stepContext.stepUuid, stepContext.methodDescriptor,
                stepContext.clientMessages, stepContext.lifecycle);
            attachResponse(stepContext.stepUuid, stepContext.serverMessages, status,
                stepContext.initialHeaders, stepContext.trailers, stepContext.lifecycle);
            stepContext.lifecycle.updateStep(stepContext.stepUuid, step -> step.setStatus(convertStatus(status)));
        } catch (Throwable throwable) {
            LOGGER.error("Failed to finalize Allure step for gRPC call", throwable);
            stepContext.lifecycle.updateStep(stepContext.stepUuid, step -> step.setStatus(Status.BROKEN));
        } finally {
            stopStepSafely(stepContext.lifecycle, stepContext.stepUuid);
        }
    }

    private void handleHeaders(final Metadata headers, final Map<String, String> destination) {
        try {
            if (interceptResponseMetadata && headers != null)
                copyAsciiResponseMetadata(headers, destination);
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

    private <T, R> void attachRequestIfPresent(
        final String stepUuid,
        final MethodDescriptor<T, R> methodDescriptor,
        final List<String> clientMessages,
        final AllureLifecycle lifecycle
    ) {
        final String body = toJsonBody(clientMessages);
        if (body == null) {
            return;
        }
        final String name = clientMessages.size() > 1
            ? "gRPC request (collection of elements from Client stream)"
            : "gRPC request";
        final GrpcRequestAttachment requestAttachment = GrpcRequestAttachment.Builder
            .create(name, methodDescriptor.getFullMethodName())
            .setBody(body)
            .build();
        addRenderedAttachmentToStep(stepUuid, requestAttachment.getName(), requestAttachment, requestTemplatePath, lifecycle);
    }

    private void attachResponse(
        final String stepUuid,
        final List<String> serverMessages,
        final io.grpc.Status status,
        final Map<String, String> initialHeaders,
        final Map<String, String> trailers,
        final AllureLifecycle lifecycle
    ) {
        final String body = toJsonBody(serverMessages);
        final String name = serverMessages.size() > 1
            ? "gRPC response (collection of elements from Server stream)"
            : "gRPC response";

        final Map<String, String> metadata = new LinkedHashMap<>();
        if (interceptResponseMetadata) {
            metadata.putAll(initialHeaders);
            metadata.putAll(trailers);
        }

        final GrpcResponseAttachment.Builder builder = GrpcResponseAttachment.Builder
            .create(name)
            .setStatus(status.toString());
        if (body != null) {
            builder.setBody(body);
        }
        if (!metadata.isEmpty()) {
            builder.addMetadata(metadata);
        }
        final GrpcResponseAttachment responseAttachment = builder.build();
        addRenderedAttachmentToStep(stepUuid, responseAttachment.getName(),
            responseAttachment, responseTemplatePath, lifecycle);
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
        final MethodDescriptor<?, ?> methodDescriptor
    ) {
        final String authority = channel != null ? channel.authority() : null;
        final String safeAuthority = authority != null ? authority : UNKNOWN;
        final String type = toSnakeCase(methodDescriptor.getType());
        return "Send " + type + " gRPC request to " + safeAuthority + "/" + methodDescriptor.getFullMethodName();
    }

    private static String toSnakeCase(final MethodDescriptor.MethodType methodType) {
        if (methodType == null) {
            return UNKNOWN;
        }
        return methodType.name().toLowerCase(Locale.ROOT);
    }

    private void addRenderedAttachmentToStep(
        final String stepUuid,
        final String attachmentName,
        final AttachmentData data,
        final String templatePath,
        final AllureLifecycle lifecycle
    ) {
        final AttachmentRenderer<AttachmentData> renderer = new FreemarkerAttachmentRenderer(templatePath);
        final io.qameta.allure.attachment.AttachmentContent content;
        try {
            content = renderer.render(data);
        } catch (Throwable throwable) {
            LOGGER.warn("Could not render attachment '{}' using template '{}'", attachmentName, templatePath, throwable);
            return;
        }
        if (content == null || content.getContent() == null) {
            LOGGER.warn("Rendered attachment '{}' is empty; skipping", attachmentName);
            return;
        }
        String fileExtension = content.getFileExtension();
        if (fileExtension == null || fileExtension.isEmpty()) {
            fileExtension = ".html";
        }
        final String source = UUID.randomUUID() + fileExtension;
        lifecycle.updateStep(
            stepUuid,
            step -> step.getAttachments().add(
                new Attachment()
                    .setName(attachmentName)
                    .setSource(source)
                    .setType(content.getContentType() != null ? content.getContentType() : "text/html")
            )
        );
        lifecycle.writeAttachment(source, new ByteArrayInputStream(content.getContent().getBytes(StandardCharsets.UTF_8)));
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

    private static void copyAsciiResponseMetadata(final Metadata source, final Map<String, String> target) {
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
}
