package io.qameta.allure.testng.samples;

import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FlakyTestClassInherited extends FlakyTestClass {

    @Override
    public void flakyTest() throws Exception {
        super.flakyTest();
    }

    @Override
    public void flakyAsWell() throws Exception {
        super.flakyAsWell();
    }

    @Test
    public void flakyInherited() throws Exception {
    }
}
