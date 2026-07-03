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
package io.qameta.allure;

/**
 * Additional options for an attachment.
 *
 * <p>When file extension is not set, it will be detected from the attachment content type when possible.
 * Use an empty string as file extension to force no extension.</p>
 */
public final class AttachmentOptions {

    private String fileExtension;

    /**
     * Creates empty attachment options. The file extension will be detected from the attachment content type when
     * possible.
     *
     * @return empty attachment options
     */
    public static AttachmentOptions empty() {
        return new AttachmentOptions();
    }

    /**
     * Creates attachment options with the file extension.
     *
     * @param fileExtension the attachment file extension
     * @return attachment options
     */
    public static AttachmentOptions withFileExtension(final String fileExtension) {
        return new AttachmentOptions()
                .setFileExtension(fileExtension);
    }

    /**
     * Gets attachment file extension.
     *
     * @return attachment file extension
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Sets attachment file extension. Use {@code null} to detect the extension from the attachment content type when
     * possible, or an empty string to force no extension.
     *
     * @param fileExtension the attachment file extension
     * @return this instance for method chaining
     */
    public AttachmentOptions setFileExtension(final String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

}
