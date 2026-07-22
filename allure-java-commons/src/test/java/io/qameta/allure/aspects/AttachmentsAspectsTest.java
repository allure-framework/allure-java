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
package io.qameta.allure.aspects;

import io.qameta.allure.Attachment;
import io.qameta.allure.AttachmentBytes;
import io.qameta.allure.Issue;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
@IsolatedLifecycle
class AttachmentsAspectsTest {

    @Issue("485")
    @Test
    void shouldUseBytesFromCustomAttachmentType() {
        final byte[] expected = new byte[]{0, 1, 2, -1};

        final AllureResults results = runWithinTestContext(() -> customAttachment(expected));

        assertThat(results.getAttachmentsRecursively())
                .singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.getName()).isEqualTo("Custom attachment");
                    assertThat(attachment.getType()).isEqualTo("application/octet-stream");
                    assertThat(results.getAttachmentContent(attachment)).containsExactly(expected);
                });
    }

    @Attachment(
            value = "Custom attachment",
            type = "application/octet-stream"
    )
    CustomAttachment customAttachment(final byte[] content) {
        return new CustomAttachment(content);
    }

    static final class CustomAttachment implements AttachmentBytes {

        private final byte[] content;

        CustomAttachment(final byte[] content) {
            this.content = content;
        }

        @Override
        public byte[] attachmentBytes() {
            return content;
        }

        @Override
        public String toString() {
            return "fallback content";
        }
    }
}
