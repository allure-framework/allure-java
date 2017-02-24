package io.qameta.allure;

import org.junit.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class ResultsUtilsTest {

    @Test(expected = IllegalStateException.class)
    public void shouldNotBeAbleToInstanceUtils() throws Exception {
        new ResultsUtils();
    }
}