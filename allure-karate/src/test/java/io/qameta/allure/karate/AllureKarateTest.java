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
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.model.Status.BROKEN;
import static io.qameta.allure.model.Status.PASSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class AllureKarateTest extends TestRunner {

    @Test
    void shouldCreateNameAndFullName() {
        final AllureResults results = run("classpath:testdata/description-and-name.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getFullName)
                .containsExactlyInAnyOrder(
                        tuple(
                                "Some api* request # comment 1",
                                "testdata.description-and-name | Some api* request # comment 1"
                        ),
                        tuple(
                                "",
                                "testdata.description-and-name | "
                        )
                );
    }

    @Test
    void shouldCreateDescription() {
        final AllureResults results = run("classpath:testdata/description-and-name.feature");
        assertThat(results.getTestResults())
                .extracting(TestResult::getDescription, TestResult::getDescriptionHtml)
                .containsExactlyInAnyOrder(
                        tuple(
                                "Request '//user' & get 20* code, ...",
                                null
                        ),
                        tuple(
                                "",
                                null
                        )
                );
    }

    @Test
    void shouldCreateStartAndStopTimeslots() {
        final AllureResults results = runApi("classpath:testdata/api.feature");

        final TestResult tr1 = results.getTestResults().get(0);
        final TestResult tr2 = results.getTestResults().get(1);

        assertThat(tr2.getStop())
                .isGreaterThan(tr2.getStart())
                .isGreaterThan(tr1.getStop());
    }

    @Test
    void shouldCreateStatusAndStage() {
        final AllureResults results = run("classpath:testdata/api.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Simple post request")
                .extracting(TestResult::getStatus, TestResult::getStage)
                .containsExactlyInAnyOrder(
                        tuple(BROKEN, Stage.FINISHED)
                );
    }

    @Test
    void shouldNotCreateStatusDetailsIfTestPassed() {
        final AllureResults results = runApi("classpath:testdata/api.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Simple get request")
                .extracting(TestResult::getStatus, TestResult::getStatusDetails)
                .containsExactlyInAnyOrder(
                        tuple(PASSED, null)
                );
    }

    @Test
    void shouldCreateStatusDetailsIfTestFailed() {
        final AllureResults results = runApi("classpath:testdata/api.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Simple post request")
                .extracting(
                        TestResult::getStatus,
                        result -> result.getStatusDetails().getMessage().substring(0, 35),
                        result -> result.getStatusDetails().getTrace().substring(0, 70)
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                BROKEN,
                                "status code was: 401, expected: 200",
                                "com.intuit.karate.KarateException: status code was: 401, expected: 200"
                        )
                );
    }

    @Test
    void shouldCreateTestCaseIdAndName() {
        final AllureResults results = run("classpath:testdata/description-and-name.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getTestCaseId, TestResult::getTestCaseName)
                .containsExactlyInAnyOrder(
                        tuple("testdata.description-and-name_1", null),
                        tuple("testdata.description-and-name_2", null)
                );
    }

    @Test
    void shouldCreateTestCaseIdAndNamesOfParametrizedTest() {
        final AllureResults results = runApi("classpath:testdata/parametrized-test.feature");

        assertThat(results.getTestResults())
                .extracting(TestResult::getName, TestResult::getTestCaseId)
                .containsExactlyInAnyOrder(
                        tuple("/login should return 200", "testdata.parametrized-test_1_1"),
                        tuple("/user should return 301", "testdata.parametrized-test_1_2"),
                        tuple("/pages should return 404", "testdata.parametrized-test_1_3")
                );
    }

    @Test
    void shouldCreateParamsForParametrizedTest() {
        final AllureResults results = runApi("classpath:testdata/parametrized-test.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "/login should return 200")
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactlyInAnyOrder(
                        tuple("path", "login"),
                        tuple("status", "200")
        );
    }

    @Test
    void shouldCreateLabels() {
        final AllureResults results = run("classpath:testdata/tags.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test with labels")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "labels"),
                        tuple("epic", "epic1"),
                        tuple("story", "story1"),
                        tuple("tag", "some_tag")
                );
    }

    @Test
    void shouldCreateSpecialLabels() {
        final AllureResults results = run("classpath:testdata/tags.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test with owner, id and layer")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "labels"),
                        tuple("AS_ID", "141413"),
                        tuple("owner", "npolly"),
                        tuple("layer", "unit_tests"),
                        tuple("severity", "blocker")
                );
    }

    @Test
    void shouldNotCreateTagLabel() {
        final AllureResults results = run("classpath:testdata/tags.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test without allure labels")
                .flatExtracting(TestResult::getLabels)
                .extracting(Label::getName, Label::getValue)
                .containsExactly(
                        tuple("feature", "labels")
                );
    }

    @Test
    void shouldCreateLinks() {
        final AllureResults results = run("classpath:testdata/links.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "Test with links")
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName, Link::getType)
                .containsExactlyInAnyOrder(
                        tuple("http://localhost:8080", "custom"),
                        tuple("http://localhost:8080", "tms"),
                        tuple("http://localhost:8080", "issue")
                );
    }

    @Test
    void shouldCreateApiTestSteps() {
        final AllureResults results = runApi("classpath:testdata/steps.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s1")
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactlyInAnyOrder(
                        "print 'first feature:@smoke, first scenario'",
                        "url 'http://localhost:8081'",
                        "path '/login'",
                        "method get",
                        "status 200"
                );
    }

    @Test
    void shouldCreateResultWithEmptySteps() {
        final AllureResults results = runApi("classpath:testdata/steps.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s2")
                .flatExtracting(TestResult::getSteps)
                .isEmpty();
    }

    @Test
    void shouldCreateStepsStatuses() {
        final AllureResults results = run("classpath:testdata/steps.feature");

        assertThat(results.getTestResults())
                .filteredOn("name", "f1 - s1")
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getStatus)
                .containsExactly(
                        PASSED,
                        PASSED,
                        PASSED,
                        BROKEN
                );
    }

    @Test
    void shouldCreateAttachmentForFailedStep() {
        final AllureResults results = run("classpath:testdata/screenshot.feature");

        assertThat(results.getTestResults().get(0).getAttachments().get(0).getName()).contains("screenshot_");
    }

    @Test
    void shouldCreateAttachment() {
        final AllureResults results = run("classpath:testdata/demo-01.feature");

        assertThat(results.getTestResults().get(0).getAttachments().get(0).getName()).contains("demo-01_");
    }

    @Test
    void buildTest() {
        Runner.builder()
                .path("classpath:testdata/demo-01.feature")
                .hook(new AllureKarate())
                .backupReportDir(false)
                .outputJunitXml(false)
                .outputCucumberJson(false)
                .outputHtmlReport(false)
                .parallel(1);
    }
}
