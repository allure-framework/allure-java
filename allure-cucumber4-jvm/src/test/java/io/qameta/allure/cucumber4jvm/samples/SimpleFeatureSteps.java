package io.qameta.allure.cucumber4jvm.samples;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.testng.Assert;

/**
 * @author charlie (Dmitry Baev).
 */
public class SimpleFeatureSteps {

    private int a;
    private int b;
    private int c;

    @Given("^a is (\\d+)$")
    public void a_is(int arg1) {
        this.a = arg1;
    }

    @Given("^b is (\\d+)$")
    public void b_is(int arg1) {
        this.b = arg1;
    }

    @When("^I add a to b$")
    public void i_add_a_to_b() {
        this.c = this.a + this.b;
    }

    @Then("^result is (\\d+)$")
    public void result_is(int arg1) {
        Assert.assertEquals(this.c, arg1);
    }

}
