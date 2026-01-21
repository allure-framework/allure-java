package io.qameta.allure.testng.samples;

import io.qameta.allure.Allure;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DataProviderWithAttachment {

    @DataProvider
    public Object[][] dataProvider() {
        Allure.addAttachment("attachment", "attachment content");
        return new Object[][]{
            {"a"}
        };
    }

    @Test(dataProvider = "dataProvider")
    public void test(String s) {
    }
}
