package io.qameta.allure.samples;

import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class MutedTestClassInherited extends MutedTestClass {

    @Override
    public void mutedTest() throws Exception {
        super.mutedTest();
    }

    @Override
    public void mutedAsWell() throws Exception {
        super.mutedAsWell();
    }

    @Test
    public void mutedInherited() throws Exception {
    }
}
