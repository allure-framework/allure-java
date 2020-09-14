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
package io.qameta.allure.junit5assert;

import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author legionivo (Andrey Konovka).
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AllureJunit5AssertTest {

    @Test
    void shouldHandleAssertAll() {
        final String FIRST_NAME = "Jane";
        final String LAST_NAME = "Doe";

        Person person = new Person();
        person.setFirstName(FIRST_NAME);
        person.setLastName(LAST_NAME);

        final AllureResults results = runWithinTestContext(() -> assertAll("name",
                () -> assertEquals(FIRST_NAME,
                        person.getFirstName(),
                        "The first name is incorrect"
                ),
                () -> assertEquals(LAST_NAME,
                        person.getLastName(),
                        "The last name is incorrect"
                )
        ), AllureJunit5Assert::setLifecycle);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("assert All in  'name'");

        List<StepResult> childSteps = Objects.requireNonNull(testResult
                .getSteps()
                .stream()
                .findFirst()
                .orElse(null))
                .getSteps();

        assertThat(childSteps)
                .flatExtracting(StepResult::getName)
                .containsExactly("assert 'Jane' Equals 'Jane'",
                        "assert 'Doe' Equals 'Doe'");

    }

    @Test
    void shouldHandleAssertAllFailure() {
        final String FIRST_NAME = "Jane";
        final String LAST_NAME = "Doe";

        Person person = new Person();
        person.setFirstName(FIRST_NAME);
        person.setLastName("LAST_NAME");

        final AllureResults results = runWithinTestContext(() -> assertAll("name",
                () -> assertEquals(FIRST_NAME,
                        person.getFirstName(),
                        "The first name is incorrect"
                ),
                () -> assertEquals(LAST_NAME,
                        person.getLastName(),
                        "The last name is incorrect"
                )
        ), AllureJunit5Assert::setLifecycle);

        final TestResult testResult = results.getTestResults().get(0);

        assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getStatus)
                .containsExactly(Status.FAILED);

        List<StepResult> childSteps = Objects.requireNonNull(testResult.getSteps().stream().findFirst().orElse(null)).getSteps();
        assertThat(childSteps)
                .flatExtracting(StepResult::getName, StepResult::getStatus)
                .containsExactly("assert 'Jane' Equals 'Jane'", Status.PASSED,
                        "assert 'Doe' Equals 'LAST_NAME'", Status.FAILED);
    }

    @Test
    void shouldHandleAssertEquals() {
        final AllureResults results = runWithinTestContext(() -> assertEquals("expectedString", "actualString"),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert 'expectedString' Equals 'actualString'");
    }

    @Test
    void shouldHandleAssertArrayEquals() {
        final int[] ACTUAL = new int[]{2, 5, 7};
        final int[] EXPECTED = new int[]{2, 5, 7};
        final AllureResults results = runWithinTestContext(() -> assertArrayEquals(EXPECTED, ACTUAL),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert Array '[2, 5, 7]' Equals '[2, 5, 7]'");
    }

    @Test
    void shouldHandleAssertTrue() {
        final AllureResults results = runWithinTestContext(() -> assertTrue(true),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assertTrue 'true'");
    }

    @Test
    void shouldHandleAssertIterableEquals() {
        final List<Integer> FIRST = Arrays.asList(1, 2, 3);
        final List<Integer> SECOND = Arrays.asList(1, 2, 3);
        final AllureResults results = runWithinTestContext(() -> assertIterableEquals(FIRST, SECOND),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert Iterable '[1, 2, 3]' Equals '[1, 2, 3]'");
    }


    @Test
    void shouldHandleAssertNotNull() {
        String argument = "someArgument";
        final AllureResults results = runWithinTestContext(() -> assertNotNull(argument),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assertNotNull 'someArgument'");
    }

    @Test
    void shouldHandleAssertNull() {
        final AllureResults results = runWithinTestContext(() -> assertNull(null),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assertNull 'null'");
    }

    @Test
    void shouldHandleAssertLinesMatch() {
        List<String> expected = Arrays.asList("Java", "\\d+", "JUnit");
        List<String> actual = Arrays.asList("Java", "11", "JUnit");
        final AllureResults results = runWithinTestContext(() -> assertLinesMatch(expected, actual),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert Lines '[Java, \\d+, JUnit]' Match '[Java, 11, JUnit]'");
    }

    @Test
    void shouldHandleAssertNotEquals() {
        final AllureResults results = runWithinTestContext(() -> assertNotEquals(13, 0),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert Not '13' Equals '0'");
    }

    @Test
    void shouldHandleAssertNotSame() {
        final List<Integer> FIRST = Arrays.asList(1, 2, 3);
        final List<Integer> SECOND = Arrays.asList(1, 2, 5);
        final AllureResults results = runWithinTestContext(() -> assertNotSame(FIRST, SECOND),
                AllureJunit5Assert::setLifecycle);
        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert Not '[1, 2, 3]' Same '[1, 2, 5]'");
    }

    static private class Person {
        private String firstName;
        private String lastName;

        Person() {
        }

        String getFirstName() {
            return firstName;
        }

        String getLastName() {
            return lastName;
        }

        void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
