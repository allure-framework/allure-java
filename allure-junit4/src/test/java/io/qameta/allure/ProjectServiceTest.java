package io.qameta.allure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class ProjectServiceTest {

    @Before
    public void setUpDatabase() throws Exception {
        validateEndpoint("jdbc://example.org/database");
    }

    @Before
    public void mockAuthorization() throws Exception {
        String username = generateRandomUsername();
        createMockUser(username);
        loginAs(username);
    }

    @After
    public void cleanUpDatabase() throws Exception {
        closeDatabaseConnection();
    }

    @After
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

    @Test
    public void shouldCreate() throws Exception {
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
}
