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
import io.qameta.allure.model.TestResultContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.FileSystemResultsWriter.generateTestResultContainerName;
import static io.qameta.allure.FileSystemResultsWriter.generateTestResultName;
import static io.qameta.allure.test.ThreadLocalEnhancedRandom.current;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

public class FileSystemResultsWriterTest {

    @Test
    void shouldNotFailIfNoResultsDirectory(@TempDir final Path folder) {
        Path resolve = folder.resolve("some-directory");
        FileSystemResultsWriter writer = new FileSystemResultsWriter(resolve);
        final TestResult testResult = current().nextObject(TestResult.class, "steps");
        writeTestResult(writer, testResult);
    }

    @Test
    void shouldWriteTestResult(@TempDir final Path folder) {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = current().nextObject(TestResult.class, "steps").setUuid(uuid);
        writeTestResult(writer, testResult);

        final String fileName = generateTestResultName(uuid);
        assertThat(folder)
                .isDirectory();

        assertThat(folder.resolve(fileName))
                .isRegularFile();
    }

    @Test
    void shouldWriteTestResultContainer(@TempDir final Path folder) {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResultContainer container = current().nextObject(TestResultContainer.class).setUuid(uuid);

        writer.write(container);

        final String fileName = generateTestResultContainerName(uuid);
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

        writeTestResult(writer, testResult);

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

        writeTestResult(writer, testResult);

        final String payload = Files.readString(folder.resolve(generateTestResultName(uuid)));
        assertThat(payload)
                .contains("\"actual\":\"actual value\"")
                .contains("\"expected\":\"expected value\"");
    }

