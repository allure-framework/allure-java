package io.qameta.allure.junit4;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.notification.RunNotifier;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4ListenerAspect {

    private final AllureJunit4 allure = new AllureJunit4();

    @After("execution(org.junit.runner.notification.RunNotifier.new())")
    public void addListener(final JoinPoint point) {
        final RunNotifier notifier = (RunNotifier) point.getThis();
        notifier.removeListener(allure);
        notifier.addListener(allure);
    }

}
