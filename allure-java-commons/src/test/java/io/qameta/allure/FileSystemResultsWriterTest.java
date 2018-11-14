package io.qameta.allure;

import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.qameta.allure.FileSystemResultsWriter.generateTestResultName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
@ExtendWith(TempDirectory.class)
public class FileSystemResultsWriterTest {

    @Test
    void shouldNotFailIfNoResultsDirectory(@TempDir final Path folder) {
        Path resolve = folder.resolve("some-directory");
        FileSystemResultsWriter writer = new FileSystemResultsWriter(resolve);
        final TestResult testResult = random(TestResult.class, "steps");
        writer.write(testResult);
    }

    @Test
    void shouldWriteTestResult(@TempDir final Path folder) {
        FileSystemResultsWriter writer = new FileSystemResultsWriter(folder);
        final String uuid = UUID.randomUUID().toString();
        final TestResult testResult = random(TestResult.class, "steps").setUuid(uuid);
        writer.write(testResult);

        final String fileName = generateTestResultName(uuid);
        assertThat(folder)
                .isDirectory();

        assertThat(folder.resolve(fileName))
                .isRegularFile();
    }
}