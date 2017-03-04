package io.qameta.allure.cucumberjvm;

import org.junit.runner.RunWith;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {"src/test/resources/features"}, plugin = {"io.qameta.allure.cucumberjvm.AllureCucumberJvm"})
public class CucumberTest {
}
