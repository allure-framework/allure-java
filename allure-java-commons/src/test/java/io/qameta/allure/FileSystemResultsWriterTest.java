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

import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.internal.Allure2ModelJackson;
import io.qameta.allure.model.GlobalAttachment;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.Globals;
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
import java.util.Collections;
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
    void shouldWriteGlobals(@TempDir final Path folder) throws IOException {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final GlobalAttachment attachment = new GlobalAttachment()
                .setName("setup log")
                .setSource("setup-attachment.txt")
                .setType("text/plain")
                .setSize(12L)
                .setTimestamp(123L);
        final GlobalError error = new GlobalError()
                .setKnown(true)
                .setMuted(false)
                .setFlaky(true)
                .setMessage("setup failed")
                .setTrace("stack trace")
                .setActual("actual value")
                .setExpected("expected value")
                .setTimestamp(456L);
        final Globals globals = new Globals()
                .setAttachments(Collections.singletonList(attachment))
                .setErrors(Collections.singletonList(error));

        writer.write(globals);

        final List<Path> files = listFiles(folder);
        assertThat(files).hasSize(1);
        final Path globalsFile = files.get(0);
        assertThat(globalsFile.getFileName().toString())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-globals\\.json");

        final JsonNode payload = Allure2ModelJackson.createMapper().readTree(globalsFile.toFile());
        assertThat(payload.size()).isEqualTo(2);
        assertThat(payload.path("attachments").size()).isEqualTo(1);
        final JsonNode attachmentPayload = payload.path("attachments").path(0);
        assertThat(attachmentPayload.size()).isEqualTo(5);
        assertThat(attachmentPayload.path("name").textValue()).isEqualTo("setup log");
        assertThat(attachmentPayload.path("source").textValue()).isEqualTo("setup-attachment.txt");
        assertThat(attachmentPayload.path("type").textValue()).isEqualTo("text/plain");
        assertThat(attachmentPayload.path("size").longValue()).isEqualTo(12L);
        assertThat(attachmentPayload.path("timestamp").longValue()).isEqualTo(123L);

        assertThat(payload.path("errors").size()).isEqualTo(1);
        final JsonNode errorPayload = payload.path("errors").path(0);
        assertThat(errorPayload.size()).isEqualTo(8);
        assertThat(errorPayload.path("known").booleanValue()).isTrue();
        assertThat(errorPayload.path("muted").booleanValue()).isFalse();
        assertThat(errorPayload.path("flaky").booleanValue()).isTrue();
        assertThat(errorPayload.path("message").textValue()).isEqualTo("setup failed");
        assertThat(errorPayload.path("trace").textValue()).isEqualTo("stack trace");
        assertThat(errorPayload.path("actual").textValue()).isEqualTo("actual value");
        assertThat(errorPayload.path("expected").textValue()).isEqualTo("expected value");
        assertThat(errorPayload.path("timestamp").longValue()).isEqualTo(456L);

        final Globals written = Allure2ModelJackson.createMapper().readValue(globalsFile.toFile(), Globals.class);
        assertThat(written.getAttachments()).hasSize(1);
        final GlobalAttachment writtenAttachment = written.getAttachments().get(0);
        assertThat(writtenAttachment.getName()).isEqualTo("setup log");
        assertThat(writtenAttachment.getSource()).isEqualTo("setup-attachment.txt");
        assertThat(writtenAttachment.getType()).isEqualTo("text/plain");
        assertThat(writtenAttachment.getSize()).isEqualTo(12L);
        assertThat(writtenAttachment.getTimestamp()).isEqualTo(123L);

        assertThat(written.getErrors()).hasSize(1);
        final GlobalError writtenError = written.getErrors().get(0);
        assertThat(writtenError.isKnown()).isTrue();
        assertThat(writtenError.isMuted()).isFalse();
        assertThat(writtenError.isFlaky()).isTrue();
        assertThat(writtenError.getMessage()).isEqualTo("setup failed");
        assertThat(writtenError.getTrace()).isEqualTo("stack trace");
        assertThat(writtenError.getActual()).isEqualTo("actual value");
        assertThat(writtenError.getExpected()).isEqualTo("expected value");
        assertThat(writtenError.getTimestamp()).isEqualTo(456L);
    }

    @Test
    void shouldWriteEachGlobalsArtifactToDistinctFile(@TempDir final Path folder) throws IOException {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);

        writer.write(new Globals());
        writer.write(new Globals());

        assertThat(listFiles(folder))
                .hasSize(2)
                .extracting(path -> path.getFileName().toString())
                .doesNotHaveDuplicates()
                .allMatch(
                        fileName -> fileName
                                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-globals\\.json")
                );
    }

    @Test
    void shouldRejectNullGlobalsWithoutCreatingArtifact(@TempDir final Path folder) throws IOException {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);

        assertThatThrownBy(() -> writer.write((Globals) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("globals");

        assertThat(listFiles(folder)).isEmpty();
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
