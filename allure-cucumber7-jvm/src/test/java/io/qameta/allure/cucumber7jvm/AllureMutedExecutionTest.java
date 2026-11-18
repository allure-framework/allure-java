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
package io.qameta.allure.cucumber7jvm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AllureMutedExecutionTest {

    @TempDir
    private Path directory;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldSkipMutedBodiesUsingAutomaticBackend(final boolean allMuted) throws Exception {
        final Path feature = directory.resolve("muted.feature");
        Files.writeString(
                feature, "Feature: muted execution\n"
                        + "  @allure.id:123\n"
                        + "  Scenario Outline: muted row\n"
                        + "    Given a muted body must not execute\n"
                        + "    Examples:\n"
                        + "      | value |\n"
                        + "      | one   |\n"
                        + "      | two   |\n"
                        + (allMuted ? "" : "  Scenario: control\n    Given an active body executes\n")
        );
        final Path plan = directory.resolve("testplan.json");
        Files.writeString(
                plan, allMuted
                        ? "{\"version\":\"1.0\",\"mutedTests\":[{\"id\":\"unmatched\",\"selector\":\""
                                + feature.toUri().getSchemeSpecificPart() + ":7\"},{\"selector\":\""
                                + feature.toUri().getSchemeSpecificPart() + ":8\"}]}"
                        : "{\"version\":\"1.0\",\"mutedTests\":[{\"id\":123}]}"
        );
        final Path results = directory.resolve("results");
        final Path output = directory.resolve("output.txt");
        final ProcessBuilder builder = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dallure.results.directory=" + results,
                "-cp", System.getProperty("allure.test.runtime.classpath"),
                "io.cucumber.core.cli.Main",
                "--glue", "io.qameta.allure.cucumber7jvm.muted",
                "--plugin", "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm", feature.toString()
        ).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().put("ALLURE_TESTPLAN_PATH", plan.toString());
        builder.environment().put("ALLURE_RESULTS_DIRECTORY", results.toString());

        final Process process = builder.start();
        final boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        assertThat(finished).as(Files.readString(output)).isTrue();
        assertThat(process.exitValue()).as(Files.readString(output)).isZero();
        final ObjectMapper mapper = new ObjectMapper();
        final List<JsonNode> emitted;
        try (Stream<Path> paths = Files.list(results)) {
            emitted = paths.filter(path -> path.toString().endsWith("-result.json"))
                    .map(path -> {
                        try {
                            return mapper.readTree(path.toFile());
                        } catch (java.io.IOException error) {
                            throw new java.io.UncheckedIOException(error);
                        }
                    }).collect(Collectors.toList());
        }
        assertThat(emitted).hasSize(allMuted ? 2 : 3);
        assertThat(emitted).filteredOn(result -> result.path("name").asText().startsWith("muted row"))
                .hasSize(2).allSatisfy(result -> {
                    assertThat(result.path("status").asText()).isEqualTo("skipped");
                    assertThat(result.path("statusDetails").path("muted").asBoolean()).isTrue();
                });
        assertThat(emitted).filteredOn(result -> result.path("name").asText().equals("control"))
                .allSatisfy(result -> assertThat(result.path("status").asText()).isEqualTo("passed"));
    }
}
