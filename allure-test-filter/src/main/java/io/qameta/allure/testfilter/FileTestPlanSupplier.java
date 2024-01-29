/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link TestPlanSupplier} that reads test plan from file, specified
 * in {@code ALLURE_TESTPLAN_PATH} environment variable.
 *
 * @author charlie (Dmitry Baev).
 */
public class FileTestPlanSupplier implements TestPlanSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTestPlanSupplier.class);

    /**
     * The possible environment variable names.
     */
    private static final String[] ENV_TESTPLAN_PATH = {"ALLURE_TESTPLAN_PATH", "AS_TESTPLAN_PATH"};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Supply test plan.
     *
     * @return the test plan.
     */
    @Override
    public Optional<TestPlan> supply() {
        final Optional<String> testPlanPath = Arrays.stream(ENV_TESTPLAN_PATH)
                .map(System::getenv)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .findFirst();

        return testPlanPath
                .flatMap(this::tryGetPath)
                .filter(Files::exists)
                .filter(Files::isRegularFile)
                .flatMap(this::readTestPlan);
    }

    private Optional<TestPlan> readTestPlan(final Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return Optional.of(OBJECT_MAPPER.readValue(stream, TestPlan.class));
        } catch (IOException e) {
            LOGGER.warn("could not read test plan file {}", path, e);
            return Optional.empty();
        }
    }

    private Optional<Path> tryGetPath(final String path) {
        try {
            return Optional.of(Paths.get(path));
        } catch (Exception e) {
            LOGGER.warn("could not read test plan file {}: invalid path", path);
            return Optional.empty();
        }
    }

}
