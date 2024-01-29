/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
     * Pointcut for things annotated with {@link ru.yandex.qatools.allure.annotations.Attachment}.
     */
    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Attachment)")
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
     * Process data returned from method annotated with {@link ru.yandex.qatools.allure.annotations.Attachment}
     * If returned data is not a byte array, then use toString() method, and get bytes from it using.
     */
    @AfterReturning(pointcut = "anyMethod() && withAttachmentAnnotation()", returning = "result")
    public void attachment(final JoinPoint joinPoint, final Object result) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Attachment attachment = methodSignature.getMethod().getAnnotation(Attachment.class);

        final String title = Allure1Utils.getTitle(
                attachment.value(),
                methodSignature.getName(),
                joinPoint.getThis(),
                joinPoint.getArgs()
        );

        final byte[] bytes = (result instanceof byte[])
                ? (byte[]) result : Objects.toString(result).getBytes(StandardCharsets.UTF_8);
        getLifecycle().addAttachment(title, attachment.type(), "", bytes);
    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure1AttachAspects.lifecycle = lifecycle;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }


}
