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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.AllureResultsWriteException;
import io.qameta.allure.internal.Allure2ModelJackson;

/**
 * Serializes HTTP exchange attachments using the Jackson runtime bundled with {@code allure-java-commons}.
 */
public final class HttpExchangeSerializer {

    private static final ObjectMapper MAPPER = Allure2ModelJackson.createMapper();

    private HttpExchangeSerializer() {
        throw new IllegalStateException("Do not instance HttpExchangeSerializer");
    }

    /**
     * Serializes the exchange to UTF-8 JSON bytes.
     *
     * <p>The serializer does not apply redaction or truncation. Build exchanges with
     * {@link HttpExchange#builder()} to apply capture policy before serialization.</p>
     *
     * @param exchange the exchange to serialize
     * @return the serialized exchange
     */
    public static byte[] toJsonBytes(final HttpExchange exchange) {
        try {
            return MAPPER.writeValueAsBytes(exchange);
        } catch (JsonProcessingException e) {
            throw new AllureResultsWriteException("Could not serialize HTTP exchange attachment", e);
        }
    }
}
