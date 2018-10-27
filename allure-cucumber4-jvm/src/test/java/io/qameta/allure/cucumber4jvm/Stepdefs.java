package io.qameta.allure.cucumber4jvm;

//import cucumber.api.DataTable;
import io.cucumber.datatable.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;

import io.qameta.allure.Step;
import org.testng.Assert;

public class Stepdefs {

    private int a, b, c;

    @Before
    public void setUp() {
        System.out.println("Before");
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

    @Given("^users are:$")
    public void usersAre(DataTable table) throws Throwable {
    }

    @Given("^step with argument (\\d+) and data table:$")
    public void stepWithArgumentAndDataTable(int argument, DataTable table) throws Throwable {
    }

    @When("^I attach picture to step$")
    public void iAttachPictureToStep() throws Throwable {
        Allure.addAttachment("Picture of a kitten",
                getClass().getClassLoader().getResourceAsStream("images/totally-open-source-kitten.jpeg"));
    }

    @Then("^it is displayed in report$")
    public void itIsDisplayedInReport() throws Throwable {
        sleep();
    }

    private void sleep() throws InterruptedException {
//        Thread.sleep(1000 + new Random().nextInt(1000));
    }

    @When("^I execute steps with @Step$")
    public void iExecuteStepsWithStep() throws Throwable {
        methodWithStepAnnotation();
    }

    @Step("Sub-step step")
    private void methodWithStepAnnotation(){

    }

    @When("^I attach file in sub-step with (.+) in name$")
    public void iAttachFileInSubStep(String value) throws Throwable {
        subStepWithAttachment();
    }

    @When("^I attach file in sub-step$")
    public void iAttachFileInSubStep() throws Throwable {
        subStepWithAttachment();
    }

    @Step("Sub-step with attachment File")
    @Attachment
    private byte[] subStepWithAttachment() throws IOException {
        return Files.readAllBytes(
                new File(getClass().getClassLoader().getResource("images/totally-open-source-kitten.jpeg").getFile()).toPath()
        );
    }

    @When("^call step with PendingException$")
    public void callUnimplementedStep() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("^test case market as unimplemented$")
    public void testCaseMarketAsUnimplemented() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }
}
