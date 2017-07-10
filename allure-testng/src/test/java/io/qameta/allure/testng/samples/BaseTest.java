package io.qameta.allure.testng.samples;

import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public abstract class BaseTest {

    @Test
    public void baseTest() throws Exception {
        check();
    }


    abstract void check();

}
