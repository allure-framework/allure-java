package io.qameta.allure;


import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class ProjectServiceTest {

    @BeforeSuite
    public void loadTestConfiguration() throws Exception {
    }

    @BeforeClass
    public void setUpDatabase() throws Exception {
        validateEndpoint("jdbc://example.org/database");
    }

    @BeforeTest
    public void mockAuthorization() throws Exception {
        String username = generateRandomUsername();
        createMockUser(username);
        loginAs(username);
    }

    @AfterClass
    public void cleanUpDatabase() throws Exception {
        closeDatabaseConnection();
    }

    @AfterTest
    public void cleanUpContext() throws Exception {
        closeContext();
    }

    @Step
    public void validateEndpoint(String databaseEndpoint) {
    }

    @Step
    public void createMockUser(String username) {
    }

    @Step
    public String generateRandomUsername() {
        return "some-user-123";
    }

    @Step
    public void loginAs(String username) {
    }

    @Step
    public void closeDatabaseConnection() {
    }

    @Step
    public void closeContext() {
    }

    @DataProvider(name = "names")
    public Object[][] createNames() {
        return new Object[][]{
                {"Cedric"},
                {"Anne"},
        };
    }

    @Test(dataProvider = "names")
    public void shouldCreate(String name) throws Exception {
        createProject();
        validateExists();
    }

    @Test
    public void shouldDelete() throws Exception {
        createProject();
        validateExists();
        deleteProject();
        validateNotExists();
    }

    @Step
    public void createProject() {
        more();
        andMore();
        andEvenMore();
    }

    @Step
    public void validateExists() {
    }

    @Step
    public void deleteProject() {
    }

    @Step
    public void validateNotExists() {
    }

    @Step
    public void more() {
    }

    @Step
    public void andMore() {
    }

    @Step
    public void andEvenMore() {
    }
}
