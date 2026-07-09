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
package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TestWithPublishedFile {

    @Test
    void testWithPublishedFile(final TestReporter testReporter) {
        testReporter.publishFile(
                "published-file.txt",
                MediaType.TEXT_PLAIN,
                path -> Files.write(path, "PUBLISHED FILE CONTENT".getBytes(StandardCharsets.UTF_8))
        );
    }
}
