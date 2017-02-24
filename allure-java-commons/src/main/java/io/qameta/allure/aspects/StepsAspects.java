package io.qameta.allure.aspects;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.qameta.allure.ResultsUtils.getStatus;
import static io.qameta.allure.ResultsUtils.getStatusDetails;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 24.10.13
 */
@Aspect
public class StepsAspects {

    private static AllureLifecycle ALLURE = AllureLifecycle.INSTANCE;

    @Pointcut("@annotation(io.qameta.allure.Step)")
    public void withStepAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withStepAnnotation()")
    public void stepStart(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String uuid = UUID.randomUUID().toString();
        StepResult result = new StepResult()
                .withName(getName(methodSignature))
                .withParameters(getParameters(methodSignature, joinPoint.getArgs()));
        ALLURE.startStep(uuid, result);
    }

    @AfterThrowing(pointcut = "anyMethod() && withStepAnnotation()", throwing = "e")
    public void stepFailed(Throwable e) {
        ALLURE.updateStep(result -> result
                .withStatus(getStatus(e).orElse(Status.BROKEN))
                .withStatusDetails(getStatusDetails(e).orElse(null)));
        ALLURE.stopStep();
    }

    @AfterReturning("anyMethod() && withStepAnnotation()")
    public void stepStop() {
        ALLURE.updateStep(step -> step.withStatus(Status.PASSED));
        ALLURE.stopStep();
    }

    /**
     * For tests only
     */
    public static void setAllure(AllureLifecycle allure) {
        StepsAspects.ALLURE = allure;
    }

    private static Parameter[] getParameters(MethodSignature signature, Object... args) {
        return IntStream.range(0, args.length).mapToObj(index -> {
            String name = signature.getParameterNames()[index];
            String value = Objects.toString(args[index]);
            return new Parameter().withName(name).withValue(value);
        }).toArray(Parameter[]::new);
    }

    private static String getName(MethodSignature signature) {
        return Optional.ofNullable(signature.getMethod().getAnnotation(Step.class))
                .map(Step::value)
                .filter(s -> !s.isEmpty())
                .orElseGet(signature::getName);
    }
}