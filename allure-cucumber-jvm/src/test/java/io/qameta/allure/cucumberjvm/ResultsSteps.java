package io.qameta.allure.cucumberjvm;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public class ResultsSteps {

    private String resultFilePath;

    @When("^looking for results in ([a-zA-Z0-9/-]+)$")
    public void lookingForReports(String resultFilePath) throws Throwable {
        this.resultFilePath = resultFilePath;
    }

    @Then("^result file with name should be present: (.*)$")
    public void reportFileWithNameShouldBePresent(final String filenamePattern) throws Throwable {
        File[] resultFiles = Optional.ofNullable(new File(resultFilePath)
                .listFiles())
                .orElseThrow(() -> new NoSuchFileException(filenamePattern));

        boolean isMatch = Arrays.stream(resultFiles)
                .anyMatch(file -> Pattern.matches(filenamePattern, file.getName()));
        Assert.assertEquals("Match: " + filenamePattern, isMatch, true);
    }

    @Given("^scenario name is (.*)$")
    public void scenarioNameIsNAME(String name) throws Throwable {
        Thread.sleep(100);
    }

    @Then("^scenario should pass == ([a-z]+)$")
    public void scenarioShouldPassSTATUS(boolean isTrue) throws Throwable {
        Assert.assertTrue(isTrue);
    }
}
