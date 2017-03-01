package io.qameta.allure.cucumberjvm.testdata;

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

    @Override
    public void write(TestResult testResult) {
        testResults.add(testResult);
    }

    @Override
    public void write(TestResultContainer testResultContainer) {
        testContainers.add(testResultContainer);
    }

    @Override
    public void write(String source, InputStream attachment) {
        //not implemented
    }

    public List<TestResult> getTestResults() {
        return testResults;
    }

    public List<TestResultContainer> getTestContainers() {
        return testContainers;
    }
}
