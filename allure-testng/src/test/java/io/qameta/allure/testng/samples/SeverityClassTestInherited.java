package io.qameta.allure.testng.samples;

import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * @author charlie (Dmitry Baev).
 */
public class SeverityClassTestInherited extends SeverityClassTest {

    @Override
    public void testWithSeverity() throws Exception {
        super.testWithSeverity();
    }

    @Override
    @Severity(SeverityLevel.NORMAL)
    public void testWithClassSeverity() throws Exception {
        super.testWithClassSeverity();
    }
}
