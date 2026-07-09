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

import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utility methods for converting Java method arguments into Allure parameters.
 *
 * <p>Aspect and framework integrations use this class to apply {@link io.qameta.allure.Param} metadata, excluded flags, modes, and display names consistently.</p>
 */
public final class ParameterUtils {

    private ParameterUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Creates and returns the parameters.
     *
     * @param method the framework or Java method to inspect
     * @param args the args
     * @return the parameters
     */
    public static List<Parameter> createParameters(final Method method,
                                                   final Object... args) {
        final java.lang.reflect.Parameter[] parameters = method.getParameters();
        return IntStream.range(0, parameters.length)
                .mapToObj(i -> {
                    final java.lang.reflect.Parameter parameter = parameters[i];
                    final Object value = args[i];
                    return createParameter(parameter, value);
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates and returns the parameter using the Java reflection parameter name as a fallback.
     *
     * @param parameter the Java parameter to inspect
     * @param value the value
     * @return the parameter
     */
    public static Parameter createParameter(final java.lang.reflect.Parameter parameter,
                                            final Object value) {
        return createParameter(parameter, value, parameter.getName());
    }

    /**
     * Creates and returns the parameter.
     *
     * @param parameter the parameter declaration site to inspect — a method or constructor parameter, or a field
     * @param value the value
     * @param defaultName the name to use when {@link Param} does not override it
     * @return the parameter
     */
    public static Parameter createParameter(final AnnotatedElement parameter,
                                            final Object value,
                                            final String defaultName) {
        Objects.requireNonNull(defaultName, "defaultName");
        final Param annotation = parameter.getAnnotation(Param.class);
        if (Objects.isNull(annotation)) {
            return ResultsUtils.createParameter(defaultName, value);
        }
        final String name = Stream.of(annotation.value(), annotation.name(), defaultName)
                .map(String::trim)
                .filter(s -> s.length() > 0)
                .findFirst()
                .orElse(defaultName);

        return ResultsUtils.createParameter(
                name,
                value,
                annotation.excluded(),
                annotation.mode()
        );
    }

}
