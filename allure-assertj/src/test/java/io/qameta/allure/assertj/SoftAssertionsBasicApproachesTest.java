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
package io.qameta.allure.assertj;

import io.qameta.allure.test.AllureFeatures;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.StandardSoftAssertionsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;

import static io.qameta.allure.assertj.util.SoftAssertionsUtils.eraseCollectedErrors;

/**
 * @author Achitheus (Yury Yurchenko).
 */
public class SoftAssertionsBasicApproachesTest {

    @AllureFeatures.Steps
    @DisplayName("Check hardcoded soft assertions object")
    @ParameterizedTest(name = "[{index}]: {arguments}")
    @MethodSource(value = "io.qameta.allure.assertj.util.SoftAssertionsTestsProvider#softAssertionsTests")
    void tests(String testName, Consumer<StandardSoftAssertionsProvider> test) {
        SoftAssertions softly = new SoftAssertions();
        test.accept(softly);
    }

    @AllureFeatures.Steps
    @DisplayName("Check autocloseable soft assertions")
    @ParameterizedTest(name = "[{index}]: {arguments}")
    @MethodSource(value = "io.qameta.allure.assertj.util.SoftAssertionsTestsProvider#softAssertionsTests")
    void autocloseableTests(String testName, Consumer<StandardSoftAssertionsProvider> test) {
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            test.accept(softly);
            eraseCollectedErrors(softly);
        }
    }

    @AllureFeatures.Steps
    @DisplayName("Check SoftAssertions.assertSoftly() method")
    @ParameterizedTest(name = "[{index}]: {arguments}")
    @MethodSource(value = "io.qameta.allure.assertj.util.SoftAssertionsTestsProvider#softAssertionsTests")
    void assertSoftlyMethodTests(String testName, Consumer<StandardSoftAssertionsProvider> test) {
        SoftAssertions.assertSoftly(softly -> {
            test.accept(softly);
            eraseCollectedErrors(softly);
        });
    }
}

