package io.qameta.allure.testng.samples;

import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

/**
 * @author charlie (Dmitry Baev).
 */
public class RetryTest {

    @Test(retryAnalyzer = Retry.class)
    public void testWithRetry() throws Exception {
        throw new RuntimeException("Unexpected failure");
    }

    public static class Retry extends RetryAnalyzerCount {

        @Override
        public boolean retryMethod(ITestResult result) {
            boolean willRetry = !result.isSuccess();
            if (willRetry) {
                result.setAttribute("retry", true);
            }
            return willRetry;
        }
    }
}
