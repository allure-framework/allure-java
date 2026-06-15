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
package io.qameta.allure.test;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AllureResultsWriterStubTest {

    @Test
    void shouldStoreResultsContainersAndAttachments() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final TestResult testResult = new TestResult()
                .setUuid("test-uuid")
                .setName("demo");
        final TestResultContainer container = new TestResultContainer()
                .setUuid("container-uuid")
                .setChildren(List.of("test-uuid"));

        Allure.step("Store a test result, its container, and an attachment", () -> {
            writer.write(testResult);
            writer.write(container);
            writer.write("payload.txt", new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)));
        });

        Allure.step("Verify the stub exposes the written runtime artifacts", () -> {
            assertThat(writer.getTestResultByName("demo"))
                    .isSameAs(testResult);
            assertThat(writer.getTestResultContainersForTestResult(testResult))
                    .containsExactly(container);
            assertThat(writer.getAttachments().get("payload.txt"))
                    .isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void shouldExposeNestedAttachmentsAndContent() {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final Attachment rootAttachment = new Attachment()
                .setName("root payload")
                .setSource("root.txt");
        final Attachment nestedAttachment = new Attachment()
                .setName("nested payload")
                .setSource("nested.txt");
        final TestResult testResult = new TestResult()
                .setUuid("test-uuid")
                .setName("demo")
                .setAttachments(List.of(rootAttachment))
                .setSteps(
                        List.of(
                                new StepResult()
                                        .setName("nested step")
                                        .setAttachments(List.of(nestedAttachment))
                        )
                );

        Allure.step("Store direct and nested attachment artifacts", () -> {
            writer.write(testResult);
            writer.write("root.txt", new ByteArrayInputStream("root".getBytes(StandardCharsets.UTF_8)));
            writer.write("nested.txt", new ByteArrayInputStream("nested".getBytes(StandardCharsets.UTF_8)));
        });

        Allure.step("Verify recursive attachment metadata and content lookup", () -> {
            assertThat(writer.getAttachmentsRecursively())
                    .containsExactly(rootAttachment, nestedAttachment);
            assertThat(writer.getAttachmentContentAsString(rootAttachment))
                    .isEqualTo("root");
            assertThat(writer.getAttachmentContent(nestedAttachment, StandardCharsets.UTF_8))
                    .isEqualTo("nested");
        });
    }
}
