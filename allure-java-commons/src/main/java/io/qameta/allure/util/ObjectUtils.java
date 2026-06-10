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
package io.qameta.allure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility methods for safe object string conversion.
 *
 * <p>Use these helpers from integrations and attachment builders when values may be arrays, maps, null, or objects whose {@code toString()} method can fail.</p>
 */
public final class ObjectUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectUtils.class);

    /**
     * Do not instance.
     */
    private ObjectUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Converts and returns the string.
     * Pretty prints arrays and guards against failing {@code toString()} implementations.
     *
     * @param object the object to convert
     * @return a string representation of this object
     */
    @SuppressWarnings(
        {
                "CyclomaticComplexity",
                "ReturnCount",
        }
    )
    public static String toString(final Object object) {
        try {
            if (Objects.nonNull(object) && object.getClass().isArray()) {
                if (object instanceof Object[]) {
                    return Arrays.stream((Object[]) object)
                            .map(ObjectUtils::toString)
                            .collect(Collectors.joining(", ", "[", "]"));
                } else if (object instanceof long[]) {
                    return Arrays.toString((long[]) object);
                } else if (object instanceof short[]) {
                    return Arrays.toString((short[]) object);
                } else if (object instanceof int[]) {
                    return Arrays.toString((int[]) object);
                } else if (object instanceof char[]) {
                    return Arrays.toString((char[]) object);
                } else if (object instanceof double[]) {
                    return Arrays.toString((double[]) object);
                } else if (object instanceof float[]) {
                    return Arrays.toString((float[]) object);
                } else if (object instanceof boolean[]) {
                    return Arrays.toString((boolean[]) object);
                } else if (object instanceof byte[]) {
                    return "<BINARY>";
                }
            }
            return Objects.toString(object);
        } catch (Exception e) {
            LOGGER.error("Could not convert object to string", e);
            return "<NPE>";
        }
    }

    /**
     * Returns the map to string.
     *
     * @param map the map
     * @return the map to string
     */
    public static String mapToString(final Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return map.keySet()
                .stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(",", "{", "}"));
    }
}
