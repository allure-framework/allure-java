package io.qameta.allure.testng.samples;

/**
 * @author charlie (Dmitry Baev).
 */
public class SeverityMethodTestInherited extends SeverityMethodTest {

    @Override
    public void testWithSeverity() throws Exception {
        super.testWithSeverity();
    }

    @Override
    public void testWithoutSeverity() throws Exception {
        super.testWithoutSeverity();
    }
}
