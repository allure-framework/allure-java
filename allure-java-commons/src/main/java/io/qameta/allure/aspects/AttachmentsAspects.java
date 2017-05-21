package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Attachment;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.qameta.allure.util.AspectUtils.getParametersMap;
import static io.qameta.allure.util.NamingUtils.processNameTemplate;

/**
 * Aspects (AspectJ) for handling {@link Attachment}.
 *
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 24.10.13
 */
@Aspect
public class AttachmentsAspects {

    private static AllureLifecycle lifecycle;

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

    /**
     * Sets lifecycle for aspects. Usually used in tests.
     *
     * @param lifecycle allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        AttachmentsAspects.lifecycle = lifecycle;
    }

    /**
     * Pointcut for things annotated with {@link Attachment}.
     */
    @Pointcut("@annotation(io.qameta.allure.Attachment)")
    public void withAttachmentAnnotation() {
        //pointcut body, should be empty
    }

    /**
     * Pointcut for any methods.
     */
    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    /**
     * Process data returned from method annotated with {@link Attachment}.
     * If returned data is not a byte array, then use toString() method, and get bytes from it.
     *
     * @param joinPoint the join point to process.
     * @param result    the returned value.
     */
    @AfterReturning(pointcut = "anyMethod() && withAttachmentAnnotation()", returning = "result")
    public void attachment(final JoinPoint joinPoint, final Object result) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Attachment attachment = methodSignature.getMethod()
                .getAnnotation(Attachment.class);
        final byte[] bytes = (result instanceof byte[]) ? (byte[]) result : result.toString()
                .getBytes(StandardCharsets.UTF_8);

        final String name = attachment.value().isEmpty()
                ? methodSignature.getName()
                : processNameTemplate(attachment.value(), getParametersMap(methodSignature, joinPoint.getArgs()));
        getLifecycle().addAttachment(name, attachment.type(), attachment.fileExtension(), bytes);
    }
}
