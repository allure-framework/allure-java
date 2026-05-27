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
package io.qameta.allure.playwright;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.AttachmentType;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.UUID;
import java.util.function.Supplier;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Aspect that logs Playwright Java browser actions as Allure steps.
 */
@Aspect
public class AllurePlaywrightAspect {

    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    private static final InheritableThreadLocal<AllureLifecycle> LIFECYCLE = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    @Pointcut(
        "execution(public * com.microsoft.playwright.Page+.*(..))"
                + " || execution(public * com.microsoft.playwright.Frame+.*(..))"
                + " || execution(public * com.microsoft.playwright.Locator+.*(..))"
                + " || execution(public * com.microsoft.playwright.ElementHandle+.*(..))"
                + " || execution(public * com.microsoft.playwright.assertions.*Assertions+.*(..))"
    )

    /**
     * Handles the playwright api callback.
     */
    public void playwrightApi() {
        //pointcut body, should be empty
    }

    @Pointcut(
        "execution(public byte[] com.microsoft.playwright.Page+.screenshot(..))"
                + " || execution(public byte[] com.microsoft.playwright.Locator+.screenshot(..))"
                + " || execution(public byte[] com.microsoft.playwright.ElementHandle+.screenshot(..))"
    )

    /**
     * Handles the screenshot api callback.
     */
    public void screenshotApi() {
        //pointcut body, should be empty
    }

    @Pointcut(
        "execution(public com.microsoft.playwright.BrowserContext "
                + "com.microsoft.playwright.Browser+.newContext(..))"
    )

    /**
     * Handles the new context api callback.
     */
    public void newContextApi() {
        //pointcut body, should be empty
    }

    @Pointcut(
        "execution(public com.microsoft.playwright.Page com.microsoft.playwright.Browser+.newPage(..))"
                + " || execution(public com.microsoft.playwright.Page "
                + "com.microsoft.playwright.BrowserContext+.newPage(..))"
    )

    /**
     * Handles the new page api callback.
     */
    public void newPageApi() {
        //pointcut body, should be empty
    }

    @Pointcut(
        "execution(public void com.microsoft.playwright.Browser+.close(..))"
                + " || execution(public void com.microsoft.playwright.BrowserContext+.close(..))"
                + " || execution(public void com.microsoft.playwright.Page+.close(..))"
    )

    /**
     * Handles the close api callback.
     */
    public void closeApi() {
        //pointcut body, should be empty
    }

    /**
     * Returns the log playwright step.
     *
     * @param joinPoint the join point
     * @return the log playwright step
     * @throws Throwable if the underlying framework operation fails
     */
    @Around("playwrightApi() && !screenshotApi()")
    public Object logPlaywrightStep(final ProceedingJoinPoint joinPoint) throws Throwable {
        registerTarget(joinPoint.getTarget());
        if (shouldSkip()) {
            return joinPoint.proceed();
        }
        final PlaywrightAction action = PlaywrightAction.from(joinPoint, false);
        if (!action.isLogged()) {
            return joinPoint.proceed();
        }
        return runStep(joinPoint, action, false);
    }

    /**
     * Returns the log screenshot step.
     *
     * @param joinPoint the join point
     * @return the log screenshot step
     * @throws Throwable if the underlying framework operation fails
     */
    @Around("screenshotApi()")
    public Object logScreenshotStep(final ProceedingJoinPoint joinPoint) throws Throwable {
        registerTarget(joinPoint.getTarget());
        if (shouldSkip()) {
            return joinPoint.proceed();
        }
        return runStep(joinPoint, PlaywrightAction.from(joinPoint, true), true);
    }

    /**
     * Returns the register created playwright object.
     *
     * @param joinPoint the join point
     * @return the register created playwright object
     * @throws Throwable if the underlying framework operation fails
     */
    @Around("newContextApi() || newPageApi()")
    public Object registerCreatedPlaywrightObject(final ProceedingJoinPoint joinPoint) throws Throwable {
        final Object result = joinPoint.proceed();
        if (!shouldSkipArtifacts()) {
            register(result);
        }
        return result;
    }

