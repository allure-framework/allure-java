/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.reader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @param <T> the enum's type
 * @author charlie (Dmitry Baev).
 * @deprecated in favor of {@link com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS}
 * and {@link com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL}
 */
@Deprecated
/* package-private */ abstract class AllureEnumDeserializer<T extends Enum<T>> extends StdDeserializer<T> {

    private final Class<T> type;

    protected AllureEnumDeserializer(final Class<T> vc) {
        super(vc);
        type = vc;
    }

    @Override
    public T deserialize(final JsonParser p,
                         final DeserializationContext ctxt) throws IOException {
        final String value = p.readValueAs(String.class);
        if (Objects.isNull(value)) {
            return null;
        }

        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return Stream.of(type.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
