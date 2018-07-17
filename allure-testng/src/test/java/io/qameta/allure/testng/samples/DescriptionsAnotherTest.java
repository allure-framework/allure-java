package io.qameta.allure.testng.samples;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Sergey Potanin sspotanin@gmail.com
 */
public class DescriptionsAnotherTest {

    /**
     * Before class description from DescriptionsAnotherTest
     */
    @BeforeClass
    @Description(useJavaDoc = true)
    public void setUpClass() {

    }

    /**
     * Before method description from DescriptionsAnotherTest
     */
    @BeforeMethod
    @Description(useJavaDoc = true)
    public void setUpMethod() {

    }

    /**
     * Sample test description from DescriptionsAnotherTest
     */
    @Description(useJavaDoc = true)
    @Test
    public void test() {
        step();
    }

    /**
     * Sample test description from DescriptionsAnotherTest
     * - next line
     * - another line
     */
    @Description(useJavaDoc = true)
    @Test
    public void testSeparated() {
        step();
    }

    /**
     * Sample step description from DescriptionsAnotherTest
     */
    @Description(useJavaDoc = true)
    @Step("Step one")
    private void step() {
    }

}
