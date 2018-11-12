package io.qameta.allure.cucumberjvm.samples;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unused")
public class DatatableFeatureSteps {

    @Given("^users are:$")
    public void usersAre(DataTable table) {
    }

}
