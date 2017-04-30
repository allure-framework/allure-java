package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Aspects (AspectJ) for handling {@link Attachment}.
 */
@Aspect
public class Allure1AttachAspects {

    private static AllureLifecycle lifecycle;

    /**
     * Pointcut for things annotated with {@link ru.yandex.qatools.allure.annotations.Attachment}
     */
    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Attachment)")
    public void withAttachmentAnnotation() {
        //pointcut body, should be empty
    }

    /**
     * Pointcut for any methods
     */
    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    /**
     * Process data returned from method annotated with {@link ru.yandex.qatools.allure.annotations.Attachment}
     * If returned data is not a byte array, then use toString() method, and get bytes from it using
     *
     * @param joinPoint
     * @param result
     */
    @AfterReturning(pointcut = "anyMethod() && withAttachmentAnnotation()", returning = "result")
    public void attachment(JoinPoint joinPoint, Object result) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Attachment attachment = methodSignature.getMethod().getAnnotation(Attachment.class);

        String title = Allure1AspectUtils.getTitle(
                attachment.value(),
                methodSignature.getName(),
                joinPoint.getThis(),
                joinPoint.getArgs()
        );

        byte[] bytes = (result instanceof byte[])
                ? (byte[]) result : result.toString().getBytes(StandardCharsets.UTF_8);
        getLifecycle().addAttachment(title, attachment.type(), "", bytes);
    }

    /**
     * For tests only
     */
    static void setLifecycle(AllureLifecycle lifecycle) {
        Allure1AttachAspects.lifecycle = lifecycle;
    }

    private static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }


}
