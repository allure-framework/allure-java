/*
 *  Copyright 2016-2024 Qameta Software Inc
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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author charlie (Dmitry Baev).
 */
public final class NamingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamingUtils.class);

    private static final Collector<CharSequence, ?, String> JOINER = Collectors.joining(", ", "[", "]");

    private NamingUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static String processNameTemplate(final String template, final Map<String, Object> params) {
        final Matcher matcher = Pattern.compile("\\{([^}]*)}").matcher(template);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            final String pattern = matcher.group(1);
            final String replacement = processPattern(pattern, params).orElseGet(matcher::group);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Optional<String> processPattern(final String pattern, final Map<String, Object> params) {
        if (pattern.isEmpty()) {
            LOGGER.error("Could not process empty pattern");
            return Optional.empty();
        }
        final String[] parts = pattern.split("\\.");
        final String parameterName = parts[0];
        if (!params.containsKey(parameterName)) {
            LOGGER.error("Could not find parameter " + parameterName);
            return Optional.empty();
        }
        final Object param = params.get(parameterName);
        return Optional.ofNullable(extractProperties(param, parts, 1));
    }

    @SuppressWarnings("ReturnCount")
    private static String extractProperties(final Object object, final String[] parts, final int index) {
        if (Objects.isNull(object)) {
            return "null";
        }
        if (index < parts.length) {
            if (object instanceof Object[]) {
                return Stream.of((Object[]) object)
                        .map(child -> extractProperties(child, parts, index))
                        .collect(JOINER);
            }
            if (object instanceof Iterable) {
                final Spliterator<?> iterator = ((Iterable) object).spliterator();
                return StreamSupport.stream(iterator, false)
                        .map(child -> extractProperties(child, parts, index))
                        .collect(JOINER);
            }
            final Object child = extractChild(object, parts[index]);
            return extractProperties(child, parts, index + 1);
        }
        return ObjectUtils.toString(object);
    }

    private static Object extractChild(final Object object, final String part) {
        final Class<?> type = object == null ? Object.class : object.getClass();
        try {
            return extractField(object, part, type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to extract " + part + " value from " + type.getName(), e);
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static Object extractField(final Object object, final String part, final Class<?> type)
            throws ReflectiveOperationException {
        try {
            final Field field = type.getField(part);
            return fieldValue(object, field);
        } catch (NoSuchFieldException e) {
            Class<?> t = type;
            while (t != null) {
                try {
                    final Field declaredField = t.getDeclaredField(part);
                    return fieldValue(object, declaredField);
                } catch (NoSuchFieldException ignore) {
                    // Ignore
                }
                t = t.getSuperclass();
            }
            throw e;
        }
    }

    private static Object fieldValue(final Object object, final Field field) throws IllegalAccessException {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            field.setAccessible(true);
            return field.get(object);
        }
    }
}
