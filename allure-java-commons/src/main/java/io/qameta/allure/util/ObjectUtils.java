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
package io.qameta.allure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
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
     * Returns string representation of given object. Pretty prints arrays.
     *
     * @param object the given object.
     * @return the string representation of given object.
     */
    @SuppressWarnings({
            "CyclomaticComplexity",
            "ReturnCount",
            "PMD.NcssCount",
            "PMD.CyclomaticComplexity"
    })
    public static String toString(final Object object) {
        try {
            if (Objects.nonNull(object) && object.getClass().isArray()) {
                if (object instanceof Object[]) {
                    return Arrays.toString((Object[]) object);
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
}
