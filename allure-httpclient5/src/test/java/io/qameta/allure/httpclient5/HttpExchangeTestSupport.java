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
import io.qameta.allure.test.AllureResults;

import java.util.List;

import static io.qameta.allure.Allure.step;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import io.qameta.allure.test.IsolatedLifecycle;

@IsolatedLifecycle
final class HttpExchangeTestSupport {

    private HttpExchangeTestSupport() {
        throw new IllegalStateException("do not instantiate");
    }

    static AllureResults executeWithAllure(final ThrowingRunnable runnable) {
        return step("Execute Apache HttpClient 5 request and collect Allure results", () -> runWithinTestContext(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }));
    }

    static Attachment httpExchangeAttachment(final AllureResults results) {
        final List<Attachment> attachments = results.getAttachmentsRecursively();

        assertThat(attachments).hasSize(1);
        return attachments.get(0);
    }

    static String attachmentContent(final AllureResults results, final Attachment attachment) {
        return results.getAttachmentContentAsString(attachment);
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
