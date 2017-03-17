package io.qameta.allure.testdata;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AllureResultsWriterStub implements AllureResultsWriter {

    private List<TestResult> testResults = new CopyOnWriteArrayList<>();
    private List<TestResultContainer> testContainers = new CopyOnWriteArrayList<>();

    public void write(final TestResult testResult) {
        testResults.add(testResult);
    }

    public void write(final TestResultContainer testResultContainer) {
        testContainers.add(testResultContainer);
    }

    public void write(final String source, final InputStream attachment) {
        //not implemented
    }

    public List<TestResult> getTestResults() {
        return testResults;
    }

    public List<TestResultContainer> getTestContainers() {
        return testContainers;
    }
}
