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
package io.qameta.allure.attachment;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;

import java.nio.charset.StandardCharsets;

/**
 * Supports Allure attachment integration with Allure reporting.
 *
 * <p>Use this type through the module that owns it when translating framework execution, result metadata, or attachments into Allure report data.</p>
 */
public class DefaultAttachmentProcessor implements AttachmentProcessor<AttachmentData> {

    private final AllureLifecycle lifecycle;

    /**
     * Creates a default attachment processor with default configuration.
     */
    public DefaultAttachmentProcessor() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates a default attachment processor with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public DefaultAttachmentProcessor(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAttachment(final AttachmentData attachmentData,
                              final AttachmentRenderer<AttachmentData> renderer) {
        final AttachmentContent content = renderer.render(attachmentData);
        lifecycle.addAttachment(
                attachmentData.getName(),
                content.getContentType(),
                content.getFileExtension(),
                content.getContent().getBytes(StandardCharsets.UTF_8)
        );
    }
}
