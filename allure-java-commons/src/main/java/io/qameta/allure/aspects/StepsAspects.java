package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;
import java.util.UUID;

import static io.qameta.allure.util.AspectUtils.getParameters;
import static io.qameta.allure.util.AspectUtils.getParametersMap;
import static io.qameta.allure.util.NamingUtils.processNameTemplate;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 24.10.13
 * @author sskorol (Sergey Korol)
 */
@Aspect
public class StepsAspects {
    private static AllureLifecycle lifecycle;

    @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
    @Around("@annotation(io.qameta.allure.Step) && execution(* *(..))")
    public Object step(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Step step = methodSignature.getMethod().getAnnotation(Step.class);
        final String name = step.value().isEmpty()
                ? methodSignature.getName()
                : processNameTemplate(step.value(), getParametersMap(methodSignature, joinPoint.getArgs()));
        final String uuid = UUID.randomUUID().toString();
        final StepResult result = new StepResult()
                .withName(name)
                .withParameters(getParameters(methodSignature, joinPoint.getArgs()));
        final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        final String qualifiedName = getFirstQNameInMap(stacktrace);
        getLifecycle().startStepThreadSafe(qualifiedName, uuid, result);

        try {
            final Object proceed = joinPoint.proceed();
            getLifecycle().updateStep(uuid, s -> s.withStatus(Status.PASSED));
            return proceed;
        } catch (Throwable e) {
            getLifecycle().updateStep(uuid, s -> s
                    .withStatus(getStatus(e).orElse(Status.BROKEN))
                    .withStatusDetails(getStatusDetails(e).orElse(null)));
            throw e;
        } finally {
            getLifecycle().stopStep(uuid);
        }
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle = allure;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

    private String getQualifiedName(StackTraceElement ex) {
        return ex.getClassName() + "." + ex.getMethodName();
    }

    private String getFirstQNameInMap(StackTraceElement[] exs) {
        for (StackTraceElement ex : exs) {
            String qName = getQualifiedName(ex);
            Thread thread = getLifecycle().getThread(qName);
            if (thread != null) {
                return qName;
            }
        }

        return null;
    }
}
