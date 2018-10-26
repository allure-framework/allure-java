package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author charlie (Dmitry Baev).
 */
public class SkippedTests {

    @Test
    void skippedTest() {
        assumeTrue(false, "Make the test skipped");
    }
}
