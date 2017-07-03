package io.qameta.allure.junit4;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.notification.RunNotifier;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4ListenerAspect {

    private final AllureJunit4 allure = new AllureJunit4();

    @AfterReturning(pointcut = "call(org.junit.runner.notification.RunNotifier.new(..))", returning = "notifier")
    public void addListener(final RunNotifier notifier) {
        notifier.removeListener(allure);
        notifier.addListener(allure);
    }

}
