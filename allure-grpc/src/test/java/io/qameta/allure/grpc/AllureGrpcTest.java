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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.grpcmock.GrpcMock.bidiStreamingMethod;
import static org.grpcmock.GrpcMock.clientStreamingMethod;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.unaryMethod;

@ExtendWith(GrpcMockExtension.class)
class AllureGrpcTest {

    private static final String RESPONSE_MESSAGE = "Hello world!";

    private ManagedChannel managedChannel;
    private TestServiceGrpc.TestServiceBlockingStub blockingServiceStub;

    @BeforeEach
    void configureMock() {
        managedChannel = ManagedChannelBuilder.forAddress("localhost", GrpcMock.getGlobalPort())
            .usePlaintext()
            .build();

        blockingServiceStub = TestServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(new AllureGrpc());

        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod())
            .willReturn(Response.newBuilder().setMessage(RESPONSE_MESSAGE).build()));

        GrpcMock.stubFor(serverStreamingMethod(TestServiceGrpc.getCalculateServerStreamMethod())
            .willReturn(asList(
                Response.newBuilder().setMessage(RESPONSE_MESSAGE).build(),
                Response.newBuilder().setMessage(RESPONSE_MESSAGE).build()
            )));

        GrpcMock.stubFor(clientStreamingMethod(TestServiceGrpc.getCalculateClientStreamMethod())
            .willReturn(Response.newBuilder().setMessage(RESPONSE_MESSAGE).build()));

        GrpcMock.stubFor(bidiStreamingMethod(TestServiceGrpc.getCalculateBidiStreamMethod())
            .willProxyTo(responseObserver -> new StreamObserver<>() {
                @Override
                public void onNext(Request request) {
                    responseObserver.onNext(Response.newBuilder().setMessage(RESPONSE_MESSAGE).build());
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

    @AfterEach
    void shutdownChannel() {
        AllureGrpc.await();
        Optional.ofNullable(managedChannel).ifPresent(ManagedChannel::shutdownNow);
    }

    @Test
    void shouldCreateRequestAttachment() {
        Request request = Request.newBuilder()
            .setTopic("1")
            .build();

        Status errorStatus = Status.NOT_FOUND;
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod()).willReturn(errorStatus));

        AllureResults allureResults = executeException(request);

        assertThat(allureResults.getTestResults().get(0).getSteps().get(0).getStatus())
            .isEqualTo(io.qameta.allure.model.Status.FAILED);

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains("gRPC request", "gRPC response");
    }

    @Test
    void shouldCreateResponseAttachment() {
        Request request = Request.newBuilder()
            .setTopic("1")
            .build();

        AllureResults allureResults = execute(request);

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains("gRPC response");
    }

    @Test
    void shouldCreateResponseAttachmentForServerStreamingResponse() {
        Request request = Request.newBuilder()
            .setTopic("1")
            .build();

        AllureResults allureResults = executeStreaming(request);

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains("gRPC response (collection of elements from Server stream)");
    }

    @Test
    void shouldCreateResponseAttachmentOnStatusException() {
        Status status = Status.NOT_FOUND;
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod()).willReturn(status));

        Request request = Request.newBuilder()
            .setTopic("2")
            .build();

        AllureResults allureResults = executeException(request);

        assertThat(allureResults.getTestResults().get(0).getSteps().get(0).getStatus())
            .isEqualTo(io.qameta.allure.model.Status.FAILED);

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains("gRPC response");
    }

    @Test
    void shouldCreateAttachmentsForClientStreamingWithAsynchronousStub() {
        Request requestOne = Request.newBuilder().setTopic("A").build();
        Request requestTwo = Request.newBuilder().setTopic("B").build();

        AllureResults allureResults = runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceStub asynchronousStub =
                TestServiceGrpc.newStub(managedChannel).withInterceptors(new AllureGrpc());

            CountDownLatch completionLatch = new CountDownLatch(1);

            StreamObserver<Response> responseObserver = new StreamObserver<>() {

                @Override
                public void onNext(Response value) {
                    assertThat(value.getMessage()).isEqualTo(RESPONSE_MESSAGE);
                }

                @Override
                public void onError(Throwable throwable) {
                    completionLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    completionLatch.countDown();
                }
            };

            StreamObserver<Request> requestObserver = asynchronousStub.calculateClientStream(responseObserver);
            requestObserver.onNext(requestOne);
            requestObserver.onNext(requestTwo);
            requestObserver.onCompleted();

            try {
                completionLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            AllureGrpc.await();
        });

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .extracting(StepResult::getName)
            .anyMatch(name -> name.startsWith("Send client_streaming gRPC request"));

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains(
                "gRPC request (collection of elements from Client stream)",
                "gRPC response"
            );
    }

    @Test
    void shouldCreateAttachmentsForBidirectionalStreamingWithAsynchronousStub() {
        Request requestOne = Request.newBuilder().setTopic("C").build();
        Request requestTwo = Request.newBuilder().setTopic("D").build();

        AllureResults allureResults = runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceStub asynchronousStub =
                TestServiceGrpc.newStub(managedChannel).withInterceptors(new AllureGrpc());

            CountDownLatch completionLatch = new CountDownLatch(1);
            List<Response> receivedResponses = new ArrayList<>();

            StreamObserver<Response> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(Response value) {
                    receivedResponses.add(value);
                }

                @Override
                public void onError(Throwable throwable) {
                    completionLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    completionLatch.countDown();
                }
            };

            StreamObserver<Request> requestObserver = asynchronousStub.calculateBidiStream(responseObserver);
            requestObserver.onNext(requestOne);
            requestObserver.onNext(requestTwo);
            requestObserver.onCompleted();

            try {
                completionLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            assertThat(receivedResponses).hasSize(2);
            assertThat(receivedResponses.get(0).getMessage()).isEqualTo(RESPONSE_MESSAGE);
            assertThat(receivedResponses.get(1).getMessage()).isEqualTo(RESPONSE_MESSAGE);

            AllureGrpc.await();
        });

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .extracting(StepResult::getName)
            .anyMatch(name -> name.startsWith("Send bidi_streaming gRPC request"));

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains(
                "gRPC request (collection of elements from Client stream)",
                "gRPC response (collection of elements from Server stream)"
            );
    }

    protected final AllureResults execute(Request request) {
        return runWithinTestContext(() -> {
            try {
                Response response = blockingServiceStub.calculate(request);
                assertThat(response.getMessage()).isEqualTo(RESPONSE_MESSAGE);
            } catch (Exception e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }

    protected final AllureResults executeStreaming(Request request) {
        return runWithinTestContext(() -> {
            try {
                Iterator<Response> responseIterator = blockingServiceStub.calculateServerStream(request);
                int responseCount = 0;
                while (responseIterator.hasNext()) {
                    assertThat(responseIterator.next().getMessage()).isEqualTo(RESPONSE_MESSAGE);
                    responseCount++;
                }
                assertThat(responseCount).isEqualTo(2);
            } catch (Exception e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }

    protected final AllureResults executeException(Request request) {
        return runWithinTestContext(() ->
            assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> blockingServiceStub.calculate(request))
        );
    }
}
