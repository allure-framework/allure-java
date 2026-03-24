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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author charlie (Dmitry Baev).
 */
public class FileSystemResultsWriter implements AllureResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemResultsWriter.class);

    private final Path outputDirectory;

    private final ObjectMapper mapper;

    private final boolean cleanBeforeRun;

    private final boolean cleanOnlyOnce;

    private final AtomicBoolean cleaned = new AtomicBoolean(false);

    public FileSystemResultsWriter(final Path outputDirectory) {
        this(outputDirectory, false, true);
    }

    public FileSystemResultsWriter(final Path outputDirectory,
                                    final boolean cleanBeforeRun,
                                    final boolean cleanOnlyOnce) {
        this.outputDirectory = outputDirectory;
        this.cleanBeforeRun = cleanBeforeRun;
        this.cleanOnlyOnce = cleanOnlyOnce;
        this.mapper = Allure2ModelJackson.createMapper();
    }

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

    @Override
    public void write(final String source, final InputStream attachment) {
        ensureInitialized();
        final Path file = outputDirectory.resolve(source);
        try (InputStream is = attachment) {
            Files.copy(is, file);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not write Allure attachment", e);
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
        try {
            if (!Files.exists(directory)) {
                return;
            }
            try (Stream<Path> stream = Files.list(directory)) {
                stream.forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            deleteDirectory(path);
                        } else {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete {} during directory cleanup", path, e);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to clean directory contents: {}", directory, e);
        }
    }

    private void deleteDirectory(final Path directory) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete {} during directory deletion", path, e);
                        }
                    });
        }
    }

    protected static String generateTestResultName() {
        return generateTestResultName(UUID.randomUUID().toString());
    }

    protected static String generateTestResultName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_FILE_SUFFIX;
    }

    protected static String generateTestResultContainerName() {
        return generateTestResultContainerName(UUID.randomUUID().toString());
    }

    protected static String generateTestResultContainerName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_CONTAINER_FILE_SUFFIX;
    }
}
