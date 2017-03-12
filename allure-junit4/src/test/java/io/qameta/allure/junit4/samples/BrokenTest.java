package io.qameta.allure.junit4.samples;

import org.junit.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class BrokenTest {

    @Test
    public void brokenTest() throws Exception {
        throw new RuntimeException("Hello, everybody");
    }
}
