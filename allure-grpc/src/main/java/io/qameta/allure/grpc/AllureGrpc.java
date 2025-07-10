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
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Allure interceptor logger for gRPC.
 *
 * @author dtuchs (Dmitry Tuchs).
 */
@SuppressWarnings({
        "checkstyle:ClassFanOutComplexity",
        "checkstyle:AnonInnerLength",
        "checkstyle:JavaNCSS"
})
public class AllureGrpc implements ClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureGrpc.class);
    private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();

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

    @Override
    public <T, A> ClientCall<T, A> interceptCall(final MethodDescriptor<T, A> method,
                                                 final CallOptions callOptions,
                                                 final Channel next) {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        return new ForwardingClientCall.SimpleForwardingClientCall<T, A>(
                next.newCall(method, callOptions.withoutWaitForReady())) {

            private String stepUuid;
            private final List<String> parsedResponses = new ArrayList<>();

            @Override
            public void sendMessage(final T message) {
                stepUuid = UUID.randomUUID().toString();
                Allure.getLifecycle().startStep(stepUuid, (new StepResult()).setName(
                        "Send gRPC request to "
                                + next.authority()
                                + trimGrpcMethodName(method.getFullMethodName())
                ));
                try {
                    final GrpcRequestAttachment rpcRequestAttach = GrpcRequestAttachment.Builder
                            .create("gRPC request", method.getFullMethodName())
                            .setBody(JSON_PRINTER.print((MessageOrBuilder) message))
                            .build();
                    processor.addAttachment(rpcRequestAttach, new FreemarkerAttachmentRenderer(requestTemplatePath));
                    super.sendMessage(message);
                } catch (InvalidProtocolBufferException e) {
                    LOGGER.warn("Can`t parse gRPC request", e);
                } catch (Throwable e) {
                    Allure.getLifecycle().updateStep(stepResult ->
                            stepResult.setStatus(ResultsUtils.getStatus(e).orElse(Status.BROKEN))
                                    .setStatusDetails(ResultsUtils.getStatusDetails(e).orElse(null))
                    );
                    Allure.getLifecycle().stopStep(stepUuid);
                    stepUuid = null;
                }
            }

            @Override
            public void start(final Listener<A> responseListener, final Metadata headers) {
                final ClientCall.Listener<A> listener = new ForwardingClientCallListener<A>() {
                    @Override
                    protected Listener<A> delegate() {
                        return responseListener;
                    }

                    @Override
                    public void onClose(final io.grpc.Status status, final Metadata trailers) {
                        GrpcResponseAttachment.Builder responseAttachmentBuilder = null;

                        if (parsedResponses.size() == 1) {
                            responseAttachmentBuilder = GrpcResponseAttachment.Builder
                                    .create("gRPC response")
                                    .setBody(parsedResponses.iterator().next());
                        } else if (parsedResponses.size() > 1) {
                            responseAttachmentBuilder = GrpcResponseAttachment.Builder
                                    .create("gRPC response (collection of elements from Server stream)")
                                    .setBody("[" + String.join(",\n", parsedResponses) + "]");
                        }
                        if (!status.isOk()) {
                            String description = status.getDescription();
                            if (description == null) {
                                description = "No description provided";
                            }
                            responseAttachmentBuilder = GrpcResponseAttachment.Builder
                                    .create(status.getCode().name())
                                    .setStatus(description);
                        }

                        requireNonNull(responseAttachmentBuilder).setStatus(status.toString());
                        if (interceptResponseMetadata) {
                            for (String key : headers.keys()) {
                                requireNonNull(responseAttachmentBuilder).setMetadata(
                                        key,
                                        headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER))
                                );
                            }
                        }
                        processor.addAttachment(
                                requireNonNull(responseAttachmentBuilder).build(),
                                new FreemarkerAttachmentRenderer(responseTemplatePath)
                        );

                        if (status.isOk() || !markStepFailedOnNonZeroCode) {
                            Allure.getLifecycle().updateStep(stepUuid, step -> step.setStatus(Status.PASSED));
                        } else {
                            Allure.getLifecycle().updateStep(stepUuid, step -> step.setStatus(Status.FAILED));
                        }
                        Allure.getLifecycle().stopStep(stepUuid);
                        stepUuid = null;
                        super.onClose(status, trailers);
                    }

                    @Override
                    public void onMessage(final A message) {
                        try {
                            parsedResponses.add(JSON_PRINTER.print((MessageOrBuilder) message));
                            super.onMessage(message);
                        } catch (InvalidProtocolBufferException e) {
                            LOGGER.warn("Can`t parse gRPC response", e);
                        } catch (Throwable e) {
                            Allure.getLifecycle().updateStep(step ->
                                    step.setStatus(ResultsUtils.getStatus(e).orElse(Status.BROKEN))
                                            .setStatusDetails(ResultsUtils.getStatusDetails(e).orElse(null))
                            );
                            Allure.getLifecycle().stopStep(stepUuid);
                            stepUuid = null;
                        }
                    }
                };
                super.start(listener, headers);
            }

            private String trimGrpcMethodName(final String source) {
                return source.substring(source.lastIndexOf('/'));
            }
        };
    }
}
