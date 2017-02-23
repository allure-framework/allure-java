package io.qameta.allure.samples;

/**
 * @author charlie (Dmitry Baev).
 */
public class FlakyMethodsInherited extends FlakyMethods {

    @Override
    public void flakyTest() throws Exception {
        super.flakyTest();
    }
}
