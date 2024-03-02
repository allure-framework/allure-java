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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.test.AllureResults;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.grpcmock.GrpcMock.*;

/**
 * @author dtuchs (Dmitrii Tuchs).
 */
@ExtendWith(GrpcMockExtension.class)
class AllureGrpcTest {

    private static final Response RESPONSE_MESSAGE = Response.newBuilder().setMessage("Hello world!").build();
    private static final ManagedChannel CHANNEL = ManagedChannelBuilder
            .forAddress("localhost", GrpcMock.getGlobalPort())
            .intercept(new AllureGrpc())
            .usePlaintext()
            .build();

    private TestServiceGrpc.TestServiceBlockingStub blockingStub;
    private TestServiceGrpc.TestServiceStub nonBlockingStub;

    @BeforeEach
    void configureMock() {
        blockingStub = TestServiceGrpc.newBlockingStub(CHANNEL);
        nonBlockingStub = TestServiceGrpc.newStub(CHANNEL);

        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod())
                .willReturn(RESPONSE_MESSAGE));

        GrpcMock.stubFor(serverStreamingMethod(TestServiceGrpc.getCalculateServerStreamMethod())
                .willReturn(asList(
                        RESPONSE_MESSAGE,
                        RESPONSE_MESSAGE
                )));

        GrpcMock.stubFor(clientStreamingMethod(TestServiceGrpc.getCalculateClientStreamMethod())
                .withFirstRequest(request -> "client".equals(request.getTopic()))
                .willReturn(RESPONSE_MESSAGE));

        GrpcMock.stubFor(bidiStreamingMethod(TestServiceGrpc.getCalculateBidirectionalStreamMethod())
                .withFirstRequest(request -> "bidirectional".equals(request.getTopic()))
                .willProxyTo(responseObserver -> new StreamObserver<Request>() {
                    @Override
                    public void onNext(Request request) {
                        responseObserver.onNext(RESPONSE_MESSAGE);
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onCompleted() {
                        responseObserver.onCompleted();
                    }
                }));
    }

    @AfterAll
    static void shutdownChannel() {
        Optional.ofNullable(CHANNEL).ifPresent(ManagedChannel::shutdownNow);
    }

    @Test
    void shouldCreateRequestAttachment() {
        final Request request = Request.newBuilder()
                .setTopic("1")
                .build();

        final AllureResults results = execute(request);

        assertThat(results.getTestResults()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains("gRPC request");
    }

    @Test
    void shouldCreateResponseAttachment() {
        final Request request = Request.newBuilder()
                .setTopic("1")
                .build();

        final AllureResults results = execute(request);

        assertThat(results.getTestResults()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains("gRPC response");
    }

    @Test
    void shouldCreateResponseAttachmentForServerStreamingResponse() {
        final Request request = Request.newBuilder()
                .setTopic("1")
                .build();

        final AllureResults results = executeServerStreaming(request);

        assertThat(results.getTestResults()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains(
                        "gRPC request",
                        "gRPC messages (collection of elements from Server stream)"
                );
    }

    @Test
    void shouldCreateResponseAttachmentForClientStreamingResponse() {
        final Request request = Request.newBuilder()
                .setTopic("client")
                .build();
        final Queue<Request> requests = new LinkedList<>();
        requests.add(request);

        final AllureResults results = executeClientStreaming(requests);

        assertThat(results.getTestResults()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains(
                        "gRPC messages (collection of elements from Client stream)",
                        "gRPC response"
                );
    }

    @Test
    void shouldCreateResponseAttachmentForBidirectionalStreaming() {
        final Request request = Request.newBuilder()
                .setTopic("bidirectional")
                .build();
        final Queue<Request> requests = new LinkedList<>();
        requests.add(request);

        final AllureResults results = executeBidirectionalStreaming(requests);

        assertThat(results.getTestResults()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains(
                        "gRPC messages (collection of elements from Client stream)",
                        "gRPC messages (collection of elements from Server stream)"
                );
    }

    @Test
    void shouldCreateResponseAttachmentOnStatusException() {
        final Status status = Status.NOT_FOUND;
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod())
                .willReturn(status));

        final Request request = Request.newBuilder()
                .setTopic("2")
                .build();

        final AllureResults results = executeException(request);

        assertThat(results.getTestResults()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps()).hasSize(1);
        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains(status.getCode().name());
    }

    protected final AllureResults execute(final Request request) {
        return runWithinTestContext(() -> {
            try {
                final Response response = blockingStub.calculate(request);
                assertThat(response).isEqualTo(RESPONSE_MESSAGE);
            } catch (Exception e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }

    protected final AllureResults executeServerStreaming(final Request request) {
        return runWithinTestContext(() -> {
            try {
                final Queue<Response> responses = new ConcurrentLinkedQueue<>();
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                    @Override
                    public void onNext(Response response) {
                        responses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onCompleted() {
                        countDownLatch.countDown();
                    }
                };

                nonBlockingStub.calculateServerStream(request, responseObserver);
                countDownLatch.await();

                // Can not execute AllureGrpc.await() in common place (@AfterEach method)
                // Because runWithinTestContext() destroys AllureLifecycle after Runnable execution.
                AllureGrpc.await();

                assertThat(responses).allMatch(response -> response.equals(RESPONSE_MESSAGE));
            } catch (Exception e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }

    protected final AllureResults executeClientStreaming(final Queue<Request> requests) {
        return runWithinTestContext(() -> {
            try {
                final Queue<Response> responses = new ConcurrentLinkedQueue<>();
                StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                    @Override
                    public void onNext(Response response) {
                        responses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onCompleted() {
                    }
                };

                StreamObserver<Request> requestObserver = nonBlockingStub.calculateClientStream(responseObserver);
                while (!requests.isEmpty()) {
                    requestObserver.onNext(requests.poll());
                }
                requestObserver.onCompleted();
                responseObserver.onCompleted();

                // Can not execute AllureGrpc.await() in common place (@AfterEach method)
                // Because runWithinTestContext() destroys AllureLifecycle after Runnable execution.
                AllureGrpc.await();

                assertThat(responses).allMatch(response -> response.equals(RESPONSE_MESSAGE));
            } catch (Exception e) {
                throw new RuntimeException("Could not execute bidirectional request " + requests, e);
            }
        });
    }

    protected final AllureResults executeBidirectionalStreaming(final Queue<Request> requests) {
        return runWithinTestContext(() -> {
            try {
                final Queue<Response> responses = new ConcurrentLinkedQueue<>();
                StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                    @Override
                    public void onNext(Response response) {
                        responses.add(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onCompleted() {
                    }
                };

                StreamObserver<Request> requestObserver = nonBlockingStub.calculateBidirectionalStream(responseObserver);
                while (!requests.isEmpty()) {
                    requestObserver.onNext(requests.poll());
                }
                requestObserver.onCompleted();
                responseObserver.onCompleted();

                // Can not execute AllureGrpc.await() in common place (@AfterEach method)
                // Because runWithinTestContext() destroys AllureLifecycle after Runnable execution.
                AllureGrpc.await();

                assertThat(responses).allMatch(response -> response.equals(RESPONSE_MESSAGE));
            } catch (Exception e) {
                throw new RuntimeException("Could not execute bidirectional request " + requests, e);
            }
        });
    }

    protected final AllureResults executeException(final Request request) {
        return runWithinTestContext(() -> {
            assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> blockingStub.calculate(request));
        });
    }
}
