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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static final ObjectMapper JSON = new ObjectMapper();

    private ManagedChannel managedChannel;

    @BeforeEach
    void configureMockServer() {
        managedChannel = ManagedChannelBuilder
            .forAddress("localhost", GrpcMock.getGlobalPort())
            .usePlaintext()
            .directExecutor()
            .build();

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
            .willProxyTo(responseObserver -> new StreamObserver<Request>() {
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
        Optional.ofNullable(managedChannel).ifPresent(ManagedChannel::shutdown);
    }

    @Test
    void shouldCreateRequestAttachment() {
        Request request = Request.newBuilder()
            .setTopic("1")
            .build();

        Status errorStatus = Status.NOT_FOUND;
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod()).willReturn(errorStatus));

        AllureResults allureResults = executeUnaryExpectingException(request);

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

        AllureResults allureResults = executeUnary(request);

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

        AllureResults allureResults = executeServerStreaming(request);

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains("gRPC response (collection of elements from Server stream)");
    }

    @Test
    void shouldCreateResponseAttachmentOnStatusException() {
        Status notFoundStatus = Status.NOT_FOUND;
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod()).willReturn(notFoundStatus));

        Request request = Request.newBuilder()
            .setTopic("2")
            .build();

        AllureResults allureResults = executeUnaryExpectingException(request);

        assertThat(allureResults.getTestResults().get(0).getSteps().get(0).getStatus())
            .isEqualTo(io.qameta.allure.model.Status.FAILED);

        assertThat(allureResults.getTestResults().get(0).getSteps())
            .flatExtracting(StepResult::getAttachments)
            .extracting(Attachment::getName)
            .contains("gRPC response");
    }

    @Test
    void shouldCreateAttachmentsForClientStreamingWithAsynchronousStub() {
        Request firstClientRequest = Request.newBuilder().setTopic("A").build();
        Request secondClientRequest = Request.newBuilder().setTopic("B").build();

        runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceStub asynchronousStub =
                TestServiceGrpc.newStub(managedChannel).withInterceptors(new AllureGrpc());

            final List<Response> receivedResponses = new ArrayList<Response>();

            Allure.step("async-root-client-stream", () -> {
                StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                    @Override
                    public void onNext(Response value) {
                        receivedResponses.add(value);
                    }
                    @Override
                    public void onError(Throwable throwable) {
                    }
                    @Override
                    public void onCompleted() {
                    }
                };

                StreamObserver<Request> requestObserver = asynchronousStub.calculateClientStream(responseObserver);
                requestObserver.onNext(firstClientRequest);
                requestObserver.onNext(secondClientRequest);
                requestObserver.onCompleted();
            });

            assertThat(receivedResponses).hasSize(1);
            assertThat(receivedResponses.get(0).getMessage()).isEqualTo(RESPONSE_MESSAGE);
        });
    }

    @Test
    void shouldCreateAttachmentsForBidirectionalStreamingWithAsynchronousStub() {
        Request firstBidirectionalRequest = Request.newBuilder().setTopic("C").build();
        Request secondBidirectionalRequest = Request.newBuilder().setTopic("D").build();

        runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceStub asynchronousStub =
                TestServiceGrpc.newStub(managedChannel).withInterceptors(new AllureGrpc());

            List<Response> receivedResponses = new ArrayList<>();

            Allure.step("async-root-bidi-stream", () -> {
                StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                    @Override public void onNext(Response value) { receivedResponses.add(value); }
                    @Override public void onError(Throwable throwable) { }
                    @Override public void onCompleted() { }
                };

                StreamObserver<Request> requestObserver = asynchronousStub.calculateBidiStream(responseObserver);
                requestObserver.onNext(firstBidirectionalRequest);
                requestObserver.onNext(secondBidirectionalRequest);
                requestObserver.onCompleted();
            });

            assertThat(receivedResponses).hasSize(2);
            assertThat(receivedResponses.get(0).getMessage()).isEqualTo(RESPONSE_MESSAGE);
            assertThat(receivedResponses.get(1).getMessage()).isEqualTo(RESPONSE_MESSAGE);
        });
    }

    @Test
    void unaryRequestBodyIsCapturedAsJsonObject() throws Exception {
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod())
            .willReturn(Response.newBuilder().setMessage("ok").build()));

        Request request = Request.newBuilder().setTopic("topic-1").build();

        AllureResults allureResults = runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceBlockingStub stub =
                TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
            Response response = stub.calculate(request);
            assertThat(response.getMessage()).isEqualTo("ok");
        });

        String jsonPayload = readJsonAttachmentByName(allureResults, "gRPC request (json)");
        JsonNode actualJsonNode = JSON.readTree(jsonPayload);
        JsonNode expectedJsonNode = JSON.createObjectNode().put("topic", "topic-1");

        assertThat(actualJsonNode).isEqualTo(expectedJsonNode);
    }

    @Test
    void unaryResponseBodyIsCapturedAsJsonObject() throws Exception {
        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod())
            .willReturn(Response.newBuilder().setMessage("hello-world").build()));

        Request request = Request.newBuilder().setTopic("x").build();

        AllureResults allureResults = runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceBlockingStub stub =
                TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
            Response response = stub.calculate(request);
            assertThat(response.getMessage()).isEqualTo("hello-world");
        });

        String jsonPayload = readJsonAttachmentByName(allureResults, "gRPC response (json)");
        JsonNode actualJsonNode = JSON.readTree(jsonPayload);
        JsonNode expectedJsonNode = JSON.createObjectNode().put("message", "hello-world");

        assertThat(actualJsonNode).isEqualTo(expectedJsonNode);
    }

    @Test
    void serverStreamingResponseBodyIsJsonArrayInOrder() throws Exception {
        GrpcMock.stubFor(serverStreamingMethod(TestServiceGrpc.getCalculateServerStreamMethod())
            .willReturn(asList(
                Response.newBuilder().setMessage("first").build(),
                Response.newBuilder().setMessage("second").build()
            )));

        Request request = Request.newBuilder().setTopic("stream-topic").build();

        AllureResults allureResults = runWithinTestContext(() -> {
            TestServiceGrpc.TestServiceBlockingStub stub =
                TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
            Iterator<Response> responseIterator = stub.calculateServerStream(request);
            assertThat(responseIterator.hasNext()).isTrue();
            assertThat(responseIterator.next().getMessage()).isEqualTo("first");
            assertThat(responseIterator.hasNext()).isTrue();
            assertThat(responseIterator.next().getMessage()).isEqualTo("second");
            assertThat(responseIterator.hasNext()).isFalse();
        });

        String jsonPayload = readJsonAttachmentByName(
            allureResults, "gRPC response (collection of elements from Server stream) (json)"
        );
        JsonNode actualJsonArray = JSON.readTree(jsonPayload);

        assertThat(actualJsonArray.isArray()).isTrue();
        assertThat(actualJsonArray.size()).isEqualTo(2);
        assertThat(actualJsonArray.get(0)).isEqualTo(JSON.createObjectNode().put("message", "first"));
        assertThat(actualJsonArray.get(1)).isEqualTo(JSON.createObjectNode().put("message", "second"));
    }
    private static String readJsonAttachmentByName(AllureResults allureResults, String jsonAttachmentName) {
        TestResult test = allureResults.getTestResults().get(0);

        Attachment matchedAttachment = flattenSteps(test.getSteps()).stream()
            .flatMap(step -> step.getAttachments().stream())
            .filter(attachment -> jsonAttachmentName.equals(attachment.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Attachment not found: " + jsonAttachmentName));

        String attachmentSourceKey = matchedAttachment.getSource();
        Map<String, byte[]> attachmentsContent = allureResults.getAttachments();
        byte[] rawAttachmentContent = attachmentsContent.get(attachmentSourceKey);
        if (rawAttachmentContent == null) {
            throw new IllegalStateException("Attachment content not found by source: " + attachmentSourceKey);
        }
        return new String(rawAttachmentContent, StandardCharsets.UTF_8);
    }

    protected final AllureResults executeUnary(Request request) {
        return runWithinTestContext(() -> {
            try {
                TestServiceGrpc.TestServiceBlockingStub stub =
                    TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                Response response = stub.calculate(request);
                assertThat(response.getMessage()).isEqualTo(RESPONSE_MESSAGE);
            } catch (Exception exception) {
                throw new RuntimeException("Could not execute request " + request, exception);
            }
        });
    }

    protected final AllureResults executeServerStreaming(Request request) {
        return runWithinTestContext(() -> {
            try {
                TestServiceGrpc.TestServiceBlockingStub stub =
                    TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                Iterator<Response> responseIterator = stub.calculateServerStream(request);
                int responseCount = 0;
                while (responseIterator.hasNext()) {
                    assertThat(responseIterator.next().getMessage()).isEqualTo(RESPONSE_MESSAGE);
                    responseCount++;
                }
                assertThat(responseCount).isEqualTo(2);
            } catch (Exception exception) {
                throw new RuntimeException("Could not execute request " + request, exception);
            }
        });
    }

    protected final AllureResults executeUnaryExpectingException(Request request) {
        return runWithinTestContext(() ->
            assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> {
                    TestServiceGrpc.TestServiceBlockingStub stub =
                        TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                    Response response = stub.calculate(request);
                    assertThat(response.getMessage()).isEqualTo("ok");
                })
        );
    }

    private static List<StepResult> flattenSteps(List<StepResult> rootSteps) {
        List<StepResult> allSteps = new ArrayList<>();
        if (rootSteps == null) {
            return allSteps;
        }
        for (StepResult step : rootSteps) {
            allSteps.add(step);
            allSteps.addAll(flattenSteps(step.getSteps()));
        }
        return allSteps;
    }
}
