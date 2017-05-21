package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
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

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.processDescription;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.joor.Reflect.on;

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
        final String uuid = UUID.randomUUID().toString();
        final StepResult result = new StepResult()
                .withName(getName(methodSignature, joinPoint.getArgs()))
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

    private static Parameter[] getParameters(final MethodSignature signature, final Object... args) {
        return IntStream.range(0, args.length).mapToObj(index -> {
            final String name = signature.getParameterNames()[index];
            final String value = Objects.toString(args[index]);
            return new Parameter().withName(name).withValue(value);
        }).toArray(Parameter[]::new);
    }

    private static String getMatchingPropertyValue(final String input,
                                                   final Function<String, Object> parameterExtractor) {
        final Matcher matcher = Pattern.compile("\\{([^}]*)}").matcher(input);
        String output = input;

        while (matcher.find()) {
            final String[] matches = matcher.group(1).split("\\.");
            final Object parameter = parameterExtractor.apply(matches[0]);

            output = parameter != null
                    ? output.replace(matcher.group(0), getParameterPropertyValue(parameter, matches))
                    : output;
        }

        return output;
    }

    private static Object getMatchingParameterValue(final MethodSignature signature, final String parameterName,
                                                    final Object... args) {
        return IntStream.range(0, args.length)
                        .filter(i -> parameterName.equals(signature.getParameterNames()[i]))
                        .mapToObj(i -> Objects.isNull(args[i]) ? Objects.toString(null) : args[i])
                        .findFirst()
                        .orElse(null);
    }

    private static String getParameterPropertyValue(final Object rootObject, final String... fieldNames) {
        Object currentObject = rootObject;

        for (int i = 1; i < fieldNames.length; i++) {
            final int currentIndex = i;

            if (currentObject == null) {
                break;
            } else if (currentObject instanceof Collection) {
                currentObject = Stream.of((Collection<?>) currentObject)
                                      .map(ob -> extractProperty(ob, fieldNames[currentIndex]))
                                      .collect(toList());
            } else if (currentObject instanceof Object[]) {
                currentObject = Stream.of((Object[]) currentObject)
                                      .map(ob -> extractProperty(ob, fieldNames[currentIndex]))
                                      .toArray();
            } else {
                currentObject = extractProperty(currentObject, fieldNames[currentIndex]);
            }
        }

        return ofNullable(currentObject)
                .filter(ob -> ob instanceof Object[])
                .map(ob -> Objects.toString(asList((Object[]) ob)))
                .orElse(Objects.toString(currentObject));
    }

    private static Object extractProperty(final Object rootObject, final String fileName) {
        try {
            return on(rootObject).get(fileName);
        } catch (Exception ignored) {
            return rootObject;
        }
    }

    private static String getName(final MethodSignature signature, final Object... args) {
        return ofNullable(signature.getMethod().getAnnotation(Step.class))
                .map(Step::value)
                .filter(value -> !value.isEmpty())
                .map(value -> getMatchingPropertyValue(value, arg -> getMatchingParameterValue(signature, arg, args)))
                .orElseGet(signature::getName);
    }
}
