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
package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.qameta.allure.util.ResultsUtils.CUSTOM_LINK_TYPE;

/**
 * Marker annotation. Annotations marked by this annotation will be discovered
 * by Allure and added to test results as a link.
 *
 * @see Link
 * @see TmsLink
 * @see Issue
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Repeatable(LinkAnnotations.class)
public @interface LinkAnnotation {

    String DEFAULT_VALUE = "$$$$$$$$__value__$$$$$$$$";

    /**
     * The value of link. In not specified will take value from <code>value()</code>
     * method of target annotation.
     *
     * @return the value of the link to add.
     */
    String value() default DEFAULT_VALUE;

    /**
     * This type is used for create an icon for link. Also there is few reserved types such as issue and tms.
     *
     * @return the link type.
     */
    String type() default CUSTOM_LINK_TYPE;

    /**
     * Url for link. By default will search for system property `allure.link.{type}.pattern`, and use it
     * to generate url.
     *
     * @return the link url.
     */
    String url() default "";
}
