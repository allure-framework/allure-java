package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Allure junit aspects.
 */
@Aspect
public class Allure1JunitAspects {

    private static AllureLifecycle lifecycle;

    /**
     * Pointcut for things annotated with {@link org.junit.Test}.
     */
    @Pointcut("@annotation(org.junit.Test)")
    public void withTestAnnotation() {
        //pointcut body, should be empty
    }

    /**
     * Pointcut for any methods.
     */
    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withTestAnnotation()")
    public void testStart(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Method method = methodSignature.getMethod();
        processAnnotations(method);
    }

    private void processAnnotations(final Method method) {
        final Allure1Annotations annotations = new Allure1Annotations(method);
        try {
            getLifecycle().updateTestCase(annotations::updateTitle);
            getLifecycle().updateTestCase(annotations::updateDescription);
            getLifecycle().updateTestCase(annotations::updateLabels);
        } catch (Exception e) {
            doNothing();
        }
    }

    @SuppressWarnings("PMD")
    private void doNothing() {

    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure1JunitAspects.lifecycle = lifecycle;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }


}
