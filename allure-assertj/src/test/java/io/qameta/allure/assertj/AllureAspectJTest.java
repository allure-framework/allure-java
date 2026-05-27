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
package io.qameta.allure.assertj;

import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
class AllureAspectJTest {

    @AllureFeatures.Steps
    @Test
    void shouldCreateSemanticChainForScalarAssert() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat("Data")
                    .hasSize(4);
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("assert \"Data\"", Status.PASSED));
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("has size 4");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .isEmpty();
    }

    @AllureFeatures.Steps
    @Test
    void shouldUseAssertDescriptionAsChainName() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat((Object) null)
                    .as("Nullable object")
                    .isNull();
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert Nullable object");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("described as \"Nullable object\"", "is null");
    }

    @AllureFeatures.Steps
    @Test
    void shouldRenderByteArraysWithoutPayload() {
        final String value = "some string";
        final AllureResults results = runWithinTestContext(() -> {
            assertThat(value.getBytes(StandardCharsets.UTF_8))
                    .as("Byte array object")
                    .isEqualTo(value.getBytes(StandardCharsets.UTF_8));
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert Byte array object");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("described as \"Byte array object\"", "is equal to <BINARY>");
    }

    @AllureFeatures.Steps
    @Test
    void shouldRenderCollectionsAsSubjectsAndExpectedValuesAsValues() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat(Arrays.asList("a", "b"))
                    .containsExactly("a", "b");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert [\"a\", \"b\"]");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("contains exactly [\"a\", \"b\"]");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .isEmpty();
    }

    @AllureFeatures.Steps
    @Test
    void shouldRenderSmallArraysAsValues() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat(new int[]{1, 2})
                    .containsExactly(1, 2);

            assertThat(new String[]{"alpha", "bravo"})
                    .containsExactly("alpha", "bravo");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert [1, 2]", "assert [\"alpha\", \"bravo\"]");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("contains exactly [1, 2]", "contains exactly [\"alpha\", \"bravo\"]");
    }

    @AllureFeatures.Steps
    @Test
    void shouldRenderTuplesAsValues() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat(
                    Arrays.asList(
                            tuple("first", Status.PASSED),
                            tuple("second", Status.FAILED)
                    )
            )
                    .containsExactly(
                            tuple("first", Status.PASSED),
                            tuple("second", Status.FAILED)
                    );
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert [(\"first\", PASSED), (\"second\", FAILED)]");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("contains exactly [(\"first\", PASSED), (\"second\", FAILED)]");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .isEmpty();
    }

    @AllureFeatures.Steps
    @Test
    void shouldRenderFieldOrPropertyValueAssertions() {
        final StatusDetails details = new StatusDetails()
                .setMessage("Make the test failed");

        final AllureResults results = runWithinTestContext(() -> {
            assertThat(details)
                    .hasFieldOrPropertyWithValue("message", "Make the test failed");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert StatusDetails");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("has field or property \"message\" with value \"Make the test failed\"");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .isEmpty();
    }

    @AllureFeatures.Steps
    @Test
    void shouldTruncateLongStepNamesAndAddOnlyTruncatedValuesAsParameters() {
        final String value = String.join("", Collections.nCopies(1200, "a"));

        final AllureResults results = runWithinTestContext(() -> {
            assertThat(value)
                    .isEqualTo(value);
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .singleElement()
                .asString()
                .hasSize(1000)
                .startsWith("assert \"")
                .endsWith("...");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .singleElement()
                .asString()
                .hasSize(1000)
                .startsWith("is equal to \"")
                .endsWith("...");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactly(tuple("expected", "\"" + value + "\""));
    }

    @AllureFeatures.Steps
    @Test
    void shouldCreateSeparateChainsForMultipleAssertThatCalls() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat("Data")
                    .hasSize(4);

            assertThat(42)
                    .isPositive()
                    .isEqualTo(42);

            assertThat(Arrays.asList("a", "b"))
                    .hasSize(2)
                    .contains("a");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("assert \"Data\"", Status.PASSED),
                        tuple("assert 42", Status.PASSED),
                        tuple("assert [\"a\", \"b\"]", Status.PASSED)
                );
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "has size 4",
                        "is positive",
                        "is equal to 42",
                        "has size 2",
                        "contains \"a\""
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldAttachOperationsToStoredAssertionInstances() {
        final String targetA = "alpha";
        final String targetB = "bravo";

        final AllureResults results = runWithinTestContext(() -> {
            final AbstractStringAssert<?> a = assertThat(targetA);
            final AbstractStringAssert<?> b = assertThat(targetB);

            a.isEqualTo("alpha");
            b.isEqualTo("bravo");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("assert \"alpha\"", Status.PASSED),
                        tuple("assert \"bravo\"", Status.PASSED)
                );
        assertThat(result.getSteps())
                .filteredOn("name", "assert \"alpha\"")
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("is equal to \"alpha\"");
        assertThat(result.getSteps())
                .filteredOn("name", "assert \"bravo\"")
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("is equal to \"bravo\"");
    }

    @AllureFeatures.Steps
    @Test
    void shouldAvoidVerboseModelToStringPayloads() {
        final TestResult model = new TestResult()
                .setUuid("uid")
                .setName("testPassed")
                .setFullName("other.PassingTest.testPassed");

        final AllureResults results = runWithinTestContext(() -> {
            assertThat(Collections.singletonList(model))
                    .hasSize(1)
                    .containsExactly(model);
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert 1 TestResult item");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("has size 1", "contains exactly [TestResult]");
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .noneMatch(name -> name.contains("fullName="))
                .noneMatch(name -> name.contains("other.PassingTest.testPassed"));
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .noneMatch(name -> name.contains("fullName="))
                .noneMatch(name -> name.contains("other.PassingTest.testPassed"));
    }

    @AllureFeatures.Steps
    @Test
    void shouldKeepNavigationInsideTheSameChain() {
        final TestResult model = new TestResult()
                .setFullName("my.company.Test.testOne");

        final AllureResults results = runWithinTestContext(() -> {
            assertThat(Collections.singletonList(model))
                    .extracting(TestResult::getFullName)
                    .containsExactly("my.company.Test.testOne");

            assertThat(Collections.singletonList("alpha"))
                    .first(InstanceOfAssertFactories.STRING)
                    .startsWith("al");

            assertThat(Collections.singletonList("bravo"))
                    .singleElement(InstanceOfAssertFactories.STRING)
                    .endsWith("vo");

            assertThat((Object) "charlie")
                    .asInstanceOf(InstanceOfAssertFactories.STRING)
                    .contains("har");

            assertThat(Collections.singletonList(Collections.singletonList("delta")))
                    .flatExtracting(value -> value)
                    .containsExactly("delta");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .hasSize(5);
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "extracts <lambda> -> [\"my.company.Test.testOne\"]",
                        "contains exactly [\"my.company.Test.testOne\"]",
                        "first element as InstanceOfAssertFactory -> \"alpha\"",
                        "starts with \"al\"",
                        "single element as InstanceOfAssertFactory -> \"bravo\"",
                        "ends with \"vo\"",
                        "as instance of InstanceOfAssertFactory -> \"charlie\"",
                        "contains \"har\"",
                        "flat extracts <lambda> -> [\"delta\"]",
                        "contains exactly [\"delta\"]"
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldRenderSerializedLambdaMethodReferences() {
        final TestResult model = new TestResult()
                .setFullName("my.company.Test.testOne");

        final AllureResults results = runWithinTestContext(() -> {
            assertThat(Collections.singletonList(model))
                    .extracting((Function<TestResult, String> & Serializable) TestResult::getFullName)
                    .containsExactly("my.company.Test.testOne");
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly(
                        "extracts TestResult::getFullName -> [\"my.company.Test.testOne\"]",
                        "contains exactly [\"my.company.Test.testOne\"]"
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldMarkTheFailedHardAssertionOperation() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat("Data")
                    .hasSize(5);
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("assert \"Data\"", Status.FAILED));
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("has size 5", Status.FAILED));
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .filteredOn("name", "has size 5")
                .extracting(step -> step.getStatusDetails().getMessage())
                .singleElement()
                .asString()
                .contains("size");
    }

    @AllureFeatures.Steps
    @Test
    void shouldMarkTheFailedSoftAssertionOperationBeforeAssertAll() {
        final AllureResults results = runWithinTestContext(() -> {
            final SoftAssertions soft = new SoftAssertions();
            soft.assertThat(25)
                    .as("Age")
                    .isEqualTo(26);
            soft.assertAll();
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("assert Age", Status.FAILED));
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        tuple("described as \"Age\"", Status.PASSED),
                        tuple("is equal to 26", Status.FAILED)
                );
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .filteredOn("name", "is equal to 26")
                .extracting(step -> step.getStatusDetails().getMessage())
                .singleElement()
                .asString()
                .contains("expected: 26");
    }

    @AllureFeatures.Steps
    @Test
    void shouldAttachNestedAssertionsUnderCallbackOperations() {
        final AllureResults results = runWithinTestContext(() -> {
            assertThat("alpha")
                    .satisfies(
                            value -> assertThat(value)
                                    .startsWith("al")
                                    .endsWith("ha")
                    );
        }, AllureAspectJ::setLifecycle);

        final TestResult result = assertOnlyOneResult(results);
        assertThat(result.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("assert \"alpha\"");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("satisfies <lambda>");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .filteredOn("name", "satisfies <lambda>")
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert \"alpha\"");
        assertThat(result.getSteps())
                .flatExtracting(StepResult::getSteps)
                .filteredOn("name", "satisfies <lambda>")
                .flatExtracting(StepResult::getSteps)
                .flatExtracting(StepResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("starts with \"al\"", "ends with \"ha\"");
    }

    private TestResult assertOnlyOneResult(final AllureResults results) {
        assertThat(results.getTestResults()).hasSize(1);
        return results.getTestResults().get(0);
    }
}
