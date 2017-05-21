package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;
import java.util.UUID;

import static io.qameta.allure.util.AspectUtils.getParameters;
import static io.qameta.allure.util.AspectUtils.getParametersMap;
import static io.qameta.allure.util.NamingUtils.processNameTemplate;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.processDescription;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 24.10.13
 * @author sskorol (Sergey Korol)
 */
@Aspect
public class StepsAspects {

    private static AllureLifecycle lifecycle;

    @Pointcut("@annotation(io.qameta.allure.Step)")
    public void withStepAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withStepAnnotation()")
    public void stepStart(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Step step = methodSignature.getMethod().getAnnotation(Step.class);
        final String name = step.value().isEmpty()
                ? methodSignature.getName()
                : processNameTemplate(step.value(), getParametersMap(methodSignature, joinPoint.getArgs()));
        final String uuid = UUID.randomUUID().toString();
        final StepResult result = new StepResult()
                .withName(name)
                .withParameters(getParameters(methodSignature, joinPoint.getArgs()));
        processDescription(getClass().getClassLoader(), methodSignature.getMethod(), result);
        getLifecycle().startStep(uuid, result);
    }

    @AfterThrowing(pointcut = "anyMethod() && withStepAnnotation()", throwing = "e")
    public void stepFailed(final Throwable e) {
        getLifecycle().updateStep(result -> result
                .withStatus(getStatus(e).orElse(Status.BROKEN))
                .withStatusDetails(getStatusDetails(e).orElse(null)));
        getLifecycle().stopStep();
    }

    @AfterReturning("anyMethod() && withStepAnnotation()")
    public void stepStop() {
        getLifecycle().updateStep(step -> step.withStatus(Status.PASSED));
        getLifecycle().stopStep();
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
}
