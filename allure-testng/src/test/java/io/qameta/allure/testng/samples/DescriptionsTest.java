package io.qameta.allure.testng.samples;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class DescriptionsTest {

    /**
     * Before class description
     */
    @BeforeClass
    @Description(useJavaDoc = true)
    public void setUpClass() {

    }

    /**
     * Before method description
     */
    @BeforeMethod
    @Description(useJavaDoc = true)
    public void setUpMethod() {

    }

    /**
     * Sample test description
     */
    @Description(useJavaDoc = true)
    @Test
    public void test() {
        step();
    }

    /**
     * Sample step description
     */
    @Description(useJavaDoc = true)
    @Step("Step one")
    private void step() {
    }
}
