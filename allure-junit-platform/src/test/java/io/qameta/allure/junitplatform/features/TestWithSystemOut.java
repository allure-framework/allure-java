package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestWithSystemOut {

    @Test
    void testWithSystemOut() {
        System.out.println("SYS OUT CONTENT");
    }
}
