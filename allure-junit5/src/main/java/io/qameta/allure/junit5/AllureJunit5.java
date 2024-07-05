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
package io.qameta.allure.junit5;

import io.qameta.allure.Param;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.util.ObjectUtils;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_FIXTURE;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER_EXCLUDED_KEY;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER_MODE_KEY;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_PARAMETER_VALUE_KEY;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_REPORT_ENTRY_BLANK_PREFIX;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.EVENT_FAILURE;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.EVENT_START;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.EVENT_STOP;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.PREPARE;
import static io.qameta.allure.junitplatform.AllureJunitPlatform.TEAR_DOWN;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("MultipleStringLiterals")
public class AllureJunit5 implements InvocationInterceptor {

    @Override
    public void interceptTestTemplateMethod(final Invocation<Void> invocation,
                                            final ReflectiveInvocationContext<Method> invocationContext,
                                            final ExtensionContext extensionContext) throws Throwable {
        sendParameterEvent(invocationContext, extensionContext);
        invocation.proceed();
    }

    private void sendParameterEvent(final ReflectiveInvocationContext<Method> invocationContext,
                                    final ExtensionContext extensionContext) {
        final Parameter[] parameters = invocationContext.getExecutable().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];

            final Class<?> parameterType = parameter.getType();
            // Skip default jupiter injectables as TestInfo, TestReporter and TempDirectory
            if (parameterType.getCanonicalName().startsWith("org.junit.jupiter.api")) {
                continue;
            }
            final Object value = invocationContext.getArguments().get(i);
            final Map<String, String> map = new HashMap<>();
            map.put(ALLURE_PARAMETER, parameter.getName());
            map.put(ALLURE_PARAMETER_VALUE_KEY, ObjectUtils.toString(value));

            Stream.of(parameter.getAnnotationsByType(Param.class))
                    .findFirst()
                    .ifPresent(param -> {
                        Stream.of(param.value(), param.name())
                                .map(String::trim)
                                .filter(name -> name.length() > 0)
                                .findFirst()
                                .ifPresent(name -> map.put(ALLURE_PARAMETER, name));

                        map.put(ALLURE_PARAMETER_MODE_KEY, param.mode().name());
                        map.put(ALLURE_PARAMETER_EXCLUDED_KEY, Boolean.toString(param.excluded()));
                    });

            extensionContext.publishReportEntry(wrap(map));
        }
    }

    @Override
    public void interceptBeforeAllMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {
        processFixture(PREPARE, invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptAfterAllMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {
        processFixture(TEAR_DOWN, invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptBeforeEachMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {
        processFixture(PREPARE, invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptAfterEachMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {
        processFixture(TEAR_DOWN, invocation, invocationContext, extensionContext);
    }

    protected void processFixture(final String type,
                                  final Invocation<Void> invocation,
                                  final ReflectiveInvocationContext<Method> invocationContext,
                                  final ExtensionContext extensionContext) throws Throwable {
        final String uuid = UUID.randomUUID().toString();
        try {
            extensionContext.publishReportEntry(wrap(buildStartEvent(
                    type,
                    uuid,
                    invocationContext.getExecutable()
            )));
            invocation.proceed();
            extensionContext.publishReportEntry(wrap(buildStopEvent(
                    type,
                    uuid
            )));
        } catch (Throwable throwable) {
            extensionContext.publishReportEntry(wrap(buildFailureEvent(
                    type,
                    uuid,
                    throwable
            )));
            throw throwable;
        }
    }

    public Map<String, String> buildStartEvent(final String type,
                                               final String uuid,
                                               final Method method) {
        final Map<String, String> map = new HashMap<>();
        map.put(ALLURE_FIXTURE, type);
        map.put("event", EVENT_START);
        map.put("uuid", uuid);
        map.put("name", method.getName());
        return map;
    }

    public Map<String, String> buildStopEvent(final String type,
                                              final String uuid) {
        final Map<String, String> map = new HashMap<>();
        map.put(ALLURE_FIXTURE, type);
        map.put("event", EVENT_STOP);
        map.put("uuid", uuid);
        return map;
    }

    public Map<String, String> buildFailureEvent(final String type,
                                                 final String uuid,
                                                 final Throwable throwable) {
        final Map<String, String> map = new HashMap<>();
        map.put(ALLURE_FIXTURE, type);
        map.put("event", EVENT_FAILURE);
        map.put("uuid", uuid);

        final Optional<Status> maybeStatus = ResultsUtils.getStatus(throwable);
        maybeStatus.map(Status::value).ifPresent(status -> map.put("status", status));

        final Optional<StatusDetails> maybeDetails = ResultsUtils.getStatusDetails(throwable);
        maybeDetails.map(StatusDetails::getMessage).ifPresent(message -> map.put("message", message));
        maybeDetails.map(StatusDetails::getTrace).ifPresent(trace -> map.put("trace", trace));
        return map;
    }

    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    public Map<String, String> wrap(final Map<String, String> data) {
        final Map<String, String> res = new HashMap<>();
        data.forEach((key, value) -> {
                    if (Objects.isNull(value) || value.trim().isEmpty()) {
                        res.put(key, ALLURE_REPORT_ENTRY_BLANK_PREFIX + value);
                    } else {
                        res.put(key, value);
                    }
                }
        );
        return res;
    }
}
