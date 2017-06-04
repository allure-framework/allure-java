package io.qameta.allure.junit4;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4ListenerAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AllureJunit4ListenerAspect.class);
    private final AllureJunit4 allureJunit4 = new AllureJunit4();

    @Pointcut("execution(void org.junit.runners.ParentRunner.run(org.junit.runner.notification.RunNotifier))")
    public void run() {
        //empty pointcut body
    }

    @Around("run()")
    public void run(final ProceedingJoinPoint pjp) {
        final Object[] args = pjp.getArgs();
        final RunNotifier notifier = (RunNotifier) args[0];
        notifier.removeListener(allureJunit4);
        notifier.addListener(allureJunit4);
        notifier.fireTestRunStarted(Description.createSuiteDescription("Tests"));

        try {
            pjp.proceed(args);
        } catch (Throwable throwable) {
            LOG.error("Exception on proceed: {}", throwable);
        }

        notifier.fireTestRunFinished(new Result());
    }
}
