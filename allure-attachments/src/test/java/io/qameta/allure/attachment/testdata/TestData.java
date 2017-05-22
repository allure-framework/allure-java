package io.qameta.allure.attachment.testdata;

import io.qameta.allure.attachment.AttachmentContent;
import io.qameta.allure.attachment.DefaultAttachmentContent;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author charlie (Dmitry Baev).
 */
public final class TestData {

    private TestData() {
        throw new IllegalStateException("Do not instance");
    }

    public static String randomString() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static HttpRequestAttachment randomHttpRequestAttachment() {
        return new HttpRequestAttachment(
                randomString(),
                randomString(),
                randomString(),
                randomString(),
                randomString(),
                randomMap(),
                randomMap()
        );
    }

    public static HttpResponseAttachment randomHttpResponseAttachment() {
        return new HttpResponseAttachment(
                randomString(),
                randomString(),
                randomString(),
                ThreadLocalRandom.current().nextInt(),
                randomMap(),
                randomMap()
        );
    }

    public static AttachmentContent randomAttachmentContent() {
        return new DefaultAttachmentContent(randomString(), randomString(), randomString());
    }

    public static Map<String, String> randomMap() {
        final Map<String, String> map = new HashMap<>();
        map.put(randomString(), randomString());
        map.put(randomString(), randomString());
        return map;
    }
}
