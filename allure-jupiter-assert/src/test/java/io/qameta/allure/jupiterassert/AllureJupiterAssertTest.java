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
package io.qameta.allure.jupiterassert;

import io.qameta.allure.jupiterassert.fixture.MissingOptionalTypeFixture;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.aspectj.weaver.Dump;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AllureJupiterAssertTest {

    @Test
    void shouldNotInspectClassesWithMissingOptionalTypes() {
        final ClassLoader classLoader = MissingOptionalTypeFixture.class.getClassLoader();
        assertThatExceptionOfType(ClassNotFoundException.class)
                .isThrownBy(
                        () -> Class.forName(
                                MissingOptionalTypeFixture.MISSING_TYPE_NAME,
                                false,
                                classLoader
                        )
                );

        final String dumpBefore = Dump.getLastDumpFileName();

        final Class<?> fixtureType = MissingOptionalTypeFixture.loadClassWithMissingTypeSignature();
        final String dumpAfter = Dump.getLastDumpFileName();

        assertThatExceptionOfType(TypeNotPresentException.class)
                .isThrownBy(fixtureType::getGenericSuperclass)
                .withMessageContaining(MissingOptionalTypeFixture.MISSING_TYPE_NAME);
        assertThat(dumpAfter)
                .as("last AspectJ dump file")
                .isEqualTo(dumpBefore);
    }

    @Test
    void shouldHandleAssertEquals() {
        final AllureResults results = runWithinTestContext(
                () -> assertEquals("expectedString", "actualString"),
                AllureJupiterAssert::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assert 'expectedString' Equals 'actualString'");
    }

    @Test
    void shouldHandleAssertThrows() {
        final AllureResults results = runWithinTestContext(
                () -> assertThrows(IllegalStateException.class, () -> {
                    throw new IllegalStateException("expected");
                }),
                AllureJupiterAssert::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .singleElement()
                .satisfies(step -> {
                    assertThat(step.getName())
                            .startsWith("assert 'class java.lang.IllegalStateException' Throws '");
                    assertThat(step.getStatus())
                            .isEqualTo(Status.PASSED);
                });
    }
}
