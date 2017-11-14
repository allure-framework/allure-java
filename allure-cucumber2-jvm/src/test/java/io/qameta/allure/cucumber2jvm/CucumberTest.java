package io.qameta.allure.cucumber2jvm;

import cucumber.api.CucumberOptions;
import cucumber.api.testng.AbstractTestNGCucumberTests;

@CucumberOptions(
        features = {"src/test/resources/features/"},
        plugin = {"io.qameta.allure.cucumber2jvm.AllureCucumber2Jvm"},
        tags = {"@good"})
public class CucumberTest extends AbstractTestNGCucumberTests {
}
