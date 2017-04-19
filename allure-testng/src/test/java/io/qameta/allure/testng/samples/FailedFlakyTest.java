package io.qameta.allure.testng.samples;

import io.qameta.allure.Flaky;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedFlakyTest {

    @Test
    @Flaky
    public void flakyWithFailure() throws Exception {
        assertThat(true).isFalse();
    }
}