    @Test
    void shouldWriteAttachmentFile(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String source = "source-attachment.txt";
        final String content = "attachment body";

        writer.write(source, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        assertThat(Files.readString(folder.resolve(source)))
                .isEqualTo(content);
    }

    /**
     * Verifies that a supported atomic move publishes an attachment without using the fallback.
     *
     * @param folder the temporary results directory
     */
    @Test
    void shouldPublishAttachmentAtomicallyWhenSupported(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String source = "source-attachment.txt";
        final String content = "attachment body";
        final Path attachmentFile = folder.resolve(source);

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            Allure.step(
                    "Write attachment using an atomic move",
                    () -> writer.write(source, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
            );

            Allure.step("Verify the atomic move was used without the fallback", () -> {
                files.verify(
                        () -> Files.move(
                                any(Path.class),
                                eq(attachmentFile),
                                aryEq(
                                        new CopyOption[]{
                                                StandardCopyOption.REPLACE_EXISTING,
                                                StandardCopyOption.ATOMIC_MOVE
                                        }
                                )
                        )
                );
                files.verify(
                        () -> Files.move(
                                any(Path.class),
                                eq(attachmentFile),
                                aryEq(new CopyOption[]{StandardCopyOption.REPLACE_EXISTING})
                        ),
                        never()
                );
            });
        }

        assertThat(Files.readString(attachmentFile))
                .isEqualTo(content);
        assertThat(listFiles(folder))
                .containsExactly(attachmentFile);
    }

    /**
     * Verifies that an atomic-move failure still replaces an attachment with a complete file using a regular move.
     *
     * @param failureName the atomic-move failure scenario
     * @param failure the atomic-move failure
     * @param folder the temporary results directory
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("atomicMoveFallbackFailures")
    void shouldReplaceAttachmentUsingFallbackWhenAtomicMoveFails(
                                                                 final String failureName,
                                                                 final Exception failure,
                                                                 @TempDir final Path folder)
            throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String source = "source-attachment.txt";
        final String content = "attachment body";
        final Path attachmentFile = folder.resolve(source);
        Files.writeString(attachmentFile, "previous attachment body");

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(
                    () -> Files.move(
                            any(Path.class),
                            eq(attachmentFile),
                            aryEq(
                                    new CopyOption[]{
                                            StandardCopyOption.REPLACE_EXISTING,
                                            StandardCopyOption.ATOMIC_MOVE
                                    }
                            )
                    )
            )
                    .thenThrow(failure);

            Allure.step(
                    "Write attachment after the atomic move fails",
                    () -> writer.write(source, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
            );

            Allure.step("Verify the atomic move was attempted before the fallback", () -> {
                files.verify(
                        () -> Files.move(
                                any(Path.class),
                                eq(attachmentFile),
                                aryEq(
                                        new CopyOption[]{
                                                StandardCopyOption.REPLACE_EXISTING,
                                                StandardCopyOption.ATOMIC_MOVE
                                        }
                                )
                        )
                );
                files.verify(
                        () -> Files.move(
                                any(Path.class),
                                eq(attachmentFile),
                                aryEq(new CopyOption[]{StandardCopyOption.REPLACE_EXISTING})
                        )
                );
            });
        }

        assertThat(Files.readString(attachmentFile))
                .isEqualTo(content);
        assertThat(listFiles(folder))
                .containsExactly(attachmentFile);
    }

    @Test
    void shouldDeleteTemporaryFileWhenFallbackMoveFailsUnchecked(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String source = "source-attachment.txt";
        final String content = "attachment body";
        final Path attachmentFile = folder.resolve(source);

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(
                    () -> Files.move(
                            any(Path.class),
                            eq(attachmentFile),
                            aryEq(
                                    new CopyOption[]{
                                            StandardCopyOption.REPLACE_EXISTING,
                                            StandardCopyOption.ATOMIC_MOVE
                                    }
                            )
                    )
            )
                    .thenThrow(new UnsupportedOperationException("Atomic move is not supported"));
            files.when(
                    () -> Files.move(
                            any(Path.class),
                            eq(attachmentFile),
                            aryEq(new CopyOption[]{StandardCopyOption.REPLACE_EXISTING})
                    )
            )
                    .thenThrow(new IllegalStateException("Fallback move failed"));

            assertThatThrownBy(
                    () -> writer.write(
                            source,
                            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
                    )
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Fallback move failed");
        }

        assertThat(attachmentFile)
                .doesNotExist();
        assertThat(listFiles(folder))
                .isEmpty();
    }

    @Test
    void shouldNotCreateFinalAttachmentFileWhenStreamFails(@TempDir final Path folder) throws IOException {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String source = "broken-attachment.txt";
        final byte[] content = "partial attachment body".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> writer.write(source, new FailingInputStream(content)))
                .isInstanceOf(AllureResultsWriteException.class)
                .hasMessage("Could not write Allure attachment")
                .hasCauseInstanceOf(IOException.class);

        assertThat(folder.resolve(source))
                .doesNotExist();
        assertThat(listFiles(folder))
                .isEmpty();
    }

    @Test
    void shouldPreserveExistingResults(@TempDir final Path folder) throws IOException {
        Path existingFile = folder.resolve("existing-result.json");
        Files.writeString(existingFile, "{}");

        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = current().nextObject(TestResult.class, "steps").setUuid(uuid);
        writeTestResult(writer, testResult);

        assertThat(existingFile).exists();
        assertThat(folder.resolve(generateTestResultName(uuid))).exists();
    }

    private static void writeTestResult(final FileSystemResultsWriter writer, final TestResult testResult) {
        Allure.step("Write test result JSON", step -> {
            step.parameter("uuid", testResult.getUuid());
            writer.write(testResult);
        });
    }

    private static List<Path> listFiles(final Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.collect(Collectors.toList());
        }
    }

    private static Stream<Arguments> atomicMoveFallbackFailures() {
        return Stream.of(
                Arguments.of(
                        "Atomic move is not supported",
                        new AtomicMoveNotSupportedException("source", "target", "Not supported")
                ),
                Arguments.of(
                        "Atomic move option is not supported",
                        new UnsupportedOperationException("Atomic move is not supported")
                ),
                Arguments.of(
                        "Atomic move cannot replace an existing target",
                        new FileAlreadyExistsException("target")
                )
        );
    }

    private static final class FailingInputStream extends InputStream {

        private final byte[] content;

        private int index;

        private FailingInputStream(final byte[] content) {
            this.content = content;
        }

        @Override
        public int read() throws IOException {
            if (index < content.length) {
                return content[index++] & 0xff;
            }
            throw new IOException("Simulated attachment stream failure");
        }
    }

}
