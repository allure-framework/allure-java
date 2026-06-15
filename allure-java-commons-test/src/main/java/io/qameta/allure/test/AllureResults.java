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

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Integrates Allure Java test support with Allure reporting.
 *
 * <p>Register this type through the standard Allure Java test support extension, listener, interceptor, or plugin mechanism so framework execution events are written to Allure results. Use explicit dependencies when embedding the integration in tests or custom runtimes.</p>
 */
public interface AllureResults {

    /**
     * Returns the test results.
     *
     * @return the test results
     */
    List<TestResult> getTestResults();

    /**
     * Returns the test result containers.
     *
     * @return the test containers
     */
    List<TestResultContainer> getTestResultContainers();

    /**
     * Returns the attachments.
     *
     * @return the attachments
     */
    Map<String, byte[]> getAttachments();

    /**
     * Returns all attachment metadata from test results and nested steps.
     *
     * @return the attachment metadata
     */
    default List<Attachment> getAttachmentsRecursively() {
        return getTestResults().stream()
                .flatMap(AllureResults::getAttachmentsRecursively)
                .collect(Collectors.toList());
    }

    /**
     * Returns stored attachment content by attachment metadata.
     *
     * @param attachment the attachment metadata
     * @return the attachment content
     */
    default byte[] getAttachmentContent(final Attachment attachment) {
        final String source = attachment.getSource();
        final byte[] content = getAttachments().get(source);
        if (Objects.isNull(content)) {
            throw new NoSuchElementException("attachment content with source " + source + " is not found");
        }
        return Arrays.copyOf(content, content.length);
    }

    /**
     * Returns stored attachment content by attachment metadata as a string.
     *
     * @param attachment the attachment metadata
     * @param charset the content charset
     * @return the attachment content
     */
    default String getAttachmentContent(final Attachment attachment, final Charset charset) {
        return new String(getAttachmentContent(attachment), charset);
    }

    /**
     * Returns stored attachment content by attachment metadata as UTF-8 text.
     *
     * @param attachment the attachment metadata
     * @return the attachment content
     */
    default String getAttachmentContentAsString(final Attachment attachment) {
        return getAttachmentContent(attachment, StandardCharsets.UTF_8);
    }

    default TestResult getTestResultByName(final String name) {
        return getTestResults().stream()
                .filter(tr -> Objects.equals(name, tr.getName()))
                .findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "test result with name " + name + " is not found"
                        )
                );
    }

    default List<TestResultContainer> getTestResultContainersForTestResult(final TestResult testResult) {
        return getTestResultContainers().stream()
                .filter(c -> Objects.nonNull(c.getChildren()))
                .filter(c -> c.getChildren().contains(testResult.getUuid()))
                .collect(Collectors.toList());
    }

    private static Stream<Attachment> getAttachmentsRecursively(final ExecutableItem item) {
        return Stream.concat(
                item.getAttachments().stream(),
                item.getSteps().stream().flatMap(AllureResults::getAttachmentsRecursively)
        );
    }

}
