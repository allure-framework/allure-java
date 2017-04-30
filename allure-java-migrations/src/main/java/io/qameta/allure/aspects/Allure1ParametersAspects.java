package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Attachment;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Objects;

/**
 * Aspects (AspectJ) for handling {@link Parameter}.
 */
@Aspect
public class Allure1ParametersAspects {

    private static AllureLifecycle lifecycle;


    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Parameter)")
    public void withParameterAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("set(* *)")
    public void setValueToAnyField() {
        //pointcut body, should be empty
    }

    @After("setValueToAnyField() && withParameterAnnotation()")
    public void parameterValueChanged(JoinPoint joinPoint) {
        try {
            FieldSignature fieldSignature = (FieldSignature) joinPoint.getSignature();
            Parameter parameter = fieldSignature.getField().getAnnotation(Parameter.class);
            String name = parameter.value().isEmpty() ? fieldSignature.getName() : parameter.value();
            getLifecycle().updateTestCase(testResult ->
                    testResult.getParameters().add(new io.qameta.allure.model.Parameter()
                            .withName(name).withValue(joinPoint.getArgs()[0].toString())));
        } catch (Exception ignored) {
        }
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    static void setLifecycle(final AllureLifecycle allure) {
        lifecycle = allure;
    }

    private static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

}
