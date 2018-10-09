package io.qameta.allure;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static io.qameta.allure.model.DemoResults.randomTestResult;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.qatools.matchers.nio.PathMatchers.hasFilesCount;
import static ru.yandex.qatools.matchers.nio.PathMatchers.isDirectory;

/**
 * @author charlie (Dmitry Baev).
 */
public class FileSystemResultsWriterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldNotFailIfNoResultsDirectory() throws Exception {
        Path resolve = folder.newFolder().toPath().resolve("some-directory");
        FileSystemResultsWriter writer = new FileSystemResultsWriter(resolve);
        writer.write(randomTestResult());
    }

    @Test
    public void shouldWriteTestResult() throws Exception {
        Path results = folder.newFolder().toPath();
        FileSystemResultsWriter writer = new FileSystemResultsWriter(results);
        writer.write(randomTestResult());
        assertThat(results, isDirectory());
        assertThat(results, hasFilesCount(1, AllureConstants.TEST_RESULT_FILE_GLOB));
    }
}