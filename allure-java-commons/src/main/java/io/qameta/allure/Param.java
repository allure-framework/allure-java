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
package io.qameta.allure;

import io.qameta.allure.model.Parameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation used to add parameters to results
 * from method parameters.
 *
 * @see Parameter
 * @see Parameter.Mode
 * @see Allure#parameter(String, Object)
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Param {

    /**
     * Alias for {@link #name()}. Overrides value, specified in {@link #name()}.
     */
    String value() default "";

    /**
     * The name of parameter. Be careful, changing parameter's name
     * may affect method signature and, as result, may get different
     * test case generated.
     * <p>
     * If not specified, the parameter name from reflection will be used.
     *
     * @return the name of parameter.
     */
    String name() default "";

    /**
     * The parameter mode.
     *
     * @return the parameter mode.
     */
    Parameter.Mode mode() default Parameter.Mode.DEFAULT;

    /**
     * Set it to true to exclude the parameter from historyKey generation.
     *
     * @return true if parameter is excluded, false otherwise.
     */
    boolean excluded() default false;

}
