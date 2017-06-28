package io.qameta.allure.testng.migration;

import io.qameta.allure.aspects.Allure1TestCaseMigration;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Provides migration from Allure 1 for Test NG-based tests.
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureTestNgMigrationAspects extends Allure1TestCaseMigration {

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

    @Before("anyMethod() && withTestNgAnnotation()")
    public void testNgTestStart(final JoinPoint joinPoint) {
        updateTestCase(joinPoint);
    }

}
