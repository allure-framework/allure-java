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
package io.qameta.allure.jupiter.features;

import io.qameta.allure.Param;
import io.qameta.allure.jupiter.AllureJupiter;
import io.qameta.allure.model.Parameter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("unused")
@ExtendWith(AllureJupiter.class)
public class ParamAnnotationTests {

    @ParameterizedTest
    @ValueSource(strings = "named value")
    void namedParameter(@Param("custom name") final String value) {
    }

    @ParameterizedTest
    @ValueSource(strings = "masked value")
    void maskedParameter(@Param(
            name = "secret",
            mode = Parameter.Mode.MASKED
    ) final String value) {
    }

    @ParameterizedTest
    @ValueSource(strings = "excluded value")
    void excludedParameter(@Param(
            name = "history key",
            excluded = true
    ) final String value) {
    }

    @ParameterizedTest
    @ValueSource(strings = "plain value")
    void defaultParameter(final String value) {
    }
}
