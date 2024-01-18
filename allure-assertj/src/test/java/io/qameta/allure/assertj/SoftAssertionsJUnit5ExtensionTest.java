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
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.StandardSoftAssertionsProvider;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;

import static io.qameta.allure.assertj.util.SoftAssertionsUtils.eraseCollectedErrors;

/**
 * @author Achitheus (Yury Yurchenko).
 */
@ExtendWith(SoftAssertionsExtension.class)
public class SoftAssertionsJUnit5ExtensionTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @AllureFeatures.Steps
    @DisplayName("Check soft assertions obj injected as field by JUnit5 extension")
    @ParameterizedTest(name = "[{index}]: {arguments}")
    @MethodSource(value = "io.qameta.allure.assertj.util.SoftAssertionsTestsProvider#softAssertionsTests")
    void fieldInjectionTests(String testName, Consumer<StandardSoftAssertionsProvider> test) {
        test.accept(softly);
        eraseCollectedErrors(softly);
    }

    @AllureFeatures.Steps
    @DisplayName("Check soft assertions obj injected as parameter by JUnit5 extension")
    @ParameterizedTest(name = "[{index}]: {arguments}")
    @MethodSource(value = "io.qameta.allure.assertj.util.SoftAssertionsTestsProvider#softAssertionsTests")
    void parameterInjectionTests(String testName, Consumer<StandardSoftAssertionsProvider> test, SoftAssertions softly) {
        test.accept(softly);
        eraseCollectedErrors(softly);
    }
}
