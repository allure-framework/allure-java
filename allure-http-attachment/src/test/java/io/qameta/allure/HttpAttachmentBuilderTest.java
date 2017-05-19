package io.qameta.allure;

import io.qameta.allure.httpattachment.AllureHttpAttachmentBuilder;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.testdata.AllureResultsWriterStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by vicdev on 13.05.17.
 */
public class HttpAttachmentBuilderTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;
    private String uuid;

    @Before
    public void initLifecycle() {
        results = new AllureResultsWriterStub();
        lifecycle = new AllureLifecycle(results);
        Allure.setLifecycle(lifecycle);

        uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);
    }

    @Test
    public void shouldCreateDefaultAttachment() {
        AllureHttpAttachmentBuilder allureHttpAttachmentBuilder =
                new AllureHttpAttachmentBuilder("GET",
                        "https://localhost");
        allureHttpAttachmentBuilder.build();
    }


    @Test
    public void shouldCreateCustomAttachment() {
        AllureHttpAttachmentBuilder allureHttpAttachmentBuilder =
                new AllureHttpAttachmentBuilder("GET",
                        "https://localhost");
        allureHttpAttachmentBuilder.build("/templates/custom_report.ftl");
    }

    @Test
    public void shouldCreateAttachmentWithAllParams() {
        Map<String, String> map = new HashMap<>();
        map.put("blah", "blah");
        map.put("blahblah", "blahblah");
        String json = "{\"blah\": \"blahblah\"}";

        AllureHttpAttachmentBuilder allureHttpAttachmentBuilder =
                new AllureHttpAttachmentBuilder("GET",
                        "https://localhost")
                        .withRequestCookies(map)
                        .withRequestHeaders(map)
                        .withQueryParams(map).withRequestBody(json).withResponseBody(json)
                        .withResponseHeaders(map);
        allureHttpAttachmentBuilder.build();
    }

    @After
    public void finishLifecycle() {
        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);
    }
}
