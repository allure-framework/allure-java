package io.qameta.allure.cucumber3jvm.samples;

import cucumber.api.java.en.Given;
import io.cucumber.datatable.DataTable;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unused")
public class DatatableFeatureSteps {

    @Given("^users are:$")
    public void usersAre(DataTable table) {
    }

}
