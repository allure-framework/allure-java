module allure.fixture {
    requires io.cucumber.core;
    requires io.cucumber.java;
    requires io.qameta.allure.cucumber7jvm;
    opens fixture to io.cucumber.java;
}
