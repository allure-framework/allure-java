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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.CallCredentials;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientStreamTracer;
import io.grpc.ForwardingServerCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.grpcmock.GrpcMock.bidiStreamingMethod;
import static org.grpcmock.GrpcMock.clientStreamingMethod;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.unaryMethod;

@IsolatedLifecycle
class AllureGrpcTest {

    private static final String RESPONSE_MESSAGE = "Hello world!";
    private static final String GRPC_EXCHANGE = "gRPC exchange";
    private static final String REDACTED_VALUE = "__ALLURE_REDACTED__";
    private static final Metadata.Key<String> REQUEST_ID = asciiKey("x-request-id");
    private static final Metadata.Key<String> REPEATED_REQUEST = asciiKey("x-request-repeated");
    private static final Metadata.Key<byte[]> BINARY_REQUEST = binaryKey("x-request-bin");
    private static final Metadata.Key<String> AUTHORIZATION = asciiKey("authorization");
    private static final Metadata.Key<String> API_KEY = asciiKey("x-api-key");
    private static final Metadata.Key<String> COOKIE = asciiKey("cookie");
    private static final Metadata.Key<String> RESPONSE_HEADER = asciiKey("x-response-repeated");
    private static final Metadata.Key<String> RESPONSE_TRAILER = asciiKey("x-response-trailer");
    private static final Metadata.Key<String> SET_COOKIE = asciiKey("set-cookie");
    private static final ObjectMapper JSON = new ObjectMapper();

    @RegisterExtension
    static final GrpcMockExtension GRPC_MOCK = GrpcMockExtension.builder()
            .withInterceptor(new ResponseMetadataInterceptor())
            .build();

    private ManagedChannel managedChannel;

