package io.qameta.allure.junit4;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.runner.notification.RunNotifier;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4ListenerAspect {

    private final AllureJunit4 allureJunit4 = new AllureJunit4();

    @Pointcut("execution(org.junit.runner.notification.RunNotifier.new())")
    public void run() {
        //empty pointcut body
    }

    @After("run()")
    public void run(final JoinPoint point) {
        final RunNotifier notifier = (RunNotifier) point.getThis();
        notifier.addListener(allureJunit4);
    }
}