    /**
     * Returns the attach close artifacts.
     *
     * @param joinPoint the join point
     * @return the attach close artifacts
     * @throws Throwable if the underlying framework operation fails
     */
    @Around("closeApi()")
    public Object attachCloseArtifacts(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (shouldSkipArtifacts()) {
            return joinPoint.proceed();
        }
        final AllurePlaywright.CloseArtifacts closeArtifacts = beforeClose(joinPoint.getTarget());
        final Object result = joinPoint.proceed();
        afterClose(closeArtifacts);
        return result;
    }

    /**
     * For tests only.
     *
     * @param lifecycle allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        LIFECYCLE.set(lifecycle);
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return LIFECYCLE.get();
    }

    private static Object runStep(final ProceedingJoinPoint joinPoint, final PlaywrightAction action,
                                  final boolean screenshot)
            throws Throwable {
        final String uuid = UUID.randomUUID().toString();
        DEPTH.set(DEPTH.get() + 1);
        getLifecycle().startStep(uuid, new StepResult().setName(action.getName()));
        try {
            final Object result = joinPoint.proceed();
            if (screenshot && result instanceof byte[]) {
                attachScreenshot(joinPoint, (byte[]) result);
            }
            getLifecycle().updateStep(uuid, step -> step.setStatus(Status.PASSED));
            return result;
        } catch (Throwable e) {
            getLifecycle().updateStep(
                    uuid, step -> step
                            .setStatus(getStatus(e).orElse(Status.BROKEN))
                            .setStatusDetails(getStatusDetails(e).orElse(null))
            );
            throw e;
        } finally {
            getLifecycle().stopStep(uuid);
            DEPTH.set(DEPTH.get() - 1);
        }
    }

    private static void attachScreenshot(final ProceedingJoinPoint joinPoint, final byte[] bytes) {
        if (AllurePlaywrightConfig.shouldAttachScreenshots()) {
            final AttachmentType type = AllurePlaywright.screenshotType(joinPoint.getArgs());
            getLifecycle().addAttachment("Screenshot", type.getMediaType(), type.getExtension(), bytes);
        }
    }

    private static void registerTarget(final Object target) {
        if (!shouldSkipArtifacts()) {
            register(target);
        }
    }

    private static void register(final Object result) {
        AllurePlaywright.withAspectSuppressed(new Supplier<Object>() {
            @Override
            public Object get() {
                if (result instanceof com.microsoft.playwright.Page) {
                    AllurePlaywrightRegistry.registerPage((com.microsoft.playwright.Page) result);
                } else if (result instanceof com.microsoft.playwright.BrowserContext) {
                    AllurePlaywright.register((com.microsoft.playwright.BrowserContext) result);
                }
                return null;
            }
        });
    }

    private static AllurePlaywright.CloseArtifacts beforeClose(final Object target) {
        return AllurePlaywright.withAspectSuppressed(new Supplier<AllurePlaywright.CloseArtifacts>() {
            @Override
            public AllurePlaywright.CloseArtifacts get() {
                return AllurePlaywright.beforeClose(target);
            }
        });
    }

    private static void afterClose(final AllurePlaywright.CloseArtifacts closeArtifacts) {
        AllurePlaywright.withAspectSuppressed(new Supplier<Object>() {
            @Override
            public Object get() {
                closeArtifacts.attachAfterClose();
                return null;
            }
        });
    }

    private static boolean shouldSkip() {
        return !AllurePlaywrightConfig.isStepsEnabled()
                || AllurePlaywright.isAspectSuppressed()
                || DEPTH.get() > 0
                || !getLifecycle().getCurrentTestCaseOrStep().isPresent();
    }

    private static boolean shouldSkipArtifacts() {
        return AllurePlaywright.isAspectSuppressed()
                || !AllurePlaywright.hasAllureContext();
    }
}
