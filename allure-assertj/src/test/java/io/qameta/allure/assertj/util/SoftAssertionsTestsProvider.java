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
package io.qameta.allure.assertj.util;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.assertj.AllureAspectJ;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import junit.framework.AssertionFailedError;
import org.assertj.core.api.StandardSoftAssertionsProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static io.qameta.allure.Allure.step;
import static io.qameta.allure.assertj.util.ReflectionUtils.staticMethodAsConsumer;
import static io.qameta.allure.assertj.util.StringsUtils.humanReadableMethodOrClassName;
import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Achitheus (Yury Yurchenko).
 */
@SuppressWarnings("unused")
public class SoftAssertionsTestsProvider {
    public static final Logger LOGGER = LoggerFactory.getLogger(SoftAssertionsTestsProvider.class);

    public static Stream<Arguments> softAssertionsTests() {
        return Arrays.stream(SoftAssertionsTestsProvider.class.getDeclaredMethods())
                .filter(method -> {
                    Class<?>[] paramTypeArray = method.getParameterTypes();
                    return !method.getName().contains("$")
                            && paramTypeArray.length == 1
                            && paramTypeArray[0] == StandardSoftAssertionsProvider.class;
                })
                .peek(method -> method.setAccessible(true))
                .map(method -> Arguments.of(humanReadableMethodOrClassName(method.getName()), staticMethodAsConsumer(method)));
    }

    private static void afterAssertionErrorCollectedCallbackShouldWork(StandardSoftAssertionsProvider softly) {
        final AllureResults results = runWithinTestContext(() -> {
            step("Allure.step()", () -> {
                annotationStep(() -> {});
                annotationStep(() -> {});
                annotationStep(() -> {
                    step("Allure.step()");
                    step("Allure.step()", () -> {
                        annotationStep(() -> {});
                        annotationStep(() -> softly.collectAssertionError(new AssertionFailedError("hellow")));
                    });
                });
                annotationStep(() -> {});
            });
        }, AllureAspectJ::setLifecycle, Allure::setLifecycle, StepsAspects::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(FAILED);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(PASSED, PASSED, FAILED, PASSED);
        assertThat(softly.assertionErrorsCollected()).hasSize(1);
    }

    private static void customMethodStepShouldBeFailed(StandardSoftAssertionsProvider softly) {
        final AllureResults results = runWithinTestContext(() -> {
            List<String> notebookList = List.of("Tests string 0", "Tests string 1");
            step("Allure.step()", () -> {
                softly.assertThatList(notebookList)
                        .hasSize(1_000_000)
                        .containsAnyOf("Passed", "Tests string 0");
            });
            step("Allure.step()", () -> {
                softly.assertThatList(notebookList)
                        .hasSize(2)
                        .containsExactly("failed", "nope", "its failed");
            });
        }, AllureAspectJ::setLifecycle, Allure::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(FAILED, FAILED);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(PASSED, FAILED, PASSED,
                        PASSED, PASSED, FAILED);
        assertThat(softly.assertionErrorsCollected()).hasSize(2);
    }

    private static void severalMiddleStepsShouldBeFailed(StandardSoftAssertionsProvider softly) {
        final AllureResults results = runWithinTestContext(() -> {
            softly.assertThat("Some test string")
                    .as("%s description passed", "awesome")
                    .containsAnyOf("passed", "blahblah", "me te")
                    .containsOnlyDigits()
                    .doesNotContainIgnoringCase("passed")
                    .startsWith("failed")
                    .containsPattern(".+ test .+")
                    .endsWith("failed");
        }, AllureAspectJ::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(PASSED, PASSED, PASSED, FAILED, PASSED, FAILED, PASSED, FAILED);
        assertThat(softly.assertionErrorsCollected()).hasSize(3);
    }

    @Step("@Step")
    public static void annotationStep(Runnable runnable) {
        runnable.run();
    }
}
