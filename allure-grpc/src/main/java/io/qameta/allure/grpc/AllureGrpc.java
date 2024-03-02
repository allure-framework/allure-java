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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allure interceptor logger for gRPC that is used to create allure steps and attach gRPC messages
 * for the gRPC Channel, blocking stubs, or non-blocking stubs with after-each hook (see examples).
 * Channels, stubs may be either static or non-static.
 *
 * <h2>Usage with Channel</h2>
 * <pre>
 *   static Channel channel = ManagedChannelBuilder.forAddress("localhost", 8092)
 *       .intercept(new AllureGrpc())
 *       .build();
 * </pre>
 * When {@code AllureGrpc} object is present in a Channel,
 * all stubs (both blocking and non-blocking) that using this Channel will intercept messages
 * to Allure-report.
 * <h3>NOTE!
 * With non-blocking Stubs you have to use after-each hook for tests because non-blocking stubs
 * are async, and allure-report might not be created after non-blocking Stubs call. See examples below
 * </h3>
 *
 * <h2>Usage with blocking Stubs</h2>
 * <pre>
 *   ServiceGrpc.ServiceBlockingStub blockingStub = ServiceGrpc
 *      .newBlockingStub(channel)
 *      .withInterceptors(new AllureGrpc());
 * </pre>
 * You can just add {@code AllureGrpc} object to blocking Stub object, all unary calls from
 * this stub will be logged to Allure-report.
 *
 * <h2>Usage with non-blocking Stubs (example for JUnit 5)</h2>
 * <pre>
 *   ServiceGrpc.ServiceStub stub = ServiceGrpc
 *      .newStub(channel)
 *      .withInterceptors(new AllureGrpc());
 *
 *   {@code @Test}
 *   void test() {
 *       stub.streamingCall(...);
 *   }
 *
 *   {@code @AfterEach}
 *   void awaitAllureForNonBlockingCalls() {
 *     AllureGrpc.await();
 *   }
 * </pre>
 * You can use {@code @AfterTest} annotation for TestNG and {@code @After} for JUnit 4.
 * Of course, you can implement Extension or Rule to achieve that.
 *
 * @author dtuchs (Dmitrii Tuchs).
 */
