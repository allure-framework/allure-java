package io.qameta.allure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.model.Allure2ModelJackson;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * @author charlie (Dmitry Baev).
 */
public class FileSystemResultsWriter implements AllureResultsWriter {

    private final Path outputDirectory;

    private final ObjectMapper mapper;

    public FileSystemResultsWriter(final Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.mapper = Allure2ModelJackson.createMapper();
    }

    @Override
    public void write(final TestResult testResult) {
        final String testResultName = Objects.isNull(testResult.getUuid())
                ? generateTestResultName()
                : generateTestResultName(testResult.getUuid());
        createDirectories(outputDirectory);
        final Path file = outputDirectory.resolve(testResultName);
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
            mapper.writeValue(os, testResult);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not write Allure test result", e);
        }
    }

    @Override
    public void write(final TestResultContainer testResultContainer) {
        final String testResultContainerName = Objects.isNull(testResultContainer.getUuid())
                ? generateTestResultContainerName()
                : generateTestResultContainerName(testResultContainer.getUuid());
        createDirectories(outputDirectory);
        final Path file = outputDirectory.resolve(testResultContainerName);
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
            mapper.writeValue(os, testResultContainer);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not write Allure test result container", e);
        }
    }

    @Override
    public void write(final String source, final InputStream attachment) {
        createDirectories(outputDirectory);
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

    private String generateTestResultName() {
        return generateTestResultName(UUID.randomUUID().toString());
    }

    private String generateTestResultName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_FILE_SUFFIX;
    }

    private String generateTestResultContainerName() {
        return generateTestResultContainerName(UUID.randomUUID().toString());
    }

    private String generateTestResultContainerName(final String uuid) {
        return uuid + AllureConstants.TEST_RESULT_CONTAINER_FILE_SUFFIX;
    }
}
