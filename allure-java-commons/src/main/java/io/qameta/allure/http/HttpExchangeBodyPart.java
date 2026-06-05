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
package io.qameta.allure.http;

import java.util.List;

/**
 * Multipart body part captured in an exchange attachment.
 *
 * @param name the part name
 * @param fileName the part file name
 * @param headers the part headers
 * @param contentType the part content type
 * @param encoding the body encoding, either {@code utf8} or {@code base64}
 * @param value the body value
 * @param size the original body size
 * @param truncated true when the body was truncated
 */
public record HttpExchangeBodyPart(String name, String fileName, List<HttpExchangeNameValue> headers,
        String contentType, String encoding, String value, Long size,
        Boolean truncated) {

    public HttpExchangeBodyPart {
        headers = copy(headers);
    }

    private static List<HttpExchangeNameValue> copy(final List<HttpExchangeNameValue> values) {
        return values == null ? null : List.copyOf(values);
    }
}
