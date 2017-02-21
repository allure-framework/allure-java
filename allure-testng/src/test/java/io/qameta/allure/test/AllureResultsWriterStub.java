package io.qameta.allure.test;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class AllureResultsWriterStub implements AllureResultsWriter {

    private List<TestResult> testResults = new ArrayList<>();
    private List<TestResultContainer> testContainers = new ArrayList<>();

    public void write(TestResult testResult) {
        testResults.add(testResult);
    }

    public void write(TestResultContainer testResultContainer) {
        testContainers.add(testResultContainer);
    }

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
