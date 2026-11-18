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
package io.qameta.allure.karate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.karatelabs.core.Runner;
import io.qameta.allure.model.Status;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import io.qameta.allure.testfilter.TestPlanV1_0;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@IsolatedLifecycle
class AllureKarateMutedTest {

    @TempDir
    private Path directory;

    @Test
    void shouldReadMutedPlanFromEnvironment() throws Exception {
        final Path plan = directory.resolve("plan.json");
        final Path results = directory.resolve("results");
        final Path log = directory.resolve("child.log");
        Files.writeString(
                plan, "{\"version\":\"1.0\",\"mutedTests\":[{\"id\":123},"
                        + "{\"selector\":\"testdata/muted-plan.feature:12\"}]}"
        );
        final ProcessBuilder builder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dallure.results.directory=" + results, "-cp",
                System.getProperty("allure.test.runtime.classpath"), MutedMain.class.getName(),
                directory.resolve("reports").toString()
        ).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("ALLURE_TESTPLAN_PATH", plan.toString());

        final Process process = builder.start();
        final boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        assertThat(finished).as(Files.readString(log)).isTrue();
        assertThat(process.exitValue()).as(Files.readString(log)).isZero();
        final List<Path> emitted;
        try (var files = Files.list(results)) {
            emitted = files.filter(path -> path.toString().endsWith("-result.json")).toList();
        }
        final ObjectMapper mapper = new ObjectMapper();
        for (Path result : emitted) {
            final var json = mapper.readTree(result.toFile());
            assertThat(json.path("status").asText()).isEqualTo("skipped");
            assertThat(json.path("statusDetails").path("muted").asBoolean()).isTrue();
        }
        assertThat(emitted).hasSize(3);
    }

    public static final class MutedMain {
        private MutedMain() {
        }

        public static void main(final String[] arguments) {
            final var results = Runner.builder().path("classpath:testdata/muted-plan.feature")
                    .listener(new AllureKarate()).outputDir(Path.of(arguments[0]))
                    .outputHtmlReport(false).outputJunitXml(false).outputCucumberJson(false).parallel(2);
            if (results.isFailed()) {
                throw new AssertionError(results.getErrors());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldSkipMutedScenarioBodies(final boolean allMuted) {
        final TestPlanV1_0 plan = new TestPlanV1_0().setMutedTests(
                allMuted
                        ? List.of(
                                new TestPlanV1_0.TestCase().setId("123"),
                                new TestPlanV1_0.TestCase().setId("unmatched")
                                        .setSelector("testdata/muted-plan.feature:12")
                        )
                        : List.of(new TestPlanV1_0.TestCase().setId("123"))
        );

        final AllureResults results = RunUtils.runTests(lifecycle -> {
            final var nativeResults = Runner.builder()
                    .path("classpath:testdata/muted-plan.feature")
                    .listener(new AllureKarate(lifecycle, plan))
                    .outputDir(directory)
                    .outputHtmlReport(false)
                    .outputJunitXml(false)
                    .outputCucumberJson(false)
                    .parallel(2);
            assertThat(nativeResults.getScenarioFailedCount()).isZero();
        });

        assertThat(results.getGlobals()).isEmpty();
        assertThat(results.getTestResults()).hasSize(3);
        assertThat(results.getTestResults()).filteredOn(result -> result.getName().equals("muted row"))
                .hasSize(2).allSatisfy(result -> {
                    assertThat(result.getStatus()).isEqualTo(Status.SKIPPED);
                    assertThat(result.getStatusDetails().isMuted()).isTrue();
                    assertThat(result.getParameters()).hasSize(1);
                });
        assertThat(results.getTestResults()).filteredOn(result -> result.getName().equals("control"))
                .singleElement().satisfies(result -> {
                    assertThat(result.getStatus()).isEqualTo(allMuted ? Status.SKIPPED : Status.PASSED);
                    if (allMuted) {
                        assertThat(result.getStatusDetails().isMuted()).isTrue();
                    }
                });
    }
}
