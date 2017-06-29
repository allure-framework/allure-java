package io.qameta.allure.test;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class AllureResultsWriterStub implements AllureResultsWriter {

    private final List<TestResult> testResults = new CopyOnWriteArrayList<>();
    private final List<TestResultContainer> testContainers = new CopyOnWriteArrayList<>();

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
