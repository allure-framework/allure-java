package io.qameta.allure.junit5.samples;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.Link;
import io.qameta.allure.Links;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import org.junit.jupiter.api.Test;

public class TestWithMethodLinks {

    @Test
    @Link(name = "LINK-1")
    @Links({
            @Link(name = "LINK-2"),
            @Link(name = "LINK-3")
    })
    @TmsLink("TMS-1")
    @TmsLinks({
            @TmsLink("TMS-2"),
            @TmsLink("TMS-3")
    })
    @Issue("ISSUE-1")
    @Issues({
            @Issue("ISSUE-2"),
            @Issue("ISSUE-3")
    })
    public void someTest() throws Exception {
    }

}
