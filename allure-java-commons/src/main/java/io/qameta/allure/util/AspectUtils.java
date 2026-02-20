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
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.util.NamingUtils.processNameTemplate;
import static io.qameta.allure.util.ResultsUtils.createParameter;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AspectUtils {

    private AspectUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static String getName(final String nameTemplate, final JoinPoint joinPoint) {
        return Optional.of(nameTemplate)
                .filter(v -> !v.isEmpty())
                .map(value -> processNameTemplate(value, getParametersMap(joinPoint)))
                .orElseGet(joinPoint.getSignature()::getName);
    }

    /**
     * @deprecated use {@link AspectUtils#getParametersMap(JoinPoint)} instead.
     */
    @Deprecated
    public static Map<String, Object> getParametersMap(final MethodSignature signature, final Object... args) {
        final String[] parameterNames = signature.getParameterNames();
        final Map<String, Object> params = new HashMap<>();
        params.put("method", signature.getName());
        for (int i = 0; i < Math.max(parameterNames.length, args.length); i++) {
            params.put(parameterNames[i], args[i]);
            params.put(Integer.toString(i), args[i]);
        }
        return params;
    }

    public static Map<String, Object> getParametersMap(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Map<String, Object> params = getParametersMap(methodSignature, joinPoint.getArgs());
        Optional.ofNullable(joinPoint.getThis()).ifPresent(objThis -> params.put("this", objThis));
        return params;
    }

    public static List<Parameter> getParameters(final MethodSignature signature, final Object... args) {
        final java.lang.reflect.Parameter[] params = signature.getMethod().getParameters();
        return IntStream
                .range(0, args.length)
                .mapToObj(index -> {
                    final Parameter parameter = createParameter(signature.getParameterNames()[index], args[index]);
                    final java.lang.reflect.Parameter ref = params[index];
                    Stream.of(ref.getAnnotationsByType(Param.class))
                            .findFirst()
                            .ifPresent(param -> {
                                Stream.of(param.value(), param.name())
                                        .map(String::trim)
                                        .filter(name -> name.length() > 0)
                                        .findFirst()
                                        .ifPresent(parameter::setName);

                                parameter.setMode(param.mode());
                                parameter.setExcluded(param.excluded());
                            });
                    return parameter;
                })
                .collect(Collectors.toList());
    }

    /**
     * @deprecated use {@link ObjectUtils#toString(Object)} instead.
     */
    @Deprecated
    public static String objectToString(final Object object) {
        return ObjectUtils.toString(object);
    }
}
