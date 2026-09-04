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

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.util.ParameterUtils;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Reports JUnit Jupiter fixture and parameter execution details to Allure.
 *
 * <p>Register this extension together with {@link AllureJunitPlatform} when Jupiter lifecycle methods should appear
 * as Allure fixtures and parameterized-test arguments as Allure parameters. The extension writes through the Allure
 * lifecycle directly, addressing the scopes and tests started by the listener with keys recomputed from the JUnit
 * Platform unique ids — see {@link AllureJunitPlatform#scopeKey(String)} and
 * {@link AllureJunitPlatform#testKey(String)}.</p>
 */
public class AllureJupiter implements InvocationInterceptor, BeforeEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AllureJupiter.class);

    private static final String TEST = "test";
    private static final String TEMPLATE = "template";
    private static final String PARAMETERS = "parameters";
    private static final String PREPARE = "prepare";
    private static final String TEAR_DOWN = "tear_down";
    private static final String CAPTURED_PARAMETERS = "captured_parameters";

    private static final boolean CURRENT_PARAMETER_INFO_SUPPORTED = isClassAvailableOnClasspath(
            "org.junit.jupiter.params.ParameterInfo"
    );
    private static final boolean LEGACY_PARAMETER_INFO_SUPPORTED = isClassAvailableOnClasspath(
            "org.junit.jupiter.params.support.ParameterInfo"
    );

    /**
     * Returns the lifecycle. Resolved at call time, so the extension follows process-wide lifecycle swaps.
     *
     * @return the Allure lifecycle used by this integration
     */
    protected AllureLifecycle getLifecycle() {
        return Allure.getLifecycle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        final Method testMethod = extensionContext.getRequiredTestMethod();
        if (!shouldHandle(extensionContext, PARAMETERS, testMethod)) {
            return;
        }
        final List<Parameter> parameters = getParameters(extensionContext);
        if (parameters.isEmpty()) {
            return;
        }
        extensionContext.getStore(NAMESPACE)
                .put(CAPTURED_PARAMETERS, new ParameterCapture(parameters));
        addParameters(extensionContext, parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptTestMethod(final Invocation<Void> invocation,
                                    final ReflectiveInvocationContext<Method> invocationContext,
                                    final ExtensionContext extensionContext)
            throws Throwable {
        if (!shouldHandle(extensionContext, TEST, invocationContext.getExecutable())) {
            invocation.proceed();
            return;
        }
        replaceCapturedParameters(extensionContext, getClassParameters(invocationContext, extensionContext));
        invocation.proceed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interceptTestTemplateMethod(final Invocation<Void> invocation,
                                            final ReflectiveInvocationContext<Method> invocationContext,
                                            final ExtensionContext extensionContext)
            throws Throwable {
        if (!shouldHandle(extensionContext, TEMPLATE, invocationContext.getExecutable())) {
            invocation.proceed();
            return;
        }
        final List<Parameter> testParameters = new ArrayList<>(getClassParameters(invocationContext, extensionContext));
        testParameters.addAll(getArgumentParameters(invocationContext));
        replaceCapturedParameters(extensionContext, testParameters);
        invocation.proceed();
    }

    private void addParameters(final ExtensionContext extensionContext,
                               final List<Parameter> testParameters) {
        if (testParameters.isEmpty()) {
            return;
        }
        getLifecycle().updateTest(
                AllureJunitPlatform.testKey(extensionContext.getUniqueId()),
                testResult -> testResult.getParameters().addAll(testParameters)
        );
    }

    private void replaceCapturedParameters(final ExtensionContext extensionContext,
                                           final List<Parameter> testParameters) {
        final ParameterCapture capture = extensionContext.getStore(NAMESPACE)
                .remove(CAPTURED_PARAMETERS, ParameterCapture.class);
        if (capture == null) {
            addParameters(extensionContext, testParameters);
            return;
        }

        // ParameterInfo exposes source arguments before setup. Once the invocation exists, prefer its converted
        // values and remove only the objects captured here so parameters added by user code remain untouched.
        final Set<Parameter> capturedParameters = Collections.newSetFromMap(new IdentityHashMap<>());
        capturedParameters.addAll(capture.parameters());
        getLifecycle().updateTest(
                AllureJunitPlatform.testKey(extensionContext.getUniqueId()),
                testResult -> {
                    testResult.getParameters().removeIf(capturedParameters::contains);
                    testResult.getParameters().addAll(testParameters);
                }
        );
    }

    private List<Parameter> getParameters(final ExtensionContext extensionContext) {
        if (CURRENT_PARAMETER_INFO_SUPPORTED) {
            return AllureJupiterParameterInfoSupport.getParameters(extensionContext);
        }
        if (LEGACY_PARAMETER_INFO_SUPPORTED) {
            return AllureJupiterLegacyParameterInfoSupport.getParameters(extensionContext);
        }
        return Collections.emptyList();
    }

    private List<Parameter> getClassParameters(final ReflectiveInvocationContext<Method> invocationContext,
                                               final ExtensionContext extensionContext) {
        if (CURRENT_PARAMETER_INFO_SUPPORTED) {
            return AllureJupiterParameterInfoSupport.getClassParameters(
                    extensionContext,
                    invocationContext.getExecutable()
            );
        }
        if (LEGACY_PARAMETER_INFO_SUPPORTED) {
            return AllureJupiterLegacyParameterInfoSupport.getClassParameters(
                    extensionContext,
                    invocationContext.getExecutable()
            );
        }
        return Collections.emptyList();
    }

    private List<Parameter> getArgumentParameters(final ReflectiveInvocationContext<Method> invocationContext) {
        final java.lang.reflect.Parameter[] parameters = invocationContext.getExecutable().getParameters();
        final List<Object> arguments = invocationContext.getArguments();
        final List<Parameter> testParameters = new ArrayList<>();
        int argumentIndex = 0;

        for (final java.lang.reflect.Parameter parameter : parameters) {
            final Class<?> parameterType = parameter.getType();

            // Skip JUnit injectables AND synthetic parameters
            if (parameterType.getCanonicalName().startsWith("org.junit.jupiter.api")
                    || parameter.isSynthetic()
                    || argumentIndex >= arguments.size()) {
                continue;
            }

            final Object value = arguments.get(argumentIndex++);
            testParameters.add(ParameterUtils.createParameter(parameter, value));
        }
        return testParameters;
    }

    private static boolean isClassAvailableOnClasspath(final String clazz) {
        try {
            Class.forName(clazz, false, AllureJupiter.class.getClassLoader());
            return true;
        } catch (Exception ignored) {
            return false;
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
     * Runs a Jupiter lifecycle method as an Allure fixture of the scope registered for the current extension
     * context. The fixture is stopped in every case; on failure its status and details are taken from the thrown
     * exception, which is then rethrown.
     *
     * @param type the fixture type, {@code prepare} for before fixtures, {@code tear_down} for after fixtures
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
        final AllureLifecycle lifecycle = getLifecycle();
        final AllureExternalKey scopeKey = AllureJunitPlatform.scopeKey(extensionContext.getUniqueId());
        final AllureExternalKey fixtureKey = AllureExternalKey.random(AllureJupiter.class);
        final FixtureResult fixtureResult = new FixtureResult()
                .setName(invocationContext.getExecutable().getName());
        if (PREPARE.equals(type)) {
            lifecycle.startBeforeFixture(scopeKey, fixtureKey, fixtureResult);
        } else {
            lifecycle.startAfterFixture(scopeKey, fixtureKey, fixtureResult);
        }
        try {
            invocation.proceed();
            lifecycle.updateFixture(fixtureKey, fixture -> fixture.setStatus(Status.PASSED));
        } catch (Throwable throwable) {
            lifecycle.updateFixture(fixtureKey, fixture -> {
                ResultsUtils.getStatus(throwable).ifPresent(fixture::setStatus);
                ResultsUtils.getStatusDetails(throwable).ifPresent(fixture::setStatusDetails);
            });
            throw throwable;
        } finally {
            // also restores the thread binding saved at fixture start
            lifecycle.stopFixture(fixtureKey);
        }
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

    private record ParameterCapture(List<Parameter> parameters) {
    }
}
