package io.qameta.allure;

import io.qameta.allure.httpattachment.AllureHttpAttachmentBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vicdev on 13.05.17.
 */
public class HttpAttachmentBuilderTest {

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
                        .withQueryParams(map).withRequestBody(json).withResponseBody(json).withResponseHeaders(map);
        allureHttpAttachmentBuilder.build();
    }
}
