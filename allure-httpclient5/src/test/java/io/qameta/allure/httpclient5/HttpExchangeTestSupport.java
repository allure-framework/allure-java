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
package io.qameta.allure.httpclient5;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

final class HttpExchangeTestSupport {

    private HttpExchangeTestSupport() {
        throw new IllegalStateException("do not instantiate");
    }

    static AllureResults executeWithAllure(final ThrowingRunnable runnable) {
        return runWithinTestContext(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }

    static Attachment httpExchangeAttachment(final AllureResults results) {
        final List<Attachment> attachments = attachments(results);

        assertThat(attachments).hasSize(1);
        return attachments.get(0);
    }

    private static List<Attachment> attachments(final AllureResults results) {
        return results.getTestResults().stream()
                .flatMap(HttpExchangeTestSupport::attachments)
                .toList();
    }

    private static Stream<Attachment> attachments(final TestResult result) {
        return Stream.concat(
                result.getAttachments().stream(),
                result.getSteps().stream().flatMap(HttpExchangeTestSupport::attachments)
        );
    }

    private static Stream<Attachment> attachments(final StepResult step) {
        return Stream.concat(
                step.getAttachments().stream(),
                step.getSteps().stream().flatMap(HttpExchangeTestSupport::attachments)
        );
    }

    static String attachmentContent(final AllureResults results, final Attachment attachment) {
        return new String(results.getAttachments().get(attachment.getSource()), StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
