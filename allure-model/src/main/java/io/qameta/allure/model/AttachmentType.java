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

/**
 * Common attachment media types and file extensions.
 */
public final class AttachmentType {

    /**
     * Predefined attachment type for png content.
     */
    public static final AttachmentType PNG = new AttachmentType("image/png", "png");

    /**
     * Predefined attachment type for jpeg content.
     */
    public static final AttachmentType JPEG = new AttachmentType("image/jpeg", "jpg");

    /**
     * Predefined attachment type for text content.
     */
    public static final AttachmentType TEXT = new AttachmentType("text/plain", "txt");

    /**
     * Predefined attachment type for html content.
     */
    public static final AttachmentType HTML = new AttachmentType("text/html", "html");

    /**
     * Predefined attachment type for zip content.
     */
    public static final AttachmentType ZIP = new AttachmentType("application/zip", "zip");

    /**
     * Predefined attachment type for webm content.
     */
    public static final AttachmentType WEBM = new AttachmentType("video/webm", "webm");

    /**
     * Predefined attachment type for octet stream content.
     */
    public static final AttachmentType OCTET_STREAM = new AttachmentType("application/octet-stream", "");

    private final String mediaType;
    private final String extension;

    private AttachmentType(final String mediaType, final String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    /**
     * Returns the media type.
     *
     * @return the media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Returns the extension.
     *
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }
}
