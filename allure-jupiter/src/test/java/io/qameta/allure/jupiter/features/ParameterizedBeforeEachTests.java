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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(AllureJupiter.class)
public class ParameterizedBeforeEachTests {

    private static boolean failBeforeEach;

    public static void failBeforeEach(final boolean fail) {
        failBeforeEach = fail;
    }

    @BeforeEach
    void setUp() {
        if (failBeforeEach) {
            throw new IllegalStateException("fail in beforeEach");
        }
    }

    @ParameterizedTest
    @CsvSource("first value, second value")
    void parameterizedTest(@Param("first") final String first,
                           @Param("second") final String second) {
    }
}
