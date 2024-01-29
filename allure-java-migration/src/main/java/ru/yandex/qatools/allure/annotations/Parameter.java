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
package ru.yandex.qatools.allure.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * You can use this annotation to add parameters to your tests:
 * <pre>
 * &#064;Parameter("My Param")
 * private String myParameter;
 *
 * &#064;Test
 * public void myTest() throws Exception {
 *      myParameter = "first";
 *      myParameter = "second";
 *      myParameter = "third";
 * }
 * </pre>
 * All three values will be added to report
 *
 * Note that the initializations of constant fields (static final fields
 * where the initializer is a constant string object or primitive value)
 * are not join points, since Java requires their references to be inlined.
 *
 * value - it's name of parameter, field name by default
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Parameter {

    String value() default "";

}
