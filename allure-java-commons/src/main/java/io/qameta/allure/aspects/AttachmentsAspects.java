/*
 *  Copyright 2016-2026 Qameta Software Inc
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
import io.qameta.allure.Attachment;
import io.qameta.allure.AttachmentOptions;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.qameta.allure.util.AspectUtils.getParametersMap;
import static io.qameta.allure.util.NamingUtils.processNameTemplate;

/**
 * Aspects (AspectJ) for handling {@link Attachment}.
 *
 */
@Aspect
public class AttachmentsAspects {

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
     * Handles the attachment callback.
     * If returned data is not a byte array, then use toString() method, and get bytes from it.
     *
     * @param joinPoint the join point to process
     * @param result    the model object or framework result to process
     */
    @AfterReturning(
            pointcut = "anyMethod() && withAttachmentAnnotation()",
            returning = "result"
    )
    public void attachment(final JoinPoint joinPoint, final Object result) {
        // enrichment aspect: silently skip — including the content conversion — when no
        // executable is running, so a disabled Allure reporter produces no warnings
        final AllureLifecycle lifecycle = getLifecycle();
        if (lifecycle.getCurrentExecutableKey().isEmpty()) {
            return;
        }
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Attachment attachment = methodSignature.getMethod()
                .getAnnotation(Attachment.class);
        final byte[] bytes = (result instanceof byte[])
                ? (byte[]) result
                : Objects.toString(result)
                        .getBytes(StandardCharsets.UTF_8);

        final String name = attachment.value().isEmpty()
                ? methodSignature.getName()
                : processNameTemplate(attachment.value(), getParametersMap(joinPoint));
        lifecycle.addAttachment(
                name,
                attachment.type(),
                new ByteArrayInputStream(bytes),
                attachment.fileExtension().isEmpty()
                        ? AttachmentOptions.empty()
                        : AttachmentOptions.withFileExtension(attachment.fileExtension())
        );
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public static AllureLifecycle getLifecycle() {
        return Allure.getLifecycle();
    }
}
