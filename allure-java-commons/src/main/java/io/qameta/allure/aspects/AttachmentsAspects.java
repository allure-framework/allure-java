package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.nio.charset.StandardCharsets;

/**
 * Aspects (AspectJ) for handling {@link Attachment}.
 *
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 24.10.13
 */
@Aspect
public class AttachmentsAspects {

    private static Allure ALLURE = Allure.LIFECYCLE;

    /**
     * Pointcut for things annotated with {@link Attachment}
     */
    @Pointcut("@annotation(io.qameta.allure.Attachment)")
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
     * Process data returned from method annotated with {@link }
     * If returned data is not a byte array, then use toString() method, and get bytes from it
     */
    @AfterReturning(pointcut = "anyMethod() && withAttachmentAnnotation()", returning = "result")
    public void attachment(JoinPoint joinPoint, Object result) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Attachment attachment = methodSignature.getMethod()
                .getAnnotation(Attachment.class);
        byte[] bytes = (result instanceof byte[]) ? (byte[]) result : result.toString()
                .getBytes(StandardCharsets.UTF_8);
        ALLURE.addAttachment(bytes, attachment.value(), attachment.type());
    }
}
