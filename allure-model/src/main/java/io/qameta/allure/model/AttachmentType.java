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

    public static final AttachmentType PNG = new AttachmentType("image/png", "png");
    public static final AttachmentType JPEG = new AttachmentType("image/jpeg", "jpg");
    public static final AttachmentType TEXT = new AttachmentType("text/plain", "txt");
    public static final AttachmentType HTML = new AttachmentType("text/html", "html");
    public static final AttachmentType ZIP = new AttachmentType("application/zip", "zip");
    public static final AttachmentType WEBM = new AttachmentType("video/webm", "webm");
    public static final AttachmentType OCTET_STREAM = new AttachmentType("application/octet-stream", "");

    private final String mediaType;
    private final String extension;

    private AttachmentType(final String mediaType, final String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getExtension() {
        return extension;
    }
}
