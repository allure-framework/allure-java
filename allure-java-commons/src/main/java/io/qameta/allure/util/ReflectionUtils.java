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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Common reflection helpers.
 */
final class ReflectionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class);
    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final String METHOD_READ_ERROR = "Unable to read value via method {}";
    private static final String FIELD_READ_ERROR = "Unable to read value via field {}";

    private ReflectionUtils() {
        throw new IllegalStateException("Do not instance");
    }

    static Object getValue(final Object target, final String propertyName) {
        if (target == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        final Optional<Method> method = findGetter(target.getClass(), propertyName);
        if (method.isPresent()) {
            try {
                return invokeMethod(target, method.get());
            } catch (ReflectiveOperationException | SecurityException e) {
                LOGGER.trace(METHOD_READ_ERROR, method.get().getName(), e);
                return null;
            }
        }
        try {
            return getFieldValue(target, propertyName);
        } catch (ReflectiveOperationException | SecurityException e) {
            LOGGER.trace(FIELD_READ_ERROR, propertyName, e);
            return null;
        }
    }

    static Boolean getBooleanValue(final Object target, final String propertyName) {
        if (target == null || propertyName == null || propertyName.isEmpty()) {
            return absentBooleanValue();
        }
        final Optional<Method> method = findPrimitiveBooleanGetter(target.getClass(), propertyName);
        if (method.isPresent()) {
            return invokeBooleanMethod(target, method.get());
        }
        return toBooleanValue(getValue(target, propertyName), propertyName);
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    static Object getFieldValue(final Object target, final String fieldName) throws ReflectiveOperationException {
        final Class<?> type = target.getClass();
        try {
            final Field field = type.getField(fieldName);
            return getFieldValue(target, field);
        } catch (NoSuchFieldException e) {
            Class<?> currentType = type;
            while (currentType != null) {
                try {
                    final Field declaredField = currentType.getDeclaredField(fieldName);
                    return getFieldValue(target, declaredField);
                } catch (NoSuchFieldException ignore) {
                    // Ignore
                }
                currentType = currentType.getSuperclass();
            }
            throw e;
        }
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static Object getFieldValue(final Object target, final Field field) throws IllegalAccessException {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            field.setAccessible(true);
            return field.get(target);
        }
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static Object invokeMethod(final Object target, final Method method) throws ReflectiveOperationException {
        try {
            return method.invoke(target);
        } catch (IllegalAccessException e) {
            method.setAccessible(true);
            return method.invoke(target);
        }
    }

    private static Optional<Method> findGetter(final Class<?> type, final String propertyName) {
        final String capitalizedName = capitalize(propertyName);
        final String[] methodNames = {GET_PREFIX + capitalizedName, propertyName};
        Class<?> currentType = type;
        while (currentType != null) {
            for (String methodName : methodNames) {
                try {
                    return Optional.of(currentType.getDeclaredMethod(methodName));
                } catch (NoSuchMethodException ignored) {
                    // Ignore
                } catch (SecurityException e) {
                    LOGGER.trace(METHOD_READ_ERROR, methodName, e);
                    return Optional.empty();
                }
            }
            currentType = currentType.getSuperclass();
        }
        return Optional.empty();
    }

    private static Optional<Method> findPrimitiveBooleanGetter(final Class<?> type, final String propertyName) {
        final String methodName = IS_PREFIX + capitalize(propertyName);
        Class<?> currentType = type;
        while (currentType != null) {
            try {
                final Method method = currentType.getDeclaredMethod(methodName);
                return method.getReturnType() == boolean.class ? Optional.of(method) : Optional.empty();
            } catch (NoSuchMethodException ignored) {
                // Ignore
            } catch (SecurityException e) {
                LOGGER.trace(METHOD_READ_ERROR, methodName, e);
                return Optional.empty();
            }
            currentType = currentType.getSuperclass();
        }
        return Optional.empty();
    }

    private static String capitalize(final String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static Boolean invokeBooleanMethod(final Object target, final Method method) {
        try {
            return Boolean.class.cast(invokeMethod(target, method));
        } catch (ReflectiveOperationException | SecurityException e) {
            LOGGER.trace(METHOD_READ_ERROR, method.getName(), e);
            return absentBooleanValue();
        }
    }

    private static Boolean toBooleanValue(final Object value, final String propertyName) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            LOGGER.trace(
                    "Unable to read boolean value {}: actual value type is {}",
                    propertyName,
                    value.getClass().getName()
            );
        }
        return absentBooleanValue();
    }

    private static Boolean absentBooleanValue() {
        return Optional.<Boolean>empty().orElse(null);
    }
}
