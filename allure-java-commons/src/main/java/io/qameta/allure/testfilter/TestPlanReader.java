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
package io.qameta.allure.testfilter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads Allure test plans without exposing the bundled JSON implementation.
 */
public class TestPlanReader {

    private static final String VERSION_FIELD = "version";
    private static final String VERSION_1_0 = "1.0";

    private final ObjectMapper objectMapper;

    /**
     * Creates a reader for supported Allure test-plan schema versions.
     */
    public TestPlanReader() {
        this(new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    TestPlanReader(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reads a test plan from a file.
     *
     * @param path the file to read.
     * @return parsed test plan.
     * @throws IOException if the file cannot be read or parsed.
     */
    public TestPlan read(final Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            return read(stream);
        }
    }

    /**
     * Reads a test plan from a stream.
     *
     * @param stream the stream to read.
     * @return parsed test plan.
     * @throws IOException if the stream cannot be parsed.
     */
    public TestPlan read(final InputStream stream) throws IOException {
        final JsonNode root = objectMapper.readTree(stream);
        if (root == null) {
            return new TestPlanUnknown();
        }

        final String version = Optional.ofNullable(root.get(VERSION_FIELD))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .orElse("");

        if (VERSION_1_0.equals(version)) {
            return objectMapper.treeToValue(root, TestPlanV1_0.class);
        }
        return objectMapper.treeToValue(root, TestPlanUnknown.class);
    }
}
