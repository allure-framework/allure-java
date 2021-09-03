/*
 *  Copyright 2021 Qameta Software OÜ
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
package io.qameta.allure.hamcrest;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * All tests should cover http://hamcrest.org/JavaHamcrest/tutorial "Logical" section
 * <ui>
 * <li>allOf</li>
 * <li>anyOf</li>
 * <li>not</li>
 * </ui>
 */
@SuppressWarnings("all")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AllureHamcrestLogicalMatchersTest {

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(
                        "thebiscuit", allOf(startsWith("the"), containsString("biscuit")),
                        "assert \"thebiscuit\" (a string starting with \"the\" and a string containing \"biscuit\")"
                ),
                Arguments.of(
                        "thebiscuit", anyOf(startsWith("the"), containsString("test")),
                        "assert \"thebiscuit\" (a string starting with \"the\" or a string containing \"test\")"
                ),
                Arguments.of(
                        "test", is(not(anyOf(startsWith("the"), containsString("biscuit")))),
                        "assert \"test\" is not (a string starting with \"the\" or a string containing \"biscuit\")"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void hamcrestAssertNameForLogicalMatchers(String actual, Matcher matcher, String expectedName) {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(actual, matcher),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(expectedName);
    }
}
