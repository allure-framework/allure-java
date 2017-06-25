package io.qameta.allure.junit4.migration;

import io.qameta.allure.aspects.Allure1TestCaseMigration;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Provides migration from Allure 1 for Junit4-based tests.
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4MigrationAspects extends Allure1TestCaseMigration {

    /**
     * Pointcut for things annotated with {@link org.junit.Test}.
     */
    @Pointcut("@annotation(org.junit.Test)")
    public void withJunitAnnotation() {
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
}