@SuppressWarnings({
        "PMD.AvoidFieldNameMatchingMethodName",
        "checkstyle:ClassFanOutComplexity",
        "checkstyle:AnonInnerLength",
        "checkstyle:JavaNCSS"
})
public class AllureGrpc implements ClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureGrpc.class);
    private static final JsonFormat.Printer GRPC_TO_JSON_PRINTER = JsonFormat.printer();
    private static final Map<String, CountDownLatch> TEST_CASE_HOLDER = new ConcurrentHashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DefaultPrettyPrinter JSON_PRETTY_PRINTER = new DefaultPrettyPrinter();
    private static final String STREAMING_MESSAGE
            = "gRPC messages (collection of elements from %s stream)";

    private String requestTemplatePath = "grpc-request.ftl";
    private String responseTemplatePath = "grpc-response.ftl";

    private boolean markStepFailedOnNonZeroCode = true;
    private boolean interceptResponseMetadata;

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

    /**
     *  <h3>Should be called only after tests that use non-blocking Stubs.</h3>
     * Generally, you can do it in the {@code @AfterEach} method in JUnit 5,
     * {@code @AfterTest} in TestNG, or {@code @After} in JUnit 4 tests
     */
    public static void await() {
        Allure.getLifecycle().getCurrentTestCase().ifPresent(caseId -> {
            try {
                final CountDownLatch latch = TEST_CASE_HOLDER.get(caseId);
                if (latch != null) {
                    latch.await();
                    TEST_CASE_HOLDER.remove(caseId);
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("Thread interrupted", e);
            }
        });
    }

    @SuppressWarnings({"checkstyle:Methodlength", "PMD.MethodArgumentCouldBeFinal", "PMD.NPathComplexity"})
    @Override
    public <T, A> ClientCall<T, A> interceptCall(MethodDescriptor<T, A> method,
                                                 CallOptions callOptions,
                                                 Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<T, A>(
                next.newCall(method, callOptions.withoutWaitForReady())) {

            private final Queue<String> parsedResponses = new ConcurrentLinkedQueue<>();
            private final Queue<String> parsedRequests = new ConcurrentLinkedQueue<>();

            private final AtomicBoolean allureThreadStarted = new AtomicBoolean(false);
            private final CountDownLatch interceptorLatch = new CountDownLatch(1);

            private final AtomicReference<Throwable> exceptionHolder = new AtomicReference<>();
            private final AtomicReference<io.grpc.Status> statusHolder = new AtomicReference<>();
            private final AtomicReference<Metadata> metadataHolder = new AtomicReference<>();

            private final ExecutorService executor = Executors.newSingleThreadExecutor();
            private String caseUuid;

            @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
            @Override
            public void sendMessage(T message) {
                final AllureLifecycle lifecycle = Allure.getLifecycle();
                caseUuid = lifecycle.getCurrentTestCase().orElseThrow(
                        () -> new IllegalStateException("No test case started")
                );
                TEST_CASE_HOLDER.put(caseUuid, new CountDownLatch(1));

                if (!allureThreadStarted.get()) {
                    executor.submit(new Runnable() {
                        private final AtomicBoolean stepCreated = new AtomicBoolean(false);
                        private final AttachmentProcessor<AttachmentData> processor
                                = new DefaultAttachmentProcessor(lifecycle);
                        private String stepUuid;

                        /**
                         * Daemon thread that will be used to create steps and attachments.
                         * This approach should be used because non-blocking stubs use several threads
                         * to send and receive messages,
                         * while {@code AllureThreadContext} uses {@code ThreadLocal} internally.
                         */
                        @Override
                        public void run() {
                            if (!stepCreated.get()) {
                                stepUuid = startStep(lifecycle);
                            }

                            try {
                                interceptorLatch.await();
                                if (exceptionHolder.get() != null) {
                                    markStepBrokenByException(lifecycle);
                                } else {
                                    final io.grpc.Status status = statusHolder.get();
                                    final Metadata metadata = metadataHolder.get();
                                    final GrpcRequestAttachment.Builder requestAttachment
                                            = requestAttachment(method);
                                    final GrpcResponseAttachment.Builder responseAttachment
                                            = responseAttachment(method, status, metadata);

                                    processor.addAttachment(
                                            requestAttachment.build(),
                                            new FreemarkerAttachmentRenderer(requestTemplatePath)
                                    );
                                    processor.addAttachment(
                                            responseAttachment.build(),
                                            new FreemarkerAttachmentRenderer(responseTemplatePath)
                                    );

                                    if (status.isOk() || !markStepFailedOnNonZeroCode) {
                                        lifecycle.updateStep(stepUuid, step -> step.setStatus(Status.PASSED));
                                    } else {
                                        lifecycle.updateStep(stepUuid, step -> step.setStatus(Status.FAILED));
                                    }
                                }
                                lifecycle.stopStep(stepUuid);
                                TEST_CASE_HOLDER.get(caseUuid).countDown();
                            } catch (InterruptedException e) {
                                throw new IllegalStateException("Thread interrupted ", e);
                            } finally {
                                stepUuid = null;
                                stepCreated.set(false);
                            }
                        }
                    });
                    allureThreadStarted.set(true);
                }

                try {
                    parsedRequests.add(GRPC_TO_JSON_PRINTER.print((MessageOrBuilder) message));
                    super.sendMessage(message);
                } catch (InvalidProtocolBufferException e) {
                    LOGGER.warn("Can`t parse gRPC request", e);
                } catch (Exception e) {
                    exceptionHolder.set(e);
                    shutdownAllureThread();
                }
            }

            @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
            @Override
            public void start(Listener<A> responseListener, Metadata headers) {
                final ClientCall.Listener<A> listener = new ForwardingClientCallListener<A>() {
                    @Override
                    protected Listener<A> delegate() {
                        return responseListener;
                    }

                    @SuppressWarnings({"PMD.MethodArgumentCouldBeFinal", "PMD.AvoidLiteralsInIfCondition"})
                    @Override
                    public void onClose(io.grpc.Status status, Metadata trailers) {
                        statusHolder.set(status);
                        metadataHolder.set(trailers);
                        shutdownAllureThread();
                        super.onClose(status, trailers);
                    }

                    @SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
                    @Override
                    public void onMessage(A message) {
                        try {
                            parsedResponses.add(GRPC_TO_JSON_PRINTER.print((MessageOrBuilder) message));
                            super.onMessage(message);
                        } catch (InvalidProtocolBufferException e) {
                            LOGGER.warn("Can`t parse gRPC response", e);
                        } catch (Exception e) {
                            exceptionHolder.set(e);
                            shutdownAllureThread();
                        }
                    }
                };
                super.start(listener, headers);
            }

            private String stepBody(final Queue<String> messages) {
                JSON_PRETTY_PRINTER.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
                try {
                    return OBJECT_MAPPER.writer(JSON_PRETTY_PRINTER)
                            .writeValueAsString(
                                    OBJECT_MAPPER.readValue(
                                            "[" + String.join(",", messages) + "]",
                                            ArrayNode.class
                                    )
                            );
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Can`t parse collected gRPC messages");
                    return "";
                }
            }

            private String grpcMethodName(final String source) {
                return source.substring(source.lastIndexOf('/'));
            }

            private void shutdownAllureThread() {
                interceptorLatch.countDown();
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            private String startStep(final AllureLifecycle lifecycle) {
                final String stepUuid = UUID.randomUUID().toString();
                lifecycle.startStep(stepUuid, (new StepResult()).setName(
                        "Send " + method.getType().toString().toLowerCase() + " gRPC request to "
                                + next.authority()
                                + grpcMethodName(method.getFullMethodName())
                ));
                return stepUuid;
            }

            private void markStepBrokenByException(final AllureLifecycle lifecycle) {
                final Throwable e = exceptionHolder.get();
                lifecycle.updateStep(stepResult ->
                        stepResult.setStatus(ResultsUtils.getStatus(e).orElse(Status.BROKEN))
                                .setStatusDetails(
                                        ResultsUtils.getStatusDetails(e).orElse(null)
                                )
                );
            }

            private <T, A> GrpcRequestAttachment.Builder requestAttachment(final MethodDescriptor<T, A> method) {
                if (method.getType().clientSendsOneMessage()) {
                    return GrpcRequestAttachment.Builder
                            .create("gRPC request", method.getFullMethodName())
                            .setBody(parsedRequests.element());
                } else {
                    return GrpcRequestAttachment.Builder
                            .create(String.format(STREAMING_MESSAGE, "Client"), method.getFullMethodName())
                            .setBody(stepBody(parsedRequests));
                }
            }

            private <T, A> GrpcResponseAttachment.Builder responseAttachment(final MethodDescriptor<T, A> method,
                                                                             final io.grpc.Status status,
                                                                             final Metadata metadata) {
                final GrpcResponseAttachment.Builder result;
                if (!status.isOk()) {
                    final String description = status.getDescription() == null
                            ? "No description provided"
                            : status.getDescription();
                    result = GrpcResponseAttachment.Builder.create(status.getCode().name())
                            .setStatus(status + " " + description);
                } else {
                    if (method.getType().serverSendsOneMessage()) {
                        result = GrpcResponseAttachment.Builder.create("gRPC response")
                                .setBody(parsedResponses.element())
                                .setStatus(status.toString());
                    } else {
                        result = GrpcResponseAttachment.Builder.create(String.format(STREAMING_MESSAGE, "Server"))
                                .setBody(stepBody(parsedResponses))
                                .setStatus(status.toString());
                    }
                }
                if (interceptResponseMetadata) {
                    for (String key : metadata.keys()) {
                        result.setMetadata(key, metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));
                    }
                }
                return result;
            }
        };
    }
}
