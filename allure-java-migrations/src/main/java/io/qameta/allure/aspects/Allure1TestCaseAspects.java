package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;

/**
 * Allure junit aspects.
 */
@Aspect
public class Allure1TestCaseAspects {

    private static AllureLifecycle lifecycle;

    /**
     * Pointcut for things annotated with {@link org.junit.Test}.
     */
    @Pointcut("@annotation(org.junit.Test)")
    public void withJunitAnnotation() {
        //pointcut body, should be empty
    }

    /**
     * Pointcut for things annotated with {@link org.testng.annotations.Test}.
     */
    @Pointcut("@annotation(org.testng.annotations.Test)")
    public void withTestNgAnnotation() {
        //pointcut body, should be empty
    }

    /**
     * Pointcut for any methods.
     */
    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withJunitAnnotation()")
    public void junitTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

    @Before("anyMethod() && withTestNgAnnotation()")
    public void testNgTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

    private void updateTestCase(final JoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Object[] args = joinPoint.getArgs();
        final Object target = joinPoint.getTarget();
        final Allure1Annotations annotations = new Allure1Annotations(target, signature, args);
        if (getLifecycle().hasCurrentTestCase()) {
            getLifecycle().updateTestCase(annotations::updateTitle);
            getLifecycle().updateTestCase(annotations::updateDescription);
            getLifecycle().updateTestCase(annotations::updateParameters);
            getLifecycle().updateTestCase(annotations::updateLabels);
        }
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
