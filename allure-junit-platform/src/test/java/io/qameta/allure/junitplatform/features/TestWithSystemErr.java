package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestWithSystemErr {

    @Test
    void testWithSystemOut() {
        System.err.println("SYS ERR CONTENT");
    }
}
