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
 * All tests should cover http://hamcrest.org/JavaHamcrest/tutorial "Number" section
 * <ui>
 * <li>closeTo</li>
 * <li>greaterThan</li>
 * <li>greaterThanOrEqualTo</li>
 * <li>lessThan</li>
 * <li>lessThanOrEqualTo</li>
 * </ui>
 */
@SuppressWarnings("all")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AllureHamcrestNumberMatchersTest {

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(
                        0.434d, is(closeTo(0.41d, 0.45d)),
                        "assert \"0.434\" is a numeric value within <0.45> of <0.41>"
                ),
                Arguments.of(
                        0.434d, is(greaterThan(0.41d)),
                        "assert \"0.434\" is a value greater than <0.41>"
                ),
                Arguments.of(
                        0.434d, is(greaterThanOrEqualTo(0.41d)),
                        "assert \"0.434\" is a value equal to or greater than <0.41>"
                ),
                Arguments.of(
                        0.41d, is(lessThan(0.434d)),
                        "assert \"0.41\" is a value less than <0.434>"
                ),
                Arguments.of(
                        0.41d, is(lessThanOrEqualTo(0.434d)),
                        "assert \"0.41\" is a value less than or equal to <0.434>"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void hamcrestAssertNameForNumberMatchers(double actual, Matcher matcher, String expectedName) {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(actual, matcher),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(expectedName);
    }
}
