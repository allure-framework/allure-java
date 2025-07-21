/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.diff;

import io.qameta.allure.Allure;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

public class AllureDiffTest {

    public static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(null, "example.json"),
                Arguments.of("example.json", null),
                Arguments.of("example.json", "example.json"),
                Arguments.of("example.json", "example-with-additional-line.json"),
                Arguments.of("example.json", "example-with-changed-one-line.json"),
                Arguments.of("example.json", "example-with-one-deleted-and-one-added.json"),
                Arguments.of("example.json", "example-without-street-line.json"),
                Arguments.of("example.xml", "example.xml"),
                Arguments.of("example.xml", "example-with-additional-line.xml"),
                Arguments.of("example.xml", "example-with-changed-one-line.xml"),
                Arguments.of("example.xml", "example-with-one-deleted-and-one-added.xml"),
                Arguments.of("example.xml", "example-without-street-line.xml")
        );
    }

    @DisplayName("Diff test between ")
    @ParameterizedTest(name = "{0} and {1}")
    @MethodSource("testData")
    void diffTest(String original, String revised) {
        final List<TestResult> testResult = runWithinTestContext(() -> Allure.step("Diff test", () -> {
            try {
                new AllureDiff().diff(read(original), read(revised));
            } catch (IOException e) {
                throw new RuntimeException("Test is broken, check test data", e);
            }
        })).getTestResults();

        assertThat(testResult)
                .describedAs("Result is exactly one TestResult")
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .describedAs("TestResult contains exactly one StepResult")
                .hasSize(1)
                .flatExtracting(StepResult::getAttachments)
                .describedAs("StepResult contains exactly one Attachment with diff")
                .hasSize(1);
    }

    private String read(String fileName) throws IOException {
        if (fileName == null) {
            return null;
        }
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (stream == null) throw new IOException("Resource not found: " + fileName);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