    @BeforeEach
    void configureMockServer() {
        managedChannel = ManagedChannelBuilder
                .forAddress("localhost", GrpcMock.getGlobalPort())
                .usePlaintext()
                .directExecutor()
                .build();

        GrpcMock.stubFor(
                unaryMethod(TestServiceGrpc.getCalculateMethod())
                        .willReturn(Response.newBuilder().setMessage(RESPONSE_MESSAGE).build())
        );

        GrpcMock.stubFor(
                serverStreamingMethod(TestServiceGrpc.getCalculateServerStreamMethod())
                        .willReturn(
                                asList(
                                        Response.newBuilder().setMessage(RESPONSE_MESSAGE).build(),
                                        Response.newBuilder().setMessage(RESPONSE_MESSAGE).build()
                                )
                        )
        );

        GrpcMock.stubFor(
                clientStreamingMethod(TestServiceGrpc.getCalculateClientStreamMethod())
                        .willReturn(Response.newBuilder().setMessage(RESPONSE_MESSAGE).build())
        );

        GrpcMock.stubFor(
                bidiStreamingMethod(TestServiceGrpc.getCalculateBidiStreamMethod())
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
                        })
        );
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
                .contains(GRPC_EXCHANGE);
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
                .contains(GRPC_EXCHANGE);
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
                .contains(GRPC_EXCHANGE);
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
                .contains(GRPC_EXCHANGE);
    }

    @Test
    void shouldCreateAttachmentsForClientStreamingWithAsynchronousStub() {
        Request firstClientRequest = Request.newBuilder().setTopic("A").build();
        Request secondClientRequest = Request.newBuilder().setTopic("B").build();

        final AllureResults results = Allure.step(
                "Execute asynchronous client-streaming gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    final TestServiceGrpc.TestServiceStub asynchronousStub = TestServiceGrpc.newStub(managedChannel).withInterceptors(new AllureGrpc());

                    final List<Response> receivedResponses = new ArrayList<Response>();
                    final CountDownLatch streamCompleted = new CountDownLatch(1);
                    final AtomicReference<Throwable> streamError = new AtomicReference<>();

                    Allure.step("async-root-client-stream", () -> {
                        StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                            @Override
                            public void onNext(Response value) {
                                receivedResponses.add(value);
                            }
                            @Override
                            public void onError(Throwable throwable) {
                                streamError.set(throwable);
                                streamCompleted.countDown();
                            }
                            @Override
                            public void onCompleted() {
                                streamCompleted.countDown();
                            }
                        };

                        StreamObserver<Request> requestObserver = asynchronousStub.calculateClientStream(
                                responseObserver
                        );
                        requestObserver.onNext(firstClientRequest);
                        requestObserver.onNext(secondClientRequest);
                        requestObserver.onCompleted();
                    });

                    awaitStreamCompletion(streamCompleted, streamError);
                    assertThat(receivedResponses).hasSize(1);
                    assertThat(receivedResponses.get(0).getMessage()).isEqualTo(RESPONSE_MESSAGE);
                })
        );

        Allure.step("Verify asynchronous client-streaming exchange evidence", () -> {
            assertThat(results.getTestResults().get(0).getSteps())
                    .extracting(StepResult::getName)
                    .contains("async-root-client-stream");
            assertThat(results.getAttachmentsRecursively())
                    .extracting(Attachment::getName)
                    .contains(GRPC_EXCHANGE);
        });
    }

    @Test
    void shouldCreateAttachmentsForBidirectionalStreamingWithAsynchronousStub() {
        Request firstBidirectionalRequest = Request.newBuilder().setTopic("C").build();
        Request secondBidirectionalRequest = Request.newBuilder().setTopic("D").build();

        final AllureResults results = Allure.step(
                "Execute asynchronous bidirectional gRPC stream and collect Allure results",
                () -> runWithinTestContext(() -> {
                    final TestServiceGrpc.TestServiceStub asynchronousStub = TestServiceGrpc.newStub(managedChannel).withInterceptors(new AllureGrpc());

                    final List<Response> receivedResponses = new ArrayList<>();
                    final CountDownLatch streamCompleted = new CountDownLatch(1);
                    final AtomicReference<Throwable> streamError = new AtomicReference<>();

                    Allure.step("async-root-bidi-stream", () -> {
                        StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
                            @Override
                            public void onNext(Response value) {
                                receivedResponses.add(value);
                            }
                            @Override
                            public void onError(Throwable throwable) {
                                streamError.set(throwable);
                                streamCompleted.countDown();
                            }
                            @Override
                            public void onCompleted() {
                                streamCompleted.countDown();
                            }
                        };

                        StreamObserver<Request> requestObserver = asynchronousStub.calculateBidiStream(
                                responseObserver
                        );
                        requestObserver.onNext(firstBidirectionalRequest);
                        requestObserver.onNext(secondBidirectionalRequest);
                        requestObserver.onCompleted();
                    });

                    awaitStreamCompletion(streamCompleted, streamError);
                    assertThat(receivedResponses).hasSize(2);
                    assertThat(receivedResponses.get(0).getMessage()).isEqualTo(RESPONSE_MESSAGE);
                    assertThat(receivedResponses.get(1).getMessage()).isEqualTo(RESPONSE_MESSAGE);
                })
        );

        Allure.step("Verify asynchronous bidirectional exchange evidence", () -> {
            assertThat(results.getTestResults().get(0).getSteps())
                    .extracting(StepResult::getName)
                    .contains("async-root-bidi-stream");
            assertThat(results.getAttachmentsRecursively())
                    .extracting(Attachment::getName)
                    .contains(GRPC_EXCHANGE);
        });
    }

    @Test
    void unaryRequestBodyIsCapturedAsJsonObject() throws Exception {
        GrpcMock.stubFor(
                unaryMethod(TestServiceGrpc.getCalculateMethod())
                        .willReturn(Response.newBuilder().setMessage("ok").build())
        );

        Request request = Request.newBuilder().setTopic("topic-1").build();

        AllureResults allureResults = Allure.step(
                "Execute unary gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                    Response response = stub.calculate(request);
                    assertThat(response.getMessage()).isEqualTo("ok");
                })
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);
        JsonNode actualJsonNode = JSON.readTree(exchange.at("/request/body/value").asText());
        JsonNode expectedJsonNode = JSON.createObjectNode().put("topic", "topic-1");

        assertThat(actualJsonNode).isEqualTo(expectedJsonNode);
        assertThat(exchange.at("/request/method").asText()).isEqualTo("POST");
        assertThat(exchange.at("/request/httpVersion").asText()).isEqualTo("HTTP/2");
        assertThat(exchange.at("/request/body/contentType").asText()).isEqualTo("application/grpc+json");
    }

    /**
     * Request metadata added by call credentials and response metadata are captured by default, including repeated
     * and binary values.
     */
    @Test
    void metadataIsCapturedByDefaultAfterCredentialsAreApplied() throws Exception {
        Metadata credentialMetadata = new Metadata();
        credentialMetadata.put(REQUEST_ID, "request-42");
        credentialMetadata.put(REPEATED_REQUEST, "first");
        credentialMetadata.put(REPEATED_REQUEST, "second");
        credentialMetadata.put(BINARY_REQUEST, "binary-value".getBytes(StandardCharsets.UTF_8));
        credentialMetadata.put(AUTHORIZATION, "Bearer secret");
        credentialMetadata.put(COOKIE, "session=request-secret; theme=dark");

        AllureResults allureResults = executeUnaryWithConfiguration(
                Request.newBuilder().setTopic("metadata-defaults").build(),
                AllureGrpc::new,
                callCredentials(credentialMetadata)
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);

        assertThat(findValuesByName(exchange.at("/request/headers"), REQUEST_ID.name()))
                .containsExactly("request-42");
        assertThat(findValuesByName(exchange.at("/request/headers"), REPEATED_REQUEST.name()))
                .containsExactly("first", "second");
        assertThat(findValuesByName(exchange.at("/request/headers"), BINARY_REQUEST.name()))
                .containsExactly("YmluYXJ5LXZhbHVl");
        assertThat(findValuesByName(exchange.at("/request/headers"), AUTHORIZATION.name()))
                .containsExactly(REDACTED_VALUE);
        assertThat(findValuesByName(exchange.at("/request/headers"), COOKIE.name())).isEmpty();
        assertThat(findValueByName(exchange.at("/request/cookies"), "session"))
                .contains("request-secret");
        assertThat(findValueByName(exchange.at("/request/cookies"), "theme"))
                .contains("dark");
        assertThat(findValuesByName(exchange.at("/response/headers"), RESPONSE_HEADER.name()))
                .containsExactly("first", "second");
        assertThat(findValuesByName(exchange.at("/response/headers"), SET_COOKIE.name())).isEmpty();
        assertThat(findValueByName(exchange.at("/response/cookies"), "session"))
                .contains("response-secret");
        assertThat(findValueByName(exchange.at("/response/cookies"), "theme"))
                .contains("light");
        assertThat(findValuesByName(exchange.at("/response/trailers"), RESPONSE_TRAILER.name()))
                .containsExactly("trailer-value");

        JsonNode responseSession = findByName(exchange.at("/response/cookies"), "session").orElseThrow();
        assertThat(responseSession.path("path").asText()).isEqualTo("/");
        assertThat(responseSession.path("domain").asText()).isEqualTo("example.test");
        assertThat(responseSession.path("expires").asText()).isEqualTo("Wed, 21 Oct 2015 07:28:00 GMT");
        assertThat(responseSession.path("httpOnly").asBoolean()).isTrue();
        assertThat(responseSession.path("secure").asBoolean()).isTrue();
        assertThat(responseSession.path("sameSite").asText()).isEqualTo("Lax");
    }

    /**
     * Metadata applied after tracer creation is refreshed when a transport-level failing stream closes without ever
     * invoking {@link ClientStreamTracer#streamCreated(io.grpc.Attributes, Metadata)}.
     */
    @Test
    void finalRequestMetadataIsCapturedWhenTransportStreamCreationFails() throws Exception {
        AllureResults allureResults = Allure.step(
                "Execute a gRPC request whose transport stream cannot be created",
                () -> runWithinTestContext(() -> {
                    ClientCall<Request, Response> call = new AllureGrpc().interceptCall(
                            TestServiceGrpc.getCalculateMethod(),
                            CallOptions.DEFAULT,
                            failingTransportChannel()
                    );
                    call.start(new ClientCall.Listener<>() {
                    }, new Metadata());
                })
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);

        assertThat(findValuesByName(exchange.at("/request/headers"), REQUEST_ID.name()))
                .containsExactly("failed-request-42");
    }

    /**
     * Callers can turn request and response metadata capture off independently of message and status capture.
     */
    @Test
    void metadataCaptureCanBeDisabled() throws Exception {
        Metadata credentialMetadata = new Metadata();
        credentialMetadata.put(REQUEST_ID, "request-42");

        AllureResults allureResults = executeUnaryWithConfiguration(
                Request.newBuilder().setTopic("metadata-disabled").build(),
                () -> AllureGrpc.builder()
                        .captureRequestMetadata(false)
                        .captureResponseMetadata(false)
                        .build(),
                callCredentials(credentialMetadata)
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);

        assertThat(findValuesByName(exchange.at("/request/headers"), REQUEST_ID.name())).isEmpty();
        assertThat(findValuesByName(exchange.at("/response/headers"), RESPONSE_HEADER.name())).isEmpty();
        assertThat(findValuesByName(exchange.at("/response/trailers"), RESPONSE_TRAILER.name())).isEmpty();
        assertThat(findValueByName(exchange.at("/response/trailers"), "grpc-status")).contains("0");
    }

    /**
     * Header and structured-cookie redaction configured on the gRPC builder is applied by the HTTP exchange builder.
     */
    @Test
    void configuredHeadersAndCookiesAreRedacted() throws Exception {
        Metadata credentialMetadata = new Metadata();
        credentialMetadata.put(API_KEY, "api-secret");
        credentialMetadata.put(COOKIE, "theme=dark; session=request-secret");

        AllureResults allureResults = executeUnaryWithConfiguration(
                Request.newBuilder().setTopic("metadata-redaction").build(),
                () -> AllureGrpc.builder()
                        .configureExchange(HttpExchange.Builder::clearRedactedHeaders)
                        .redactHeader(API_KEY.name())
                        .redactCookie("session")
                        .build(),
                callCredentials(credentialMetadata)
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);

        assertThat(findValuesByName(exchange.at("/request/headers"), API_KEY.name()))
                .containsExactly(REDACTED_VALUE);
        assertThat(findValuesByName(exchange.at("/request/headers"), COOKIE.name())).isEmpty();
        assertThat(findValueByName(exchange.at("/request/cookies"), "session"))
                .contains(REDACTED_VALUE);
        assertThat(findValueByName(exchange.at("/request/cookies"), "theme"))
                .contains("dark");
        assertThat(findValuesByName(exchange.at("/response/headers"), SET_COOKIE.name())).isEmpty();
        assertThat(findValueByName(exchange.at("/response/cookies"), "session"))
                .contains(REDACTED_VALUE);
        assertThat(findValueByName(exchange.at("/response/cookies"), "theme"))
                .contains("light");
    }

    @Test
    void unaryResponseBodyIsCapturedAsJsonObject() throws Exception {
        GrpcMock.stubFor(
                unaryMethod(TestServiceGrpc.getCalculateMethod())
                        .willReturn(Response.newBuilder().setMessage("hello-world").build())
        );

        Request request = Request.newBuilder().setTopic("x").build();

        AllureResults allureResults = Allure.step(
                "Execute unary gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                    Response response = stub.calculate(request);
                    assertThat(response.getMessage()).isEqualTo("hello-world");
                })
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);
        JsonNode actualJsonNode = JSON.readTree(exchange.at("/response/body/value").asText());
        JsonNode expectedJsonNode = JSON.createObjectNode().put("message", "hello-world");

        assertThat(actualJsonNode).isEqualTo(expectedJsonNode);
        assertThat(findValueByName(exchange.at("/response/trailers"), "grpc-status")).contains("0");
    }

    @Test
    void serverStreamingResponseBodyIsJsonArrayInOrder() throws Exception {
        GrpcMock.stubFor(
                serverStreamingMethod(TestServiceGrpc.getCalculateServerStreamMethod())
                        .willReturn(
                                asList(
                                        Response.newBuilder().setMessage("first").build(),
                                        Response.newBuilder().setMessage("second").build()
                                )
                        )
        );

        Request request = Request.newBuilder().setTopic("stream-topic").build();

        AllureResults allureResults = Allure.step(
                "Execute server-streaming gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                    Iterator<Response> responseIterator = stub.calculateServerStream(request);
                    assertThat(responseIterator.hasNext()).isTrue();
                    assertThat(responseIterator.next().getMessage()).isEqualTo("first");
                    assertThat(responseIterator.hasNext()).isTrue();
                    assertThat(responseIterator.next().getMessage()).isEqualTo("second");
                    assertThat(responseIterator.hasNext()).isFalse();
                })
        );

        JsonNode exchange = readGrpcExchangeAttachment(allureResults);
        JsonNode actualJsonArray = JSON.readTree(exchange.at("/response/body/value").asText());

        assertThat(actualJsonArray.isArray()).isTrue();
        assertThat(actualJsonArray.size()).isEqualTo(2);
        assertThat(actualJsonArray.get(0)).isEqualTo(JSON.createObjectNode().put("message", "first"));
        assertThat(actualJsonArray.get(1)).isEqualTo(JSON.createObjectNode().put("message", "second"));
        assertThat(exchange.at("/response/body/stream/type").asText()).isEqualTo("grpc");
        assertThat(exchange.at("/response/body/stream/chunkCount").asLong()).isEqualTo(2L);
    }

    private static void awaitStreamCompletion(
                                              final CountDownLatch streamCompleted,
                                              final AtomicReference<Throwable> streamError) {
        Allure.step("Wait for asynchronous gRPC stream completion", () -> {
            assertThat(streamCompleted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(streamError.get()).isNull();
        });
    }

    private static JsonNode readGrpcExchangeAttachment(AllureResults allureResults) throws Exception {
        List<Attachment> attachments = allureResults.getAttachmentsRecursively();
        assertThat(attachments)
                .extracting(Attachment::getType)
                .doesNotContain("text/html");

        Attachment matchedAttachment = attachments.stream()
                .filter(attachment -> GRPC_EXCHANGE.equals(attachment.getName()))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Attachment not found: " + GRPC_EXCHANGE
                                        + ", attachments=" + attachments
                                        + ", raw=" + allureResults.getAttachments().keySet()
                        )
                );

        assertThat(matchedAttachment.getType()).isEqualTo(HttpExchange.CONTENT_TYPE);
        assertThat(matchedAttachment.getSource()).endsWith(HttpExchange.FILE_EXTENSION);

        return JSON.readTree(allureResults.getAttachmentContentAsString(matchedAttachment));
    }

    private static Optional<String> findValueByName(final JsonNode values, final String name) {
        return findByName(values, name).map(value -> value.path("value").asText());
    }

    private static Optional<JsonNode> findByName(final JsonNode values, final String name) {
        for (JsonNode value : values) {
            if (name.equals(value.path("name").asText())) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static List<String> findValuesByName(final JsonNode values, final String name) {
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (name.equals(value.path("name").asText())) {
                result.add(value.path("value").asText());
            }
        }
        return result;
    }

    private static Metadata.Key<String> asciiKey(final String name) {
        return Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
    }

    private static Metadata.Key<byte[]> binaryKey(final String name) {
        return Metadata.Key.of(name, Metadata.BINARY_BYTE_MARSHALLER);
    }

    private static CallCredentials callCredentials(final Metadata source) {
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(
                                             final RequestInfo requestInfo,
                                             final Executor appExecutor,
                                             final MetadataApplier applier) {
                appExecutor.execute(() -> {
                    Metadata metadata = new Metadata();
                    metadata.merge(source);
                    applier.apply(metadata);
                });
            }
        };
    }

    private static Channel failingTransportChannel() {
        return new Channel() {
            @Override
            public String authority() {
                return "unavailable.example";
            }

            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                                                                 final MethodDescriptor<ReqT, RespT> methodDescriptor,
                                                                 final CallOptions callOptions) {
                return new ClientCall<>() {
                    @Override
                    public void start(final Listener<RespT> responseListener, final Metadata headers) {
                        final List<ClientStreamTracer.Factory> factories = callOptions.getStreamTracerFactories();
                        final ClientStreamTracer.Factory factory = factories.get(factories.size() - 1);
                        final ClientStreamTracer tracer = factory.newClientStreamTracer(
                                ClientStreamTracer.StreamInfo.newBuilder()
                                        .setCallOptions(callOptions)
                                        .build(),
                                headers
                        );

                        headers.put(REQUEST_ID, "failed-request-42");
                        tracer.streamClosed(Status.UNAVAILABLE);
                        responseListener.onClose(Status.UNAVAILABLE, new Metadata());
                    }

                    @Override
                    public void request(final int numMessages) {
                    }

                    @Override
                    public void cancel(final String message, final Throwable cause) {
                    }

                    @Override
                    public void halfClose() {
                    }

                    @Override
                    public void sendMessage(final ReqT message) {
                    }
                };
            }
        };
    }

    private AllureResults executeUnaryWithConfiguration(
                                                        final Request request,
                                                        final Supplier<AllureGrpc> interceptor,
                                                        final CallCredentials credentials) {
        return Allure.step(
                "Execute configured unary gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel)
                            .withInterceptors(interceptor.get())
                            .withCallCredentials(credentials);
                    Response response = stub.calculate(request);
                    assertThat(response.getMessage()).isEqualTo(RESPONSE_MESSAGE);
                })
        );
    }

    protected final AllureResults executeUnary(Request request) {
        return Allure.step(
                "Execute unary gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    try {
                        TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
                        Response response = stub.calculate(request);
                        assertThat(response.getMessage()).isEqualTo(RESPONSE_MESSAGE);
                    } catch (Exception exception) {
                        throw new RuntimeException("Could not execute request " + request, exception);
                    }
                })
        );
    }

    protected final AllureResults executeServerStreaming(Request request) {
        return Allure.step(
                "Execute server-streaming gRPC request and collect Allure results",
                () -> runWithinTestContext(() -> {
                    try {
                        TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel).withInterceptors(new AllureGrpc());
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
                })
        );
    }

    protected final AllureResults executeUnaryExpectingException(Request request) {
        return Allure.step(
                "Execute unary gRPC request expecting a status exception and collect Allure results",
                () -> runWithinTestContext(
                        () -> assertThatExceptionOfType(StatusRuntimeException.class)
                                .isThrownBy(() -> {
                                    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel)
                                            .withInterceptors(new AllureGrpc());
                                    Response response = stub.calculate(request);
                                    assertThat(response.getMessage()).isEqualTo("ok");
                                })
                )
        );
    }

    private static final class ResponseMetadataInterceptor implements ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                                                                     final ServerCall<ReqT, RespT> call,
                                                                     final Metadata headers,
                                                                     final ServerCallHandler<ReqT, RespT> next) {
            final ServerCall<ReqT, RespT> forwardingCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void sendHeaders(final Metadata responseHeaders) {
                    responseHeaders.put(RESPONSE_HEADER, "first");
                    responseHeaders.put(RESPONSE_HEADER, "second");
                    responseHeaders.put(
                            SET_COOKIE,
                            "session=response-secret; Path=/; Domain=example.test; "
                                    + "Expires=Wed, 21 Oct 2015 07:28:00 GMT; HttpOnly; Secure; SameSite=Lax"
                    );
                    responseHeaders.put(SET_COOKIE, "theme=light; Path=/");
                    super.sendHeaders(responseHeaders);
                }

                @Override
                public void close(final Status status, final Metadata trailers) {
                    trailers.put(RESPONSE_TRAILER, "trailer-value");
                    super.close(status, trailers);
                }
            };
            return next.startCall(forwardingCall, headers);
        }
    }

}
