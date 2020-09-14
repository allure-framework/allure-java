/*
 *  Copyright 2020 Qameta Software OÜ
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

import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.qameta.allure.junitplatform.AllureJunitPlatform.ALLURE_FIXTURE;
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
            extensionContext.publishReportEntry(buildStartEvent(
                    type,
                    uuid,
                    invocationContext.getExecutable()
            ));
            invocation.proceed();
            extensionContext.publishReportEntry(buildStopEvent(
                    type,
                    uuid
            ));
        } catch (Throwable throwable) {
            extensionContext.publishReportEntry(buildFailureEvent(
                    type,
                    uuid,
                    throwable
            ));
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
}
