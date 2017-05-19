package io.qameta.allure.testdata;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AllureResultsWriterStub implements AllureResultsWriter {

    private List<TestResult> testResults = new CopyOnWriteArrayList<>();

    private List<Attachment> attachments = new CopyOnWriteArrayList<>();

    private List<TestResultContainer> testContainers = new CopyOnWriteArrayList<>();

    public void write(final TestResult testResult) {
        testResults.add(testResult);
    }

    public void write(final TestResultContainer testResultContainer) {
        testContainers.add(testResultContainer);
    }

    public void write(final String name, final InputStream attachment) {
        String result = null;
        try {
            result = IOUtils.toString(attachment, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        attachments.add(new Attachment().withName(name).withSource(result));
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public List<TestResult> getTestResults() {
        return testResults;
    }

    public List<TestResultContainer> getTestContainers() {
        return testContainers;
    }
}
