/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that produce attachments. Returned value of such methods
 * will be copied and shown in the report as attachment.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Attachment {

    /**
     * The attachment name.
     *
     * @return the attachment name.
     */
    String value() default "";

    /**
     * Valid attachment MimeType, for example "text/plain" or "application/json".
     *
     * @return the attachment type.
     */
    String type() default "";

    /**
     * Optional attachment file extension. By default file extension will be determined by
     * provided media type. Should be started with dot.
     *
     * @return the attachment file extension.
     */
    String fileExtension() default "";
}
