package io.qameta.allure.test;

import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.util.List;
import java.util.Map;

/**
 * @author charlie (Dmitry Baev).
 */
public interface AllureResults {

    List<TestResult> getTestResults();

    List<TestResultContainer> getTestResultContainers();

    Map<String, byte[]> getAttachments();

}
