package io.qameta.allure.testng.samples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class VarargsParameters {

    @DataProvider
    public Object[][] getVarArgsData() {
        return new Object[][]{new Object[]{"a", "b", "c"}};
    }

    @Test(dataProvider = "getVarArgsData")
    public void doSmth(final String... args) {
    }

}
