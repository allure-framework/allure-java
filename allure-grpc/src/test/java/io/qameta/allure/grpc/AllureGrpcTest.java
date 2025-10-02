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

            final List<Response> receivedResponses = new ArrayList<>();

            Allure.step("async-root-client-stream", () -> {
                StreamObserver<Response> responseObserver = new StreamObserver<>() {
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
                StreamObserver<Response> responseObserver = new StreamObserver<>() {
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

        String attachmentHtmlContent = readAttachmentContentByName(allureResults, "gRPC request");
        String jsonPayload = extractJsonPayload(attachmentHtmlContent);
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

        String attachmentHtmlContent = readAttachmentContentByName(allureResults, "gRPC response");
        String jsonPayload = extractJsonPayload(attachmentHtmlContent);
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

        String attachmentHtmlContent = readAttachmentContentByName(
            allureResults,
            "gRPC response (collection of elements from Server stream)"
        );
        String jsonPayload = extractJsonPayload(attachmentHtmlContent);
        JsonNode actualJsonArray = JSON.readTree(jsonPayload);

        assertThat(actualJsonArray.isArray()).isTrue();
        assertThat(actualJsonArray.size()).isEqualTo(2);
        assertThat(actualJsonArray.get(0)).isEqualTo(JSON.createObjectNode().put("message", "first"));
        assertThat(actualJsonArray.get(1)).isEqualTo(JSON.createObjectNode().put("message", "second"));
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

    private static String readAttachmentContentByName(AllureResults allureResults, String attachmentName) {
        var test = allureResults.getTestResults().get(0);

        Attachment matchedAttachment = flattenSteps(test.getSteps()).stream()
            .flatMap(step -> step.getAttachments().stream())
            .filter(attachment -> attachmentName.equals(attachment.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Attachment not found: " + attachmentName));

        String attachmentSourceKey = matchedAttachment.getSource();
        Map<String, byte[]> attachmentsContent = allureResults.getAttachments();
        byte[] rawAttachmentContent = attachmentsContent.get(attachmentSourceKey);
        if (rawAttachmentContent == null) {
            throw new IllegalStateException("Attachment content not found by source: " + attachmentSourceKey);
        }
        return new String(rawAttachmentContent, StandardCharsets.UTF_8);
    }

    private static String extractJsonPayload(String htmlContent) {
        String textWithoutHtml = stripHtmlTags(unescapeHtml(htmlContent));
        int fullLength = textWithoutHtml.length();
        for (int currentIndex = 0; currentIndex < fullLength; currentIndex++) {
            char currentChar = textWithoutHtml.charAt(currentIndex);
            if (currentChar == '{' || currentChar == '[') {
                int matchingBracketIndex = findMatchingBracket(textWithoutHtml, currentIndex);
                if (matchingBracketIndex > currentIndex) {
                    String candidateJson = textWithoutHtml.substring(currentIndex, matchingBracketIndex + 1).trim();
                    if (looksLikeJson(candidateJson) && canParseJson(candidateJson)) {
                        return candidateJson;
                    }
                }
            }
        }
        throw new IllegalStateException("JSON payload not found or not valid inside attachment");
    }

    private static boolean canParseJson(String candidateJson) {
        try {
            JSON.readTree(candidateJson);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static boolean looksLikeJson(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return false;
        }
        return trimmed.matches("(?s).*\"[^\"]+\"\\s*:\\s*.*");
    }

    private static int findMatchingBracket(String input, int startIndex) {
        char openingBracket = input.charAt(startIndex);
        char closingBracket = (openingBracket == '{') ? '}' : ']';
        int nestingDepth = 0;
        boolean insideString = false;
        for (int index = startIndex; index < input.length(); index++) {
            char symbol = input.charAt(index);
            if (symbol == '"' && (index == 0 || input.charAt(index - 1) != '\\')) {
                insideString = !insideString;
            }
            if (insideString) {
                continue;
            }
            if (symbol == openingBracket) {
                nestingDepth++;
            } else if (symbol == closingBracket) {
                nestingDepth--;
                if (nestingDepth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static String stripHtmlTags(String input) {
        String withoutTags = input.replaceAll("(?is)<script.*?</script>", "")
            .replaceAll("(?is)<style.*?</style>", "")
            .replaceAll("(?s)<[^>]*>", " ");
        return withoutTags
            .replace("\r", " ")
            .replace("\n", " ")
            .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
            .trim();
    }

    private static String unescapeHtml(String input) {
        return input.replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&#123;", "{")
            .replace("&#125;", "}")
            .replace("&#91;", "[")
            .replace("&#93;", "]")
            .replace("&#58;", ":")
            .replace("&#44;", ",");
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
