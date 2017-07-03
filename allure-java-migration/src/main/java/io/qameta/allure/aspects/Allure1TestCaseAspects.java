package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;

/**
 * Allure junit aspects.
 */
@Aspect
public class Allure1TestCaseAspects {

    private static AllureLifecycle lifecycle;

    @Before("execution(@org.junit.Test * *.*(..))")
    public void junitTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

    @Before("execution(@org.testng.annotations.Test * *.*(..))")
    public void testNgTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

    private void updateTestCase(final JoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Object[] args = joinPoint.getArgs();
        final Object target = joinPoint.getTarget();
        final Allure1Annotations annotations = new Allure1Annotations(target, signature, args);
        getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            getLifecycle().updateTestCase(uuid, annotations::updateTitle);
            getLifecycle().updateTestCase(uuid, annotations::updateDescription);
            getLifecycle().updateTestCase(uuid, annotations::updateParameters);
            getLifecycle().updateTestCase(uuid, annotations::updateLabels);
            getLifecycle().updateTestCase(uuid, annotations::updateLinks);
        });
    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure1TestCaseAspects.lifecycle = lifecycle;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

}
