package io.qameta.allure.cucumberjvm;

import org.junit.runner.RunWith;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"src/test/resources/features"},
        plugin = {"io.qameta.allure.cucumberjvm.AllureCucumberJvm"},
        junit = {"--filename-compatible-names"},
        tags = {"@good"})
public class CucumberTest {
}
