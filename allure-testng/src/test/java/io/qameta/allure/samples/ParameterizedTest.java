package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class ParameterizedTest {

    @BeforeMethod
    public void beforeMethod() {

    }

    @DataProvider
    public Object[][] testData() {
        return new String[][]{
                {"param1"},
                {"param2"}
        };
    }

    @Test(dataProvider = "testData")
    public void parameterizedTest(String param) {
        step(param);
    }

    @Step
    public void step(String param) {

    }
}
