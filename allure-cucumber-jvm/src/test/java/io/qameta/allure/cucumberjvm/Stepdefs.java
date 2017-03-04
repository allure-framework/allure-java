package io.qameta.allure.cucumberjvm;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;

public class Stepdefs {

    private int a, b, c;

    @Given("^a is (\\d+)$")
    public void a_is(int arg1) throws Throwable {
        this.a = arg1;
    }

    @Given("^b is (\\d+)$")
    public void b_is(int arg1) throws Throwable {
        this.b = arg1;
    }

    @When("^I add a to b$")
    public void i_add_a_to_b() throws Throwable {
        this.c = this.a + this.b;
    }

    @Then("^result is (\\d+)$")
    public void result_is(int arg1) throws Throwable {
        Assert.assertEquals(this.c, arg1);
    }
}
