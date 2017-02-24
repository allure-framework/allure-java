package io.qameta.allure.samples;

import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class LinksOnTestsInherited extends LinksOnTests {

    @Override
    public void shouldHasLinks() throws Exception {
        super.shouldHasLinks();
    }

    @Override
    @Test
    @Link("inheritedLink1")
    @Link("inheritedLink2")
    @Issue("inheritedIssue")
    @TmsLink("inheritedTmsLink")
    public void shouldHasLinksAsWell() throws Exception {
        super.shouldHasLinksAsWell();
    }

}
