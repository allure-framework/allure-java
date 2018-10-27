package io.qameta.allure;

import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.InputStream;

/**
 * @author charlie (Dmitry Baev).
 * @since 1.0-BETA2
 */
public interface AllureResultsWriter {

    /**
     * Writes Allure test result bean.
     *
     * @param testResult the given bean to write.
     * @throws AllureResultsWriteException if some error occurs
     *                                     during operation.
     */
    void write(TestResult testResult);

    /**
     * Writes Allure test result container bean.
     *
     * @param testResultContainer the given bean to write.
     * @throws AllureResultsWriteException if some error occurs
     *                                     during operation.
     */
    void write(TestResultContainer testResultContainer);

    /**
     * Writes given attachment. Will close the given stream.
     *
     * @param source     the file name of the attachment. Make sure that file name
     *                   matches the following glob: <pre>*-attachment*</pre>. The right way
     *                   to generate attachment is generate UUID, determinate attachment
     *                   extension and then use it as <pre>{UUID}-attachment.{ext}</pre>
     * @param attachment the steam that contains attachment body.
     */
    void write(String source, InputStream attachment);

}
