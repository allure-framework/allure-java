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
package io.qameta.allure.assertj;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.api.AbstractAssert;

import java.util.function.Supplier;

/**
 * Captures user-side AssertJ factories and fluent calls, then delegates assertion-chain state
 * to {@link AssertJRecorder}.
 *
 */
@SuppressWarnings("all")
@Aspect
public class AllureAspectJ {

    private static InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    private static final ThreadLocal<AssertJRecorder> RECORDER = ThreadLocal.withInitial(AssertJRecorder::new);

    private static final ThreadLocal<Boolean> RECORDING_MUTED = ThreadLocal.withInitial(() -> false);

    @Pointcut(
        "("
                + "call(public static * org.assertj.core.api.Assertions*.assertThat*(..))"
                + " || call(public static * org.assertj.core.api.BDDAssertions*.then*(..))"
                + " || call(public * org.assertj.core.api.*SoftAssertionsProvider+.assertThat*(..))"
                + " || call(public * org.assertj.core.api.*SoftAssertionsProvider+.then*(..))"
                + ")"
    )

    /**
     * Handles the assert factory call callback.
     */
    public void assertFactoryCall() {
        //pointcut body, should be empty
    }

    @Pointcut(
        "("
                + "call(public * org.assertj.core.api.AbstractAssert+.*(..))"
                + " || call(public * org.assertj.core.api.Assert+.*(..))"
                + " || call(public * org.assertj.core.api.Descriptable+.*(..))"
                + ")"
                + " && target(assertion)"
    )

    /**
     * Handles the assert operation call callback.
     *
     * @param assertion the assertion
     */
    public void assertOperationCall(final AbstractAssert<?, ?> assertion) {
        //pointcut body, should be empty
    }

    /**
     * Handles the user code call callback.
     */
    @Pointcut("!within(org.assertj..*) && !within(io.qameta.allure.assertj.AllureAspectJ)")
    public void userCodeCall() {
        //pointcut body, should be empty
    }

    @AfterReturning(
            pointcut = "assertFactoryCall() && userCodeCall()",
            returning = "result"
    )

    /**
     * Handles the log assert creation callback.
     *
     * @param joinPoint the join point
     * @param result the model object or framework result to process
     */
    public void logAssertCreation(final JoinPoint joinPoint, final Object result) {
        if (isRecordingMuted() || !(result instanceof AbstractAssert)) {
            return;
        }

        final AbstractAssert<?, ?> assertion = (AbstractAssert<?, ?>) result;
        getRecorder().assertionCreated(getLifecycle(), assertion, firstArgumentOf(joinPoint));
    }

    /**
     * Returns the log assert operation.
     *
     * @param joinPoint the join point
     * @param assertion the assertion
     * @return the log assert operation
     * @throws Throwable if the underlying framework operation fails
     */
    @Around("assertOperationCall(assertion) && userCodeCall()")
    public Object logAssertOperation(final ProceedingJoinPoint joinPoint,
                                     final AbstractAssert<?, ?> assertion)
            throws Throwable {
        final String methodName = getMethodName(joinPoint);
        if (isRecordingMuted() || getRecorder().isIgnored(methodName)) {
            return joinPoint.proceed();
        }

        final AssertJOperation operation = getRecorder().startOperation(
                getLifecycle(),
                assertion,
                methodName,
                joinPoint.getArgs()
        );
        try {
            final Object result = joinPoint.proceed();
            getRecorder().operationPassed(operation, result);
            return result;
        } catch (Throwable throwable) {
            getRecorder().operationFailed(operation, throwable);
            throw throwable;
        }
    }

    @After(
        "execution(public void org.assertj.core.api.DefaultAssertionErrorCollector.collectAssertionError("
                + "java.lang.AssertionError)) && args(error)"
    )

    /**
     * Handles the soft assertion failed callback.
     *
     * @param error the error reported by the framework
     */
    public void softAssertionFailed(final AssertionError error) {
        getRecorder().softAssertionFailed(error);
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle.set(allure);
        clearContext();
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return lifecycle.get();
    }

    /**
     * Handles the clear context callback.
     */
    public static void clearContext() {
        RECORDER.remove();
    }

    static <T> T withoutRecording(final Supplier<T> supplier) {
        final boolean previous = RECORDING_MUTED.get();
        RECORDING_MUTED.set(true);
        try {
            return supplier.get();
        } finally {
            RECORDING_MUTED.set(previous);
        }
    }

    private static AssertJRecorder getRecorder() {
        return RECORDER.get();
    }

    private static boolean isRecordingMuted() {
        return RECORDING_MUTED.get();
    }

    private static Object firstArgumentOf(final JoinPoint joinPoint) {
        return joinPoint.getArgs().length == 0 ? null : joinPoint.getArgs()[0];
    }

    private static String getMethodName(final ProceedingJoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
    }
}
