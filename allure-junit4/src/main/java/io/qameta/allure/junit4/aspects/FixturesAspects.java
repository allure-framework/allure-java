package io.qameta.allure.junit4.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * @author charlie (Dmitry Baev).
 */
@Aspect
public class FixturesAspects {

    //private static AllureOld ALLURE = AllureOld.LIFECYCLE;

    @Pointcut("@annotation(org.junit.Before)")
    public void withBeforeAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("@annotation(org.junit.After)")
    public void withAfterAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("@annotation(org.junit.Test)")
    public void withTestAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withBeforeAnnotation()")
    public void beforeStart(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
//        ALLURE.startTestCaseBefore(new TestBeforeResult()
//                .withName(methodSignature.getName())
//        );
    }

    @AfterThrowing("anyMethod() && withBeforeAnnotation()")
    public void beforeFailed() {
//        ALLURE.finishBefore();
    }

    @AfterReturning("anyMethod() && withBeforeAnnotation()")
    public void beforeStop() {
//        ALLURE.finishBefore();
    }

    @Before("anyMethod() && withTestAnnotation()")
    public void testStart() {
//        ALLURE.startTestCase();
    }

    @AfterThrowing("anyMethod() && withTestAnnotation()")
    public void testFailed() {
//        ALLURE.finishTestCase();
    }

    @AfterReturning("anyMethod() && withTestAnnotation()")
    public void testStop() {
//        ALLURE.finishTestCase();
    }


    @Before("anyMethod() && withAfterAnnotation()")
    public void afterStart(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
//        ALLURE.startAfter(new TestAfterResult()
//                .withName(methodSignature.getName())
//        );
    }

    @AfterThrowing("anyMethod() && withBeforeAnnotation()")
    public void afterFailed() {
        //ALLURE.finishAfter();
    }

    @AfterReturning("anyMethod() && withAfterAnnotation()")
    public void afterStop() {
        //ALLURE.finishAfter();
    }
}
