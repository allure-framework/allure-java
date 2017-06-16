package io.qameta.allure.testng.samples;

/*
  @author Andrejs Kalnacs akalnacs@evolutiongaming.com
 */
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class SuiteParameterTest {
    @Parameters("parameter")
    @BeforeSuite
    public void beforeSuite(String parameter, ITestContext context) {
        context.getCurrentXmlTest().addParameter("param", parameter);
    }

    @Test()
    public void simpleTest() {
    }
}
