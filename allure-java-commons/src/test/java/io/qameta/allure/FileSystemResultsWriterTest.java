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
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.GlobalAttachment;
import io.qameta.allure.model.GlobalError;
import io.qameta.allure.model.Globals;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

    /**
     * Containers with both before and after fixtures must be written with their complete contents.
     */
    @Description
    @Test
    void shouldWriteTestResultContainer(@TempDir final Path folder) {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setChildren(Collections.singletonList("test-uuid"))
                .setBefores(Collections.singletonList(new FixtureResult().setName("setUp").setStatus(Status.PASSED)))
                .setAfters(Collections.singletonList(new FixtureResult().setName("tearDown").setStatus(Status.PASSED)));

        Allure.step("Write container with before and after fixtures", () -> writer.write(container));

        final String fileName = generateTestResultContainerName(uuid);
        assertThat(folder.resolve(fileName))
                .isRegularFile();
        final TestResultContainer written = readContainer(folder.resolve(fileName));
        assertThat(written).usingRecursiveComparison().isEqualTo(container);
    }

    /**
     * Containers without fixtures must not create result files or initialize the results directory,
     * even when they reference tests. Null fixture lists are treated as empty.
     */
    @Description
    @Issue("1483")
    @ParameterizedTest(name = "befores={0}, afters={1}")
    @MethodSource("emptyFixtureLists")
    void shouldNotWriteContainerWithoutFixtures(final List<FixtureResult> befores,
                                                final List<FixtureResult> afters,
                                                @TempDir final Path folder) {
        final Path outputDirectory = folder.resolve("allure-results");
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(outputDirectory);
        final TestResultContainer container = new TestResultContainer()
                .setUuid("empty-container")
                .setChildren(Collections.singletonList("test-uuid"))
                .setBefores(befores)
                .setAfters(afters);

        Allure.step("Write container without fixtures", () -> writer.write(container));

        assertThat(outputDirectory).doesNotExist();
    }

    /**
     * A container without fixtures or an explicit UUID must not leave files in an existing results directory.
     */
    @Description
    @Issue("1483")
    @Test
    void shouldNotWriteEmptyContainerWithGeneratedUuid(@TempDir final Path folder) throws IOException {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final TestResultContainer container = new TestResultContainer()
                .setChildren(Collections.singletonList("test-uuid"));

        Allure.step("Write empty container without a UUID", () -> writer.write(container));

        assertThat(listFiles(folder)).isEmpty();
    }

    /**
     * Before fixtures and their test associations must be persisted when no after fixtures exist.
     */
    @Description
    @Issue("1483")
    @ParameterizedTest(name = "afters={0}")
    @NullAndEmptySource
    void shouldWriteContainerWithOnlyBeforeFixtures(final List<FixtureResult> afters,
                                                    @TempDir final Path folder) {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final FixtureResult fixture = new FixtureResult().setName("setUp").setStatus(Status.PASSED);
        final TestResultContainer container = new TestResultContainer()
                .setUuid("before-container")
                .setChildren(Collections.singletonList("test-uuid"))
                .setBefores(Collections.singletonList(fixture))
                .setAfters(afters);

        Allure.step("Write container with only before fixtures", () -> writer.write(container));

        final TestResultContainer written = readContainer(folder.resolve("before-container-container.json"));
        assertThat(written.getUuid()).isEqualTo("before-container");
        assertThat(written.getChildren()).containsExactly("test-uuid");
        assertThat(written.getBefores()).singleElement().usingRecursiveComparison().isEqualTo(fixture);
        assertThat(written.getAfters()).isEmpty();
    }

    /**
     * After fixtures and their test associations must be persisted when no before fixtures exist.
     */
    @Description
    @Issue("1483")
    @ParameterizedTest(name = "befores={0}")
    @NullAndEmptySource
    void shouldWriteContainerWithOnlyAfterFixtures(final List<FixtureResult> befores,
                                                   @TempDir final Path folder) {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final FixtureResult fixture = new FixtureResult().setName("tearDown").setStatus(Status.PASSED);
        final TestResultContainer container = new TestResultContainer()
                .setUuid("after-container")
                .setChildren(Collections.singletonList("test-uuid"))
                .setBefores(befores)
                .setAfters(Collections.singletonList(fixture));

        Allure.step("Write container with only after fixtures", () -> writer.write(container));

        final TestResultContainer written = readContainer(folder.resolve("after-container-container.json"));
        assertThat(written.getUuid()).isEqualTo("after-container");
        assertThat(written.getChildren()).containsExactly("test-uuid");
        assertThat(written.getBefores()).isEmpty();
        assertThat(written.getAfters()).singleElement().usingRecursiveComparison().isEqualTo(fixture);
    }

    /**
     * Failed or broken fixtures must retain their diagnostics even when the container has no child tests.
     */
    @Description
    @Issue("1483")
    @ParameterizedTest
    @EnumSource(
            value = Status.class,
            names = {"FAILED", "BROKEN"}
    )
    void shouldWriteContainerWithUnsuccessfulFixtures(final Status status, @TempDir final Path folder) {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final FixtureResult before = new FixtureResult()
                .setName("setUpAll")
                .setStatus(status)
                .setStatusDetails(new StatusDetails().setMessage("setup failed").setTrace("setup trace"));
        final FixtureResult after = new FixtureResult()
                .setName("tearDownAll")
                .setStatus(status)
                .setStatusDetails(new StatusDetails().setMessage("cleanup failed").setTrace("cleanup trace"));
        final TestResultContainer container = new TestResultContainer()
                .setUuid("unsuccessful-container")
                .setBefores(Collections.singletonList(before))
                .setAfters(Collections.singletonList(after));

        Allure.step("Write unsuccessful fixtures without child tests", () -> writer.write(container));

        final TestResultContainer written = readContainer(folder.resolve("unsuccessful-container-container.json"));
        assertThat(written).usingRecursiveComparison().isEqualTo(container);
    }

    @ParameterizedTest
    @EnumSource(
            value = Status.class,
            names = {"FAILED", "BROKEN"}
    )
    void shouldWriteGlobals(final Status status, @TempDir final Path folder) throws IOException {
        final FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final GlobalAttachment attachment = new GlobalAttachment()
                .setName("setup log")
                .setSource("setup-attachment.txt")
                .setType("text/plain")
                .setSize(12L)
                .setTimestamp(123L);
        final GlobalError error = new GlobalError()
                .setStatus(status)
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
        assertThat(errorPayload.size()).isEqualTo(9);
        assertThat(errorPayload.path("status").textValue()).isEqualTo(status.value());
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
        assertThat(writtenError.getStatus()).isEqualTo(status);
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
    void shouldDefaultAbsentGlobalErrorStatusToBroken() throws IOException {
        final Globals globals = Allure2ModelJackson.createMapper().readValue(
                "{\"errors\":[{\"message\":\"run error\",\"timestamp\":123}]}", Globals.class
        );
        final GlobalError error = globals.getErrors().get(0);
        assertThat(error.getStatus()).isEqualTo(Status.BROKEN);
        assertThat(error.getMessage()).isEqualTo("run error");
        assertThat(error.getTimestamp()).isEqualTo(123L);
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

    private static Stream<Arguments> emptyFixtureLists() {
        return Stream.of(
                Arguments.of(Collections.emptyList(), Collections.emptyList()),
                Arguments.of(null, Collections.emptyList()),
                Arguments.of(Collections.emptyList(), null),
                Arguments.of(null, null)
        );
    }

    private static TestResultContainer readContainer(final Path file) {
        return Allure.step("Read written container JSON", () -> {
            final String payload = Files.readString(file);
            Allure.attachment(file.getFileName().toString(), "application/json", payload);
            return Allure2ModelJackson.createMapper().readValue(payload, TestResultContainer.class);
        });
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
