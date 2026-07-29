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
package io.qameta.allure.testng;

import io.qameta.allure.model.Parameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * You can use this annotation to add test instance parameters to your TestNG tests.
 * Fields declared by the test class and its superclasses are supported.
 * If multiple fields resolve to the same parameter name, the field declared by
 * the most-derived class takes precedence.
 *
 * @see Parameter
 * @see Parameter.Mode
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TestInstanceParameter {

    /**
     * The name of parameter. If empty, the field name will be used.
     *
     * @return the name of parameter.
     */
    String value() default "";

    /**
     * The parameter mode. It controls how the value is displayed and does not
     * exclude the parameter from history ID generation.
     *
     * @return the parameter mode.
     */
    Parameter.Mode mode() default Parameter.Mode.DEFAULT;

    /**
     * Set it to true to exclude the parameter from history ID generation.
     *
     * @return true if parameter is excluded, false otherwise.
     */
    boolean excluded() default false;

}
