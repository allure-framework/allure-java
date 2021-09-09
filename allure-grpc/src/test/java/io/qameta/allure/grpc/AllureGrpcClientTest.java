/*
 *  Copyright 2021 Qameta Software OÜ
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;

/**
 * Tests should cover the creation of a step with the correct name and the creation of attachments
 */
@ExtendWith(GrpcMockExtension.class)
public class AllureGrpcClientTest {

    private ManagedChannel channel;

    @BeforeEach
    void setupChannel() {
        channel = ManagedChannelBuilder.forAddress("localhost", GrpcMock.getGlobalPort())
                .usePlaintext()
                .build();
    }

    final io.qameta.allure.grpc.SimpleRequest request = io.qameta.allure.grpc.SimpleRequest.newBuilder()
            .setBody("Hi Allure!").build();
    final io.qameta.allure.grpc.SimpleResponse response = io.qameta.allure.grpc.SimpleResponse.newBuilder()
            .setBody("Yep hi!").build();

    @Test
    void interceptedMethodAndStepNameIsCorrect() {
        assertThat(execute().getTestResults().get(0).getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("gRPC interaction io.qameta.allure.grpc.Greet/GreetMe");
    }

    @Test
    void interceptedAllData() {
        assertThat(execute().getTestResults().get(0).getSteps().get(0).getAttachments())
                .hasSize(4)
                .flatExtracting(Attachment::getName)
                .containsExactlyInAnyOrder("gRPC request", "gRPC responses", "gRPC headers", "gRPC status");
    }

    @Test
    void formattingRequestIsPretty() {
        final List<String> attachmentValues = new ArrayList<>();
        execute().getAttachments()
                .forEach((k, v) -> attachmentValues.add(new String(v, StandardCharsets.UTF_8)));
        assertThat(attachmentValues).contains("{\n  \"body\": \"Hi Allure!\"\n}");
    }

    protected final AllureResults execute() {
        return runWithinTestContext(
                () -> {
                    stubFor(unaryMethod(io.qameta.allure.grpc.GreetGrpc.getGreetMeMethod())
                            .withRequest(request)
                            .willReturn(response));

                    io.qameta.allure.grpc.SimpleResponse response = io.qameta.allure.grpc.GreetGrpc.newBlockingStub(channel)
                            .withInterceptors(new AllureGrpcClientInterceptor())
                            .greetMe(request);
                    await().until(() -> response != null);
                }, CustomForwardingClientCall::setLifecycle
        );
    }

    @AfterEach
    void shutdownChannel() {
        Optional.ofNullable(channel).ifPresent(ManagedChannel::shutdownNow);
    }
}
