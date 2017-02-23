package io.qameta.allure.samples;

import io.qameta.allure.Muted;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class MutedMethods {

    @Test
    @Muted
    public void mutedTest() throws Exception {
    }

    @Test
    public void notMuted() throws Exception {
    }
}
