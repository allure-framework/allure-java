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
package io.qameta.allure;

import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static io.qameta.allure.FileSystemResultsWriter.generateTestResultName;
import static io.qameta.allure.test.ThreadLocalEnhancedRandom.current;
import static org.assertj.core.api.Assertions.assertThat;
public class FileSystemResultsWriterTest {

    @Test
    void shouldNotFailIfNoResultsDirectory(@TempDir final Path folder) {
        Path resolve = folder.resolve("some-directory");
        FileSystemResultsWriter writer = new FileSystemResultsWriter(resolve);
        final TestResult testResult = current().nextObject(TestResult.class, "steps");
        writer.write(testResult);
    }

    @Test
    void shouldWriteTestResult(@TempDir final Path folder) {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = current().nextObject(TestResult.class, "steps").setUuid(uuid);
        writer.write(testResult);

        final String fileName = generateTestResultName(uuid);
        assertThat(folder)
                .isDirectory();

        assertThat(folder.resolve(fileName))
                .isRegularFile();
    }

    @Test
    void shouldWriteTitlePath(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = new TestResult()
                .setUuid(uuid)
                .setTitlePath(Arrays.asList("parent", "child"));

        writer.write(testResult);

        assertThat(Files.readString(folder.resolve(generateTestResultName(uuid))))
                .contains("\"titlePath\"")
                .contains("\"parent\"")
                .contains("\"child\"");
    }

    @Test
    void shouldWriteStatusDetailsActualAndExpected(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = new TestResult()
                .setUuid(uuid)
                .setStatusDetails(
                        new StatusDetails()
                                .setActual("actual value")
                                .setExpected("expected value")
                );

        writer.write(testResult);

        final String payload = Files.readString(folder.resolve(generateTestResultName(uuid)));
        assertThat(payload)
                .contains("\"actual\":\"actual value\"")
                .contains("\"expected\":\"expected value\"");
    }

    @Test
    void shouldPreserveOldResultsWhenCleanIsDisabled(@TempDir final Path folder) throws IOException {
        Path existingFile = folder.resolve("existing-result.json");
        Files.writeString(existingFile, "{}");

        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder, false, true);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = current().nextObject(TestResult.class, "steps").setUuid(uuid);
        writer.write(testResult);

        assertThat(existingFile).exists();
        assertThat(folder.resolve(generateTestResultName(uuid))).exists();
    }

    @Test
    void shouldCleanDirectoryWhenCleanBeforeRunEnabled(@TempDir final Path folder) throws IOException {
        Path existingFile = folder.resolve("existing-result.json");
        Files.writeString(existingFile, "{}");

        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder, true, true);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = current().nextObject(TestResult.class, "steps").setUuid(uuid);
        writer.write(testResult);

        assertThat(existingFile).doesNotExist();
        assertThat(folder.resolve(generateTestResultName(uuid))).exists();
    }

    @Test
    void shouldCleanOnlyOnceWhenCleanOnlyOnceEnabled(@TempDir final Path folder) throws IOException {
        Path existingFile = folder.resolve("existing-result.json");
        Files.writeString(existingFile, "{}");

        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder, true, true);

        final String uuid1 = UUID.randomUUID().toString();
        final TestResult testResult1 = current().nextObject(TestResult.class, "steps").setUuid(uuid1);
        writer.write(testResult1);

        final String uuid2 = UUID.randomUUID().toString();
        final TestResult testResult2 = current().nextObject(TestResult.class, "steps").setUuid(uuid2);
        writer.write(testResult2);

        assertThat(folder.resolve(generateTestResultName(uuid1))).exists();
        assertThat(folder.resolve(generateTestResultName(uuid2))).exists();
    }

    @Test
    void shouldCleanOnEveryFirstWriteWhenCleanOnlyOnceDisabled(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer1 = new FileSystemResultsWriter(folder, true, false);
        final String uuid1 = UUID.randomUUID().toString();
        final TestResult testResult1 = current().nextObject(TestResult.class, "steps").setUuid(uuid1);
        writer1.write(testResult1);

        Path intermediateFile = folder.resolve("intermediate-result.json");
        Files.writeString(intermediateFile, "{}");

        FileSystemResultsWriter writer2 = new FileSystemResultsWriter(folder, true, false);
        final String uuid2 = UUID.randomUUID().toString();
        final TestResult testResult2 = current().nextObject(TestResult.class, "steps").setUuid(uuid2);
        writer2.write(testResult2);

        assertThat(intermediateFile).doesNotExist();
        assertThat(folder.resolve(generateTestResultName(uuid2))).exists();
    }

    @Test
    void shouldNotDeleteDirectoryItself(@TempDir final Path folder) throws IOException {
        Path existingFile = folder.resolve("existing-result.json");
        Files.writeString(existingFile, "{}");

        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder, true, true);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = current().nextObject(TestResult.class, "steps").setUuid(uuid);
        writer.write(testResult);

        assertThat(folder).isDirectory();
        assertThat(folder.resolve(generateTestResultName(uuid))).exists();
    }
}
