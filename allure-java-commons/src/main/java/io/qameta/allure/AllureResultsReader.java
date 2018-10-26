package io.qameta.allure;

import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 *
 * @deprecated scheduled to remove in 3.0
 */
@Deprecated
public interface AllureResultsReader {

    Stream<TestResult> readTestResults();

    Stream<TestResultContainer> readTestResultsContainers();

    Stream<String> findAllAttachments();

    InputStream readAttachment(String source) throws IOException;

    List<ReadError> getErrors();

}
