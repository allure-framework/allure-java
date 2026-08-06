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
package io.qameta.allure.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalAttachmentTest {

    @Test
    void shouldSetAndCompareAttachmentDetailsAndTimestamp() {
        final GlobalAttachment attachment = globalAttachment("setup.log", 123L);
        final GlobalAttachment equalAttachment = globalAttachment("setup.log", 123L);

        assertThat(attachment.getName()).isEqualTo("setup log");
        assertThat(attachment.getSource()).isEqualTo("setup.log");
        assertThat(attachment.getType()).isEqualTo("text/plain");
        assertThat(attachment.getSize()).isEqualTo(42L);
        assertThat(attachment.getTimestamp()).isEqualTo(123L);
        assertThat(attachment)
                .isEqualTo(attachment)
                .isEqualTo(equalAttachment)
                .isNotEqualTo(globalAttachment("different.log", 123L))
                .isNotEqualTo(globalAttachment("setup.log", 456L))
                .isNotEqualTo(new Attachment().setSource("setup.log"))
                .isNotEqualTo(null);
        assertThat(attachment.hashCode()).isEqualTo(equalAttachment.hashCode());
    }

    private static GlobalAttachment globalAttachment(final String source, final long timestamp) {
        return new GlobalAttachment()
                .setName("setup log")
                .setSource(source)
                .setType("text/plain")
                .setSize(42L)
                .setTimestamp(timestamp);
    }
}
