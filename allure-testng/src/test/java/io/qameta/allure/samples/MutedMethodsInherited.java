package io.qameta.allure.samples;

/**
 * @author charlie (Dmitry Baev).
 */
public class MutedMethodsInherited extends MutedMethods {

    @Override
    public void mutedTest() throws Exception {
        super.mutedTest();
    }
}
