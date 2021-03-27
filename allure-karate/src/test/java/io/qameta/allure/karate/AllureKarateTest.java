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
package io.qameta.allure.karate;

import com.intuit.karate.Runner;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureKarateTest {

    @Test
    void shouldCreateAllureResults() {
        final AllureResults results = run("classpath:testdata/first.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("f1 - s1", Status.PASSED),
                        tuple("f1 - s2", Status.PASSED)
                );

    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateSteps() {
        final AllureResults results = run("classpath:testdata/first.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s1")
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("print 'first feature:@smoke, first scenario'", Status.PASSED)
                );

    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateAttachments() {
        final AllureResults results = run("classpath:testdata/first.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s1")
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("print 'first feature:@smoke, first scenario'", Status.PASSED)
                );

    }

    @Test
    void testTest() {
        Runner.builder()
                .path("classpath:testdata/demo-01.feature")
                .hook(new AllureKarate())
                .backupReportDir(false)
                .outputJunitXml(false)
                .outputCucumberJson(false)
                .outputHtmlReport(false)
                .parallel(1);
    }

    AllureResults run(final String... path) {
        final AllureResultsWriterStub writerStub = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writerStub);
        final AllureKarate allureKarate = new AllureKarate(lifecycle);

        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);
            AttachmentsAspects.setLifecycle(lifecycle);

            Runner.builder()
                    .path(path)
                    .hook(allureKarate)
                    .backupReportDir(false)
                    .outputJunitXml(false)
                    .outputCucumberJson(false)
                    .outputHtmlReport(false)
                    .parallel(1);

            return writerStub;
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            StepsAspects.setLifecycle(defaultLifecycle);
            AttachmentsAspects.setLifecycle(defaultLifecycle);
        }
    }
}
