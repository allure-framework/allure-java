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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.internal.Allure2ModelJackson;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Writes Allure result model objects and attachments to the file system.
 *
 * <p>Create a writer with an output directory and pass it to an {@link AllureLifecycle} when results should be persisted as standard Allure files. The writer creates directories as needed and names files consistently with the Allure format.</p>
 */
public class FileSystemResultsWriter implements AllureResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemResultsWriter.class);

    private static final int ATTACHMENT_COPY_BUFFER_SIZE = 1024 * 1024;

    private final Path outputDirectory;

    private final ObjectMapper mapper;

    private final boolean cleanBeforeRun;

    private final boolean cleanOnlyOnce;

    private final AtomicBoolean cleaned = new AtomicBoolean(false);

    /**
     * Creates a file system results writer with the supplied values.
     *
     * @param outputDirectory the output directory
     */
    public FileSystemResultsWriter(final Path outputDirectory) {
        this(outputDirectory, false, true);
    }

    /**
     * Creates a file system results writer with the supplied values.
     *
     * @param outputDirectory the output directory
     * @param cleanBeforeRun the clean before run
     * @param cleanOnlyOnce the clean only once
     */
    public FileSystemResultsWriter(final Path outputDirectory,
                                   final boolean cleanBeforeRun,
                                   final boolean cleanOnlyOnce) {
        this.outputDirectory = outputDirectory;
        this.cleanBeforeRun = cleanBeforeRun;
        this.cleanOnlyOnce = cleanOnlyOnce;
        this.mapper = Allure2ModelJackson.createMapper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final TestResult testResult) {
        final String testResultName = Objects.isNull(testResult.getUuid())
                ? generateTestResultName()
                : generateTestResultName(testResult.getUuid());
        ensureInitialized();
        final Path file = outputDirectory.resolve(testResultName);
        try {
            mapper.writeValue(file.toFile(), testResult);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not write Allure test result", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final TestResultContainer testResultContainer) {
        final String testResultContainerName = Objects.isNull(testResultContainer.getUuid())
                ? generateTestResultContainerName()
                : generateTestResultContainerName(testResultContainer.getUuid());
        ensureInitialized();
        final Path file = outputDirectory.resolve(testResultContainerName);
        try {
            mapper.writeValue(file.toFile(), testResultContainer);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not write Allure test result container", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final String source, final InputStream attachment) {
        ensureInitialized();
        final Path file = outputDirectory.resolve(source);
        Path tempFile = null;
        try (InputStream is = attachment) {
            tempFile = Files.createTempFile(outputDirectory, ".allure-write-", ".tmp");
            try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
                copy(is, channel);
                sync(channel);
            }
            Files.move(tempFile, file);
            tempFile = null;
        } catch (IOException e) {
            deleteIfExists(tempFile);
            throw new AllureResultsWriteException("Could not write Allure attachment", e);
        }
    }

    private static void copy(final InputStream source, final FileChannel target) throws IOException {
        final byte[] buffer = new byte[ATTACHMENT_COPY_BUFFER_SIZE];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        int length;
        while ((length = source.read(buffer)) != -1) {
            byteBuffer.clear();
            byteBuffer.limit(length);
            while (byteBuffer.hasRemaining()) {
                target.write(byteBuffer);
            }
        }
    }

    void sync(final FileChannel channel) throws IOException {
        channel.force(true);
    }

    private static void deleteIfExists(final Path file) {
        if (Objects.isNull(file)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Failed to delete temporary Allure attachment file {}", file, e);
        }
    }

    private void createDirectories(final Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not create Allure results directory", e);
        }
    }

    private void ensureInitialized() {
        createDirectories(outputDirectory);
        if (cleanBeforeRun) {
            final boolean shouldClean = !cleanOnlyOnce || cleaned.compareAndSet(false, true);
            if (shouldClean) {
                cleanDirectoryContents(outputDirectory);
            }
        }
    }

    private void cleanDirectoryContents(final Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete {} during directory cleanup", path, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to clean directory contents: {}", directory, e);
        }
    }

    /**
     * Generates and returns the test result name.
     *
     * @return the generated test result name
     */
    protected static String generateTestResultName() {
        return generateTestResultName(UUID.randomUUID().toString());
    }

    /**
     * Generates and returns the test result name.
     *
     * @param uuid the Allure UUID of the model object
     * @return the generated test result name
     */
    protected static String generateTestResultName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_FILE_SUFFIX;
    }

    /**
     * Generates and returns the test result container name.
     *
     * @return the generated test result container name
     */
    protected static String generateTestResultContainerName() {
        return generateTestResultContainerName(UUID.randomUUID().toString());
    }

    /**
     * Generates and returns the test result container name.
     *
     * @param uuid the Allure UUID of the model object
     * @return the generated test result container name
     */
    protected static String generateTestResultContainerName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_CONTAINER_FILE_SUFFIX;
    }
}
