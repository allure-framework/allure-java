/*
 *  Copyright 2019 Qameta Software OÜ
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
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.test.AllureResults;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;
import java.util.Optional;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.grpcmock.GrpcMock.bidiStreamingMethod;
import static org.grpcmock.GrpcMock.clientStreamingMethod;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.unaryMethod;

/**
 * @author dtuchs (Dmitry Tuchs).
 */
@ExtendWith(GrpcMockExtension.class)
class AllureGrpcTest {

    private static final String RESPONSE_MESSAGE = "Hello world!";

    private ManagedChannel channel;
    private TestServiceGrpc.TestServiceBlockingStub blockingStub;

    @BeforeEach
    void configureMock() {
        channel = ManagedChannelBuilder.forAddress("localhost", GrpcMock.getGlobalPort())
                .usePlaintext()
                .build();
        blockingStub = TestServiceGrpc.newBlockingStub(channel)
                .withInterceptors(new AllureGrpc());

        GrpcMock.stubFor(unaryMethod(TestServiceGrpc.getCalculateMethod())
                .willReturn(Response.newBuilder().setMessage(RESPONSE_MESSAGE).build()));
        GrpcMock.stubFor(serverStreamingMethod(TestServiceGrpc.getCalculateServerStreamMethod())
                .willReturn(asList(
                        Response.newBuilder().setMessage(RESPONSE_MESSAGE).build(),
                        Response.newBuilder().setMessage(RESPONSE_MESSAGE).build()
                )));
    }

    @AfterEach
    void shutdownChannel() {
        Optional.ofNullable(channel).ifPresent(ManagedChannel::shutdownNow);
    }

    @Test
    void shouldCreateRequestAttachment() {
        final Request request = Request.newBuilder()
                .setTopic("1")
                .build();

        final AllureResults results = execute(request);

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

        final AllureResults results = executeStreaming(request);

        assertThat(results.getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getAttachments)
                .extracting(Attachment::getName)
                .contains("gRPC response (collection of elements from Server stream)");
    }

    protected final AllureResults execute(final Request request) {
        return runWithinTestContext(() -> {
            try {
                final Response response = blockingStub.calculate(request);
                assertThat(response.getMessage()).isEqualTo(RESPONSE_MESSAGE);
            } catch (Exception e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }

    protected final AllureResults executeStreaming(final Request request) {
        return runWithinTestContext(() -> {
            try {
                Iterator<Response> responseIterator = blockingStub.calculateServerStream(request);
                assertThat(responseIterator.next().getMessage()).isEqualTo(RESPONSE_MESSAGE);
            } catch (Exception e) {
                throw new RuntimeException("Could not execute request " + request, e);
            }
        });
    }
}
