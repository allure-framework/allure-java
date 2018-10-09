package io.qameta.allure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.model.Allure2ModelJackson;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.qameta.allure.AllureConstants.ATTACHMENT_FILE_GLOB;
import static io.qameta.allure.AllureConstants.TEST_RESULT_CONTAINER_FILE_GLOB;
import static io.qameta.allure.AllureConstants.TEST_RESULT_FILE_GLOB;
import static java.nio.file.Files.newDirectoryStream;

/**
 * @author charlie (Dmitry Baev).
 */
public class FileSystemResultsReader implements AllureResultsReader {

    private final Path resultsDirectory;

    private final ObjectMapper mapper;

    private final List<ReadError> errors = new ArrayList<>();

    public FileSystemResultsReader(final Path resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
        this.mapper = Allure2ModelJackson.createMapper();
    }

    @Override
    public Stream<TestResult> readTestResults() {
        return listFiles(resultsDirectory, TEST_RESULT_FILE_GLOB)
                .map(this::readTestResult)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Stream<TestResultContainer> readTestResultsContainers() {
        return listFiles(resultsDirectory, TEST_RESULT_CONTAINER_FILE_GLOB)
                .map(this::readTestResultContainer)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Stream<String> findAllAttachments() {
        return listFiles(resultsDirectory, ATTACHMENT_FILE_GLOB)
                .map(Path::getFileName)
                .map(Path::toString);
    }

    @Override
    public InputStream readAttachment(final String source) throws IOException {
        return Files.newInputStream(resultsDirectory.resolve(source));
    }

    @Override
    public List<ReadError> getErrors() {
        return errors;
    }

    private Optional<TestResult> readTestResult(final Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return Optional.ofNullable(mapper.readValue(is, TestResult.class));
        } catch (IOException e) {
            errors.add(new ReadError(e, "Could not read result file {}", file));
            return Optional.empty();
        }
    }

    private Optional<TestResultContainer> readTestResultContainer(final Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return Optional.ofNullable(mapper.readValue(is, TestResultContainer.class));
        } catch (IOException e) {
            errors.add(new ReadError(e, "Could not read result container file {}", file));
            return Optional.empty();
        }
    }

    private Stream<Path> listFiles(final Path directory, final String glob) {
        try (DirectoryStream<Path> directoryStream = newDirectoryStream(directory, glob)) {
            return StreamSupport.stream(directoryStream.spliterator(), false)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList())
                    .stream();
        } catch (IOException e) {
            errors.add(new ReadError(e, "Could not list files in directory {}", directory));
            return Stream.empty();
        }
    }
}
