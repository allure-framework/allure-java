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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class AllureGrpc implements ClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureGrpc.class);
    private static final String UNKNOWN = "unknown";
    private static final JsonFormat.Printer GRPC_TO_JSON_PRINTER = JsonFormat.printer();

    private String requestTemplatePath = "grpc-request.ftl";
    private String responseTemplatePath = "grpc-response.ftl";

    private volatile boolean markStepFailedOnNonZeroCode = true;
    private volatile boolean interceptResponseMetadata;

    private AllureLifecycle lifecycle;

    private static final ConcurrentLinkedQueue<CompletableFuture<Void>> PENDING_COMPLETIONS =
        new ConcurrentLinkedQueue<>();

    public AllureGrpc() {
        this(Allure.getLifecycle());
    }

    public AllureGrpc(final AllureLifecycle allureLifecycle) {
        this.lifecycle = allureLifecycle;
    }

    public void setLifecycle(final AllureLifecycle allureLifecycle) {
        this.lifecycle = allureLifecycle;
    }

    public AllureGrpc setRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureGrpc setResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    public AllureGrpc markStepFailedOnNonZeroCode(final boolean value) {
        this.markStepFailedOnNonZeroCode = value;
        return this;
    }

    public AllureGrpc interceptResponseMetadata(final boolean value) {
        this.interceptResponseMetadata = value;
        return this;
    }

    public static void await() {
        CompletableFuture<Void> completion;
        while ((completion = PENDING_COMPLETIONS.poll()) != null) {
            try {
                completion.join();
            } catch (RuntimeException runtimeException) {
                LOGGER.warn("Await interrupted with exception", runtimeException);
            }
        }
    }

    @Override
    public <T, R> ClientCall<T, R> interceptCall(
        final MethodDescriptor<T, R> methodDescriptor,
        final CallOptions callOptions,
        final Channel nextChannel
    ) {
        final AllureLifecycle current = Allure.getLifecycle();
        final String parent = current.getCurrentTestCaseOrStep().orElse(null);
        final String stepUuid = UUID.randomUUID().toString();

        final List<String> clientMessages = Collections.synchronizedList(new ArrayList<>());
        final List<String> serverMessages = Collections.synchronizedList(new ArrayList<>());
        final Map<String, String> initialHeaders = new LinkedHashMap<>();
        final Map<String, String> trailers = new LinkedHashMap<>();
        final CompletableFuture<Void> completion = new CompletableFuture<>();
        PENDING_COMPLETIONS.add(completion);

        final String stepName = buildStepName(nextChannel, methodDescriptor);
        if (parent != null) {
            current.startStep(parent, stepUuid, new StepResult().setName(stepName));
        } else {
            current.startStep(stepUuid, new StepResult().setName(stepName));
        }

        final StepContext<T, R> ctx = new StepContext<>(
            stepUuid, methodDescriptor, current, clientMessages, serverMessages, initialHeaders, trailers, completion
        );

        return new ForwardingClientCall.SimpleForwardingClientCall<T, R>(
            nextChannel.newCall(methodDescriptor, callOptions)
        ) {
            @Override
            public void start(final Listener<R> rl, final Metadata rh) {
                final Listener<R> l = new ForwardingClientCallListener<R>() {
                    @Override protected Listener<R> delegate() { return rl; }
                    @Override public void onHeaders(final Metadata h) {
                        handleHeaders(h, ctx.initialHeaders); super.onHeaders(h);
                    }
                    @Override public void onMessage(final R m) {
                        handleServerMessage(m, ctx.serverMessages); super.onMessage(m);
                    }
                    @Override public void onClose(final io.grpc.Status s, final Metadata t) {
                        handleClose(s, t, ctx); super.onClose(s, t);
                    }
                };
                super.start(l, rh);
            }
            @Override
            public void sendMessage(final T m) {
                handleClientMessage(m, ctx.clientMessages);
                super.sendMessage(m);
            }
        };
    }

    private static final class StepContext<T, R> {
        final String stepUuid;
        final MethodDescriptor<T, R> method;
        final AllureLifecycle lifecycleRef;
        final List<String> clientMessages;
        final List<String> serverMessages;
        final Map<String, String> initialHeaders;
        final Map<String, String> trailers;
        final CompletableFuture<Void> done;

        StepContext(
            final String stepUuid,
            final MethodDescriptor<T, R> method,
            final AllureLifecycle lifecycleRef,
            final List<String> clientMessages,
            final List<String> serverMessages,
            final Map<String, String> initialHeaders,
            final Map<String, String> trailers,
            final CompletableFuture<Void> done
        ) {
            this.stepUuid = stepUuid;
            this.method = method;
            this.lifecycleRef = lifecycleRef;
            this.clientMessages = clientMessages;
            this.serverMessages = serverMessages;
            this.initialHeaders = initialHeaders;
            this.trailers = trailers;
            this.done = done;
        }
    }

    private void handleHeaders(final Metadata headers, final Map<String, String> dst) {
        try {
            if (interceptResponseMetadata && headers != null) {
                copyAsciiResponseMetadata(headers, dst);
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to capture initial response headers", t);
        }
    }

    private <T> void handleClientMessage(final T message, final List<String> dst) {
        try {
            dst.add(GRPC_TO_JSON_PRINTER.print((MessageOrBuilder) message));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Could not serialize gRPC request message to JSON", e);
        } catch (Throwable t) {
            LOGGER.error("Unexpected error while serializing gRPC request message", t);
        }
    }

    private <R> void handleServerMessage(final R message, final List<String> dst) {
        try {
            dst.add(GRPC_TO_JSON_PRINTER.print((MessageOrBuilder) message));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Could not serialize gRPC response message to JSON", e);
        } catch (Throwable t) {
            LOGGER.error("Unexpected error while serializing gRPC response message", t);
        }
    }

    private void handleClose(
        final io.grpc.Status status,
        final Metadata trailers,
        final StepContext<?, ?> ctx
    ) {
        try {
            if (interceptResponseMetadata && trailers != null) {
                copyAsciiResponseMetadata(trailers, ctx.trailers);
            }
            attachRequestIfPresent(ctx.stepUuid, ctx.method, ctx.clientMessages, ctx.lifecycleRef);
            attachResponse(ctx.stepUuid, ctx.serverMessages, status, ctx.initialHeaders, ctx.trailers, ctx.lifecycleRef);
            ctx.lifecycleRef.updateStep(ctx.stepUuid, s -> s.setStatus(convertStatus(status)));
        } catch (Throwable t) {
            LOGGER.error("Failed to finalize Allure step for gRPC call", t);
            ctx.lifecycleRef.updateStep(ctx.stepUuid, s -> s.setStatus(Status.BROKEN));
        } finally {
            stopStepSafely(ctx.lifecycleRef, ctx.stepUuid);
            ctx.done.complete(null);
            PENDING_COMPLETIONS.remove(ctx.done);
        }
    }

    private <T, R> void attachRequestIfPresent(
        final String stepUuid,
        final MethodDescriptor<T, R> method,
        final List<String> clientMessages,
        final AllureLifecycle lifecycleRef
    ) {
        final String body = toJsonBody(clientMessages);
        if (body == null) {
            return;
        }
        final String name = clientMessages.size() > 1
            ? "gRPC request (collection of elements from Client stream)"
            : "gRPC request";
        final GrpcRequestAttachment req = GrpcRequestAttachment.Builder
            .create(name, method.getFullMethodName())
            .setBody(body)
            .build();
        addRenderedAttachmentToStep(stepUuid, req.getName(), req, requestTemplatePath, lifecycleRef);
    }

    private void attachResponse(
        final String stepUuid,
        final List<String> serverMessages,
        final io.grpc.Status status,
        final Map<String, String> initialHeaders,
        final Map<String, String> trailers,
        final AllureLifecycle lifecycleRef
    ) {
        final String body = toJsonBody(serverMessages);
        final String name = serverMessages.size() > 1
            ? "gRPC response (collection of elements from Server stream)"
            : "gRPC response";

        final Map<String, String> meta = new LinkedHashMap<>();
        if (interceptResponseMetadata) {
            meta.putAll(initialHeaders);
            meta.putAll(trailers);
        }

        final GrpcResponseAttachment.Builder b = GrpcResponseAttachment.Builder
            .create(name)
            .setStatus(status.toString());
        if (body != null) {
            b.setBody(body);
        }
        if (!meta.isEmpty()) {
            b.addMetadata(meta);
        }
        final GrpcResponseAttachment res = b.build();
        addRenderedAttachmentToStep(stepUuid, res.getName(), res, responseTemplatePath, lifecycleRef);
    }

    private void stopStepSafely(final AllureLifecycle lc, final String stepUuid) {
        try {
            lc.stopStep(stepUuid);
        } catch (Throwable t) {
            LOGGER.warn("Failed to stop Allure step {}", stepUuid, t);
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
        final AllureLifecycle lifecycleRef
    ) {
        final AttachmentRenderer<AttachmentData> renderer = new FreemarkerAttachmentRenderer(templatePath);
        final io.qameta.allure.attachment.AttachmentContent content;
        try {
            content = renderer.render(data);
        } catch (Throwable t) {
            LOGGER.warn("Could not render attachment '{}' using template '{}'", attachmentName, templatePath, t);
            return;
        }
        if (content == null || content.getContent() == null) {
            LOGGER.warn("Rendered attachment '{}' is empty; skipping", attachmentName);
            return;
        }
        String ext = content.getFileExtension();
        if (ext == null || ext.isEmpty()) {
            ext = ".html";
        }
        final String source = UUID.randomUUID() + ext;
        lifecycleRef.updateStep(
            stepUuid,
            s -> s.getAttachments().add(
                new Attachment()
                    .setName(attachmentName)
                    .setSource(source)
                    .setType(content.getContentType() != null ? content.getContentType() : "text/html")
            )
        );
        lifecycleRef.writeAttachment(source, new ByteArrayInputStream(content.getContent().getBytes(StandardCharsets.UTF_8)));
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
            final Metadata.Key<String> k = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            final String v = source.get(k);
            if (v != null) {
                target.put(key, v);
            }
        }
    }
}
