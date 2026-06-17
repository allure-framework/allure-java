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

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Writes Allure result model objects and attachments to the file system.
 *
 * <p>Create a writer with an output directory and pass it to an {@link AllureLifecycle} when results should be persisted as standard Allure files. The writer creates directories as needed and names files consistently with the Allure format.</p>
 */
public class FileSystemResultsWriter implements AllureResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemResultsWriter.class);

    private static final String TEST_RESULT_ENTITY_NAME = "test result";

    private static final String TEST_RESULT_CONTAINER_ENTITY_NAME = "test result container";

    private static final String ATTACHMENT_ENTITY_NAME = "attachment";

    private final Path outputDirectory;

    private final ObjectMapper mapper;

    /**
     * Creates a file system results writer with the supplied values.
     *
     * @param outputDirectory the output directory
     */
    public FileSystemResultsWriter(final Path outputDirectory) {
        this.outputDirectory = outputDirectory;
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
        final Path file = outputDirectory.resolve(testResultName);
        write(file, TEST_RESULT_ENTITY_NAME, channel -> {
            final DataOutput output = new DataOutputStream(Channels.newOutputStream(channel));
            mapper.writeValue(output, testResult);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final TestResultContainer testResultContainer) {
        final String testResultContainerName = Objects.isNull(testResultContainer.getUuid())
                ? generateTestResultContainerName()
                : generateTestResultContainerName(testResultContainer.getUuid());
        final Path file = outputDirectory.resolve(testResultContainerName);
        write(file, TEST_RESULT_CONTAINER_ENTITY_NAME, channel -> {
            final DataOutput output = new DataOutputStream(Channels.newOutputStream(channel));
            mapper.writeValue(output, testResultContainer);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final String source, final InputStream attachment) {
        final Path file = outputDirectory.resolve(source);
        try (InputStream is = attachment) {
            write(file, ATTACHMENT_ENTITY_NAME, channel -> {
                final OutputStream output = Channels.newOutputStream(channel);
                is.transferTo(output);
            });
        } catch (IOException e) {
            throw new AllureResultsWriteException(getErrorMessage(ATTACHMENT_ENTITY_NAME), e);
        }
    }

    private void write(final Path file, final String entityName, final ChannelConsumer consumer) {
        ensureInitialized();
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(outputDirectory, ".allure-write-", ".tmp");
            try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
                consumer.accept(channel);
                channel.force(true);
            }
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            tempFile = null;
        } catch (IOException e) {
            deleteIfExists(tempFile);
            throw new AllureResultsWriteException(getErrorMessage(entityName), e);
        }
    }

    private static String getErrorMessage(final String entityName) {
        return "Could not write Allure " + entityName;
    }

    private interface ChannelConsumer {

        void accept(FileChannel channel) throws IOException;
    }

    private static void deleteIfExists(final Path file) {
        if (Objects.isNull(file)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Failed to delete temporary Allure result file {}", file, e);
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
