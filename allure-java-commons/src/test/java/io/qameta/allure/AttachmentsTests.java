package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.testdata.AllureResultsWriterStub;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.qameta.allure.Allure.addStreamAttachmentAsync;
import static io.qameta.allure.Allure.setLifecycle;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author sskorol (Sergey Korol).
 */
public class AttachmentsTests {

    private static final List<CompletableFuture<InputStream>> STREAM_FUTURE = new CopyOnWriteArrayList<>();

    @Test
    public void shouldAttachAsync() throws Exception {
        final AllureResultsWriterStub results = spy(AllureResultsWriterStub.class);

        final ArgumentCaptor<String> sources = ArgumentCaptor.forClass(String.class);
        doNothing().when(results).write(sources.capture(), any(InputStream.class));

        final AllureLifecycle lifecycle = new AllureLifecycle(results);
        setLifecycle(lifecycle);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        STREAM_FUTURE.add(addStreamAttachmentAsync(
                "Async attachment 1", "video/mp4", getStreamWithTimeout(2)));
        STREAM_FUTURE.add(addStreamAttachmentAsync(
                "Async attachment 2", "text/plain", getStreamWithTimeout(1)));

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        allOf(STREAM_FUTURE.toArray(new CompletableFuture[0])).join();

        final List<Attachment> attachments = results
                .getTestResults()
                .stream()
                .flatMap(r -> r.getAttachments().stream())
                .collect(Collectors.toList());

        assertThat(attachments)
                .as("Attachments list")
                .hasSize(2);

        assertThat(sources.getAllValues())
                .as("Sources list")
                .hasSize(2);

        assertThat(attachments)
                .flatExtracting(attachment -> asList(attachment.getName(), attachment.getType(), attachment.getSource()))
                .as("Attachments content")
                .containsExactly(
                        "Async attachment 1", "video/mp4", sources.getAllValues().get(0),
                        "Async attachment 2", "text/plain", sources.getAllValues().get(1));
    }

    private Supplier<InputStream> getStreamWithTimeout(final long sec) throws InterruptedException {
        TimeUnit.SECONDS.sleep(sec);
        return () -> mock(InputStream.class);
    }
}
