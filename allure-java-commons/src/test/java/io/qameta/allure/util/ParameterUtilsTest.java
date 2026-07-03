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
package io.qameta.allure.util;

import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.qameta.allure.Allure.attachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.tuple;

class ParameterUtilsTest {

    @Test
    void shouldApplyParamAnnotationToFrameworkParameter() throws NoSuchMethodException {
        step("Create framework parameters and verify @Param metadata", () -> {
            final Method method = getClass().getDeclaredMethod(
                    "methodWithParameters",
                    String.class,
                    String.class,
                    String.class
            );
            final List<Parameter> parameters = Arrays.asList(
                    ParameterUtils.createParameter(method.getParameters()[0], "1", "first"),
                    ParameterUtils.createParameter(method.getParameters()[1], "2", "second"),
                    ParameterUtils.createParameter(method.getParameters()[2], "3", "third")
            );

            attachment(
                    "created parameters", "text/plain", parameters.stream()
                            .map(
                                    parameter -> String.format(
                                            "%s=%s excluded=%s mode=%s",
                                            parameter.getName(),
                                            parameter.getValue(),
                                            parameter.getExcluded(),
                                            parameter.getMode()
                                    )
                            )
                            .collect(Collectors.joining("\n"))
            );

            assertThat(parameters)
                    .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                    .containsExactly(
                            tuple("Named", "1", false, Parameter.Mode.DEFAULT),
                            tuple("Hidden", "2", true, Parameter.Mode.HIDDEN),
                            tuple("third", "3", null, null)
                    );
        });
    }

    @Test
    void shouldUseReflectionNameWhenFrameworkNameIsNotProvided() throws NoSuchMethodException {
        step("Create parameters with reflection names and verify @Param metadata", () -> {
            final Method method = getClass().getDeclaredMethod(
                    "methodWithParameters",
                    String.class,
                    String.class,
                    String.class
            );
            final List<Parameter> parameters = Arrays.asList(
                    ParameterUtils.createParameter(method.getParameters()[0], "1"),
                    ParameterUtils.createParameter(method.getParameters()[1], "2"),
                    ParameterUtils.createParameter(method.getParameters()[2], "3")
            );

            assertThat(parameters)
                    .extracting(Parameter::getName, Parameter::getValue, Parameter::getExcluded, Parameter::getMode)
                    .containsExactly(
                            tuple("Named", "1", false, Parameter.Mode.DEFAULT),
                            tuple("Hidden", "2", true, Parameter.Mode.HIDDEN),
                            tuple("plain", "3", null, null)
                    );
        });
    }

    @Test
    void shouldRejectNullFrameworkDefaultName() throws NoSuchMethodException {
        step("Verify framework default name is required", () -> {
            final Method method = getClass().getDeclaredMethod(
                    "methodWithParameters",
                    String.class,
                    String.class,
                    String.class
            );

            assertThatNullPointerException()
                    .isThrownBy(() -> ParameterUtils.createParameter(method.getParameters()[0], "1", null))
                    .withMessage("defaultName");
        });
    }

    void methodWithParameters(@Param("Named") final String named,
                              @Param(
                                      name = "Hidden",
                                      excluded = true,
                                      mode = Parameter.Mode.HIDDEN
                              ) final String hidden,
                              final String plain) {
    }

}
