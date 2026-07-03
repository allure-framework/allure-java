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
package io.qameta.allure.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WellKnownFileExtensionsUtilsTest {

    @Test
    void shouldResolveWellKnownFileExtensionsByContentType() {
        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put("application/json", ".json");
        expected.put("text/plain; charset=UTF-8", ".txt");
        expected.put("APPLICATION/VND.ALLURE.HTTP+JSON", ".httpexchange");
        expected.put("application/vnd.allure.http", ".httpexchange");
        expected.put("application/vnd.allure.metadata", ".metadata");
        expected.put("application/vnd.allure.metadata+json", ".metadata");
        expected.put("application/vnd.allure.image.diff", ".imagediff");
        expected.put("application/vnd.allure.image.diff+json", ".imagediff");
        expected.put("application/dita+xml; format=map", ".ditamap");
        expected.put("application/gzip", ".gz");
        expected.put("audio/mpeg", ".mpga");
        expected.put("text/uri-list", ".uri");
        expected.put("application/x-unknown", "");

        final Map<String, String> actual = new LinkedHashMap<>();
        expected.keySet()
                .forEach(type -> actual.put(type, WellKnownFileExtensionsUtils.getExtensionByMimeType(type)));

        assertThat(actual)
                .containsExactlyEntriesOf(expected);
    }

}
