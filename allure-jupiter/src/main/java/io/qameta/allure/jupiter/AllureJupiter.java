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
package io.qameta.allure.jupiter;

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
import java.util.List;
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
 * Reports JUnit Jupiter fixture execution details to Allure.
 *
 * <p>Register this extension when Jupiter lifecycle methods should appear as Allure fixtures with start, stop, and failure metadata.</p>
 */
@SuppressWarnings("MultipleStringLiterals")
public class AllureJupiter implements InvocationInterceptor {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AllureJupiter.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptTestTemplateMethod(final Invocation<Void> invocation,
                                            final ReflectiveInvocationContext<Method> invocationContext,
                                            final ExtensionContext extensionContext)
            throws Throwable {
        if (!shouldHandle(extensionContext, "template", invocationContext.getExecutable())) {
            invocation.proceed();
            return;
        }
        sendParameterEvent(invocationContext, extensionContext);
        invocation.proceed();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void sendParameterEvent(final ReflectiveInvocationContext<Method> invocationContext,
                                    final ExtensionContext extensionContext) {
        final Parameter[] parameters = invocationContext.getExecutable().getParameters();
        final List<Object> arguments = invocationContext.getArguments();
        int argumentIndex = 0;

        for (final Parameter parameter : parameters) {
            final Class<?> parameterType = parameter.getType();

            // Skip JUnit injectables AND synthetic parameters
            if (parameterType.getCanonicalName().startsWith("org.junit.jupiter.api")
                    || parameter.isSynthetic()
                    || argumentIndex >= arguments.size()) {
                continue;
            }

            final Object value = arguments.get(argumentIndex++);
            final Map<String, String> map = new HashMap<>();
            map.put(ALLURE_PARAMETER, parameter.getName());
            map.put(ALLURE_PARAMETER_VALUE_KEY, ObjectUtils.toString(value));

            Stream.of(parameter.getAnnotationsByType(Param.class))
                    .findFirst()
                    .ifPresent(param -> {
                        Stream.of(param.value(), param.name())
                                .map(String::trim)
                                .filter(name -> !name.isEmpty())
                                .findFirst()
                                .ifPresent(name -> map.put(ALLURE_PARAMETER, name));

                        map.put(ALLURE_PARAMETER_MODE_KEY, param.mode().name());
                        map.put(ALLURE_PARAMETER_EXCLUDED_KEY, Boolean.toString(param.excluded()));
                    });

            extensionContext.publishReportEntry(wrap(map));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptBeforeAllMethod(
                                         final Invocation<Void> invocation,
                                         final ReflectiveInvocationContext<Method> invocationContext,
                                         final ExtensionContext extensionContext)
            throws Throwable {
        processFixture(PREPARE, invocation, invocationContext, extensionContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptAfterAllMethod(
                                        final Invocation<Void> invocation,
                                        final ReflectiveInvocationContext<Method> invocationContext,
                                        final ExtensionContext extensionContext)
            throws Throwable {
        processFixture(TEAR_DOWN, invocation, invocationContext, extensionContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptBeforeEachMethod(
                                          final Invocation<Void> invocation,
                                          final ReflectiveInvocationContext<Method> invocationContext,
                                          final ExtensionContext extensionContext)
            throws Throwable {
        processFixture(PREPARE, invocation, invocationContext, extensionContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptAfterEachMethod(
                                         final Invocation<Void> invocation,
                                         final ReflectiveInvocationContext<Method> invocationContext,
                                         final ExtensionContext extensionContext)
            throws Throwable {
        processFixture(TEAR_DOWN, invocation, invocationContext, extensionContext);
    }

    /**
     * Handles the process fixture callback.
     *
     * @param type the event or label type
     * @param invocation the invocation
     * @param invocationContext the invocation context
     * @param extensionContext the extension context
     * @throws Throwable if the underlying framework operation fails
     */
    protected void processFixture(final String type,
                                  final Invocation<Void> invocation,
                                  final ReflectiveInvocationContext<Method> invocationContext,
                                  final ExtensionContext extensionContext)
            throws Throwable {
        if (!shouldHandle(extensionContext, type, invocationContext.getExecutable())) {
            invocation.proceed();
            return;
        }
        final String uuid = UUID.randomUUID().toString();
        try {
            extensionContext.publishReportEntry(
                    wrap(
                            buildStartEvent(
                                    type,
                                    uuid,
                                    invocationContext.getExecutable()
                            )
                    )
            );
            invocation.proceed();
            extensionContext.publishReportEntry(
                    wrap(
                            buildStopEvent(
                                    type,
                                    uuid
                            )
                    )
            );
        } catch (Throwable throwable) {
            extensionContext.publishReportEntry(
                    wrap(
                            buildFailureEvent(
                                    type,
                                    uuid,
                                    throwable
                            )
                    )
            );
            throw throwable;
        }
    }

    /**
     * Builds and returns the start event.
     *
     * @param type the event or label type
     * @param uuid the Allure UUID of the model object
     * @param method the framework or Java method to inspect
     * @return the start event
     */
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

    /**
     * Builds and returns the stop event.
     *
     * @param type the event or label type
     * @param uuid the Allure UUID of the model object
     * @return the stop event
     */
    public Map<String, String> buildStopEvent(final String type,
                                              final String uuid) {
        final Map<String, String> map = new HashMap<>();
        map.put(ALLURE_FIXTURE, type);
        map.put("event", EVENT_STOP);
        map.put("uuid", uuid);
        return map;
    }

    /**
     * Builds and returns the failure event.
     *
     * @param type the event or label type
     * @param uuid the Allure UUID of the model object
     * @param throwable the throwable
     * @return the failure event
     */
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
        maybeDetails.map(StatusDetails::getActual).ifPresent(actual -> map.put("actual", actual));
        maybeDetails.map(StatusDetails::getExpected).ifPresent(expected -> map.put("expected", expected));
        return map;
    }

    /**
     * Returns the wrap.
     *
     * @param data the data map to wrap or process
     * @return the wrap
     */
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

    private boolean shouldHandle(final ExtensionContext extensionContext,
                                 final String eventType,
                                 final Method method) {
        final Object marker = new Object();
        final String key = String.join(
                ":",
                extensionContext.getUniqueId(),
                eventType,
                method.toGenericString()
        );
        final Object storedMarker = extensionContext.getRoot()
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(key, ignored -> marker);
        return marker.equals(storedMarker);
    }
}
