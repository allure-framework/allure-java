package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class BrokenTests {

    @Test
    void brokenTest() {
        throw new RuntimeException("Make the test broken");
    }
}
