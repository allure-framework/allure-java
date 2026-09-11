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
package io.qameta.allure.test;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureConstants;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.internal.json.JsonSupport;
import io.qameta.allure.model.Parameter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Provides utility methods for Allure Java test support support.
 *
 * <p>The methods are stateless helpers intended for integrations, tests, and extension code that need the same conventions as the built-in Allure adapters.</p>
 */
public final class AllureTestCommonsUtils {

    private static final String DOT = ".";
    private static final String JSON_EXTENSION = "json";
    private static final String JSON_TYPE = "application/json";
    private static final String TEXT_EXTENSION = "txt";
    private static final String TEXT_TYPE = "text/plain";

    private AllureTestCommonsUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Attach {@link AllureResults} to the report.
     */
    public static void attach(final AllureResults allureResults) {
        allureResults.getTestResults().forEach(testResult -> {
            try {
                Allure.attachment(
                        testResult.getUuid() + AllureConstants.TEST_RESULT_FILE_SUFFIX,
                        JSON_TYPE,
                        JsonSupport.writeModel(testResult)
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        allureResults.getTestResultContainers().forEach(container -> {
            try {
                Allure.attachment(
                        container.getUuid() + AllureConstants.TEST_RESULT_CONTAINER_FILE_SUFFIX,
                        JSON_TYPE,
                        JsonSupport.writeModel(container)
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        allureResults.getGlobals().forEach(globals -> {
            try {
                Allure.attachment(
                        UUID.randomUUID() + AllureConstants.GLOBALS_FILE_SUFFIX,
                        JSON_TYPE,
                        JsonSupport.writeModel(globals)
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        allureResults.getAttachments().forEach(
                (fileName, body) -> Allure
                        .attachment(
                                fileName,
                                type(fileName),
                                new ByteArrayInputStream(body),
                                attachmentOptions(fileName)
                        )
        );
    }

    /**
     * Calculates the compatibility history id expected in adapter tests.
     *
     * @param testCaseId the test case id
     * @param parameters the final parameters
     * @return the expected history id
     */
    public static String expectedHistoryId(final String testCaseId, final List<Parameter> parameters) {
        final StringBuilder source = new StringBuilder(testCaseId);
        final Stream<Parameter> parameterStream = Objects.isNull(parameters) ? Stream.empty() : parameters.stream();
        parameterStream
                .filter(Objects::nonNull)
                .filter(parameter -> !Boolean.TRUE.equals(parameter.getExcluded()))
                .sorted(
                        Comparator.comparing((Parameter parameter) -> Objects.toString(parameter.getName(), ""))
                                .thenComparing(parameter -> Objects.toString(parameter.getValue(), ""))
                )
                .forEachOrdered(
                        parameter -> source
                                .append(Objects.toString(parameter.getName(), ""))
                                .append(Objects.toString(parameter.getValue(), ""))
                );
        return md5(source.toString());
    }

    private static AttachmentOptions attachmentOptions(final String fileName) {
        if (fileName.endsWith(DOT + JSON_EXTENSION) || fileName.endsWith(DOT + TEXT_EXTENSION)) {
            return AttachmentOptions.empty();
        }
        return AttachmentOptions.withFileExtension(extension(fileName));
    }

    private static String type(final String fileName) {
        if (fileName.endsWith(DOT + JSON_EXTENSION)) {
            return JSON_TYPE;
        }
        if (fileName.endsWith(DOT + TEXT_EXTENSION)) {
            return TEXT_TYPE;
        }
        return null;
    }

    private static String extension(final String fileName) {
        final int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(index + 1);
    }

}
