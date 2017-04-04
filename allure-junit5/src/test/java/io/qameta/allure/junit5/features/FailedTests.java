package io.qameta.allure.junit5.features;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedTests {

    @Test
    void failedTest() {
        fail("Make the test failed");
    }
}
