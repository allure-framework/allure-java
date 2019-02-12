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
package io.qameta.allure.test;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class AllureResultsWriterStub implements AllureResultsWriter, AllureResults {

    private final List<TestResult> testResults = new CopyOnWriteArrayList<>();
    private final List<TestResultContainer> testContainers = new CopyOnWriteArrayList<>();
    private final Map<String, byte[]> attachments = new ConcurrentHashMap<>();

    @Override
    public void write(final TestResult testResult) {
        testResults.add(testResult);
    }

    @Override
    public void write(final TestResultContainer testResultContainer) {
        testContainers.add(testResultContainer);
    }

    @Override
    public void write(final String source, final InputStream attachment) {
        try {
            final byte[] bytes = IOUtils.toByteArray(attachment);
            attachments.put(source, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Could not read attachment content " + source, e);
        }
    }

    @Override
    public List<TestResult> getTestResults() {
        return testResults;
    }

    @Override
    public List<TestResultContainer> getTestResultContainers() {
        return testContainers;
    }

    @Override
    public Map<String, byte[]> getAttachments() {
        return attachments;
    }
}
