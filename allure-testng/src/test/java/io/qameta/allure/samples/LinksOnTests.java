package io.qameta.allure.samples;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.Link;
import io.qameta.allure.Links;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Link("testClass")
@Issue("testClassIssue")
@TmsLink("testClassTmsLink")
public class LinksOnTests {

    @Test
    @Link("a")
    @Link("b")
    @Link("c")
    public void shouldHasLinks() throws Exception {

    }

    @Test
    @Links({
            @Link("nested1"),
            @Link("nested2"),
    })
    @Link("nested3")
    @Issue("issue1")
    @Issues({
            @Issue("issue2"),
            @Issue("issue3")
    })
    @TmsLink("tms1")
    @TmsLinks({
            @TmsLink("tms2"),
            @TmsLink("tms3")
    })
    public void shouldHasLinksAsWell() throws Exception {

    }
}
