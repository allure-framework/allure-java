/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.attachment.testdata;

import io.qameta.allure.attachment.AttachmentContent;
import io.qameta.allure.attachment.DefaultAttachmentContent;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
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
                LocalDateTime.now(),
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
