package io.qameta.allure.junit4.samples;

import org.junit.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class BrokenWithoutMessageTest {

    @Test
    public void brokenWithoutMessageTest() throws Exception {
        throw new RuntimeException();
    }
}
