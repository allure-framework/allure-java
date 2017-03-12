package io.qameta.allure.testng.samples;

import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class SeverityMethodTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    public void testWithSeverity() throws Exception {
    }

    @Test
    public void testWithoutSeverity() throws Exception {
    }
}
