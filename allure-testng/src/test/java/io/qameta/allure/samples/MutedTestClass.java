package io.qameta.allure.samples;

import io.qameta.allure.Muted;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Muted
public class MutedTestClass {

    @Test
    @Muted
    public void mutedTest() throws Exception {
    }

    @Test
    public void mutedAsWell() throws Exception {
    }
}
