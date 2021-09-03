/*
 *  Copyright 2021 Qameta Software OÜ
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
public final class ParameterUtils {

    private ParameterUtils() {
        throw new IllegalStateException("do not instance");
    }

    public static List<Parameter> createParameters(final Method method,
                                                   final Object... args) {
        final java.lang.reflect.Parameter[] parameters = method.getParameters();
        return IntStream.range(0, parameters.length)
                .mapToObj(i -> {
                    final java.lang.reflect.Parameter parameter = parameters[i];
                    final Object value = args[i];
                    final Param annotation = parameter.getAnnotation(Param.class);
                    if (Objects.isNull(annotation)) {
                        return ResultsUtils.createParameter(parameter.getName(), value);
                    }
                    final String name = Stream.of(annotation.value(), annotation.name(), parameter.getName())
                            .map(String::trim)
                            .filter(s -> s.length() > 0)
                            .findFirst()
                            .orElseGet(() -> "arg" + i);

                    return ResultsUtils.createParameter(
                            name,
                            value,
                            annotation.excluded(),
                            annotation.mode()
                    );
                })
                .collect(Collectors.toList());
    }

}
