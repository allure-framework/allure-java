package io.qameta.allure.samples;

import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Severity(SeverityLevel.MINOR)
public class SeverityClassTest {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    public void testWithSeverity() throws Exception {
    }

    @Test
    public void testWithClassSeverity() throws Exception {
    }
}
