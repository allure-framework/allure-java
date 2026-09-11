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
package io.qameta.allure.internal.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.qameta.allure.internal.Allure2ModelJackson;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * Internal serialization shared by Allure modules without exposing the bundled Jackson API.
 */
public final class JsonSupport {

    private static final ObjectWriter JSON = new ObjectMapper().writer();
    private static final ObjectWriter MODEL = Allure2ModelJackson.createMapper()
            .setSerializationInclusion(NON_DEFAULT)
            .writerWithDefaultPrettyPrinter();

    private JsonSupport() {
        throw new IllegalStateException("Do not instance JsonSupport");
    }

    /**
     * Serializes JSON values such as maps, lists, strings, numbers, booleans, and null.
     *
     * @param value the JSON-compatible value
     * @return the serialized JSON
     * @throws IOException if serialization fails
     */
    public static String writeJson(final Object value) throws IOException {
        return JSON.writeValueAsString(value);
    }

    /**
     * Serializes Allure model evidence using the test utilities' compact field inclusion and enum values.
     *
     * @param value the Allure model value
     * @return formatted JSON without default-valued fields
     * @throws IOException if serialization fails
     */
    public static String writeModel(final Object value) throws IOException {
        return MODEL.writeValueAsString(value);
    }
}
