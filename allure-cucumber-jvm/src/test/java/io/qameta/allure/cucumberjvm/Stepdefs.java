package io.qameta.allure.cucumberjvm;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.qameta.allure.Attachment;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.junit.Assert;

public class Stepdefs {

    private int a, b, c;

    @Before
    public void setUp() {
    }

    @Before
    public void setUp2() {
    }

    @After
    public void tearDown() {
        System.out.println("After");
    }

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

    @Attachment(type = "image/png", fileExtension = "png", value = "att")
    public byte[] attach() {
        try {
            BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            return imageInByte;
        } catch (AWTException | IOException e) {
            return null;
        }
    }
}
