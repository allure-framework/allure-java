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
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to add some links to results. Usage:
 * <pre>
 * &#064;Link("https://qameta.io")
 * public void myTest() {
 *     ...
 * }
 * </pre>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Links.class)
@LabelAnnotation(name = "link")
public @interface Link {

    /**
     * Alias for {@link #name()}.
     *
     * @return the link name.
     */
    String value() default "";

    /**
     * Name for link, by default url.
     *
     * @return the link name.
     */
    String name() default "";

    /**
     * Url for link. By default will search for system property `allure.link.{type}.pattern`, and use it
     * to generate url.
     *
     * @return the link url.
     */
    String url() default "";

    /**
     * This type is used for create an icon for link. Also there is few reserved types such as issue and tms.
     *
     * @return the link type.
     */
    String type() default "custom";
}
