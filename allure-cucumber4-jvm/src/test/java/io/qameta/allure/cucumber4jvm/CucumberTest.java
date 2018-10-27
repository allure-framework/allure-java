package io.qameta.allure.cucumber4jvm;

import cucumber.api.CucumberOptions;
import cucumber.api.testng.AbstractTestNGCucumberTests;
import org.testng.annotations.DataProvider;

@CucumberOptions(
        features = {"src/test/resources/features/"},
        plugin = {"io.qameta.allure.cucumber4jvm.AllureCucumber4Jvm"},
        tags = {"@good"})
public class CucumberTest extends AbstractTestNGCucumberTests {
    @Override
    @DataProvider(parallel = true)
    public Object[][]scenarios(){
        return super.scenarios();
    }
}
