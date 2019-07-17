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

import io.qameta.allure.model.Parameter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public static Map<String, Object> getParametersMap(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final String[] parameterNames = methodSignature.getParameterNames();
        final Map<String, Object> params = new HashMap<>();
        params.put("method", methodSignature.getName());
        params.put("this", joinPoint.getThis());
        final Object[] args = joinPoint.getArgs();
        for (int i = 0; i < Math.max(parameterNames.length, args.length); i++) {
            params.put(parameterNames[i], args[i]);
            params.put(Integer.toString(i), args[i]);
        }
        return params;
    }

    public static List<Parameter> getParameters(final MethodSignature signature, final Object... args) {
        return IntStream
                .range(0, args.length)
                .mapToObj(index -> createParameter(signature.getParameterNames()[index], args[index]))
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
