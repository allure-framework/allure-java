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

/**
 * Supports Allure attachment integration with Allure reporting.
 *
 * <p>Use this type through the module that owns it when translating framework execution, result metadata, or attachments into Allure report data.</p>
 */
public class DefaultAttachmentContent implements AttachmentContent {

    private final String content;

    private final String contentType;

    private final String fileExtension;

    /**
     * Creates a default attachment content with the supplied values.
     *
     * @param content the attachment content
     * @param contentType the attachment content type
     * @param fileExtension the attachment file extension
     */
    public DefaultAttachmentContent(final String content,
                                    final String contentType,
                                    final String fileExtension) {
        this.content = content;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContent() {
        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileExtension() {
        return fileExtension;
    }
}
