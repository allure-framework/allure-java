package io.qameta.allure.samples;

import io.qameta.allure.Link;
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
    public void shouldHasLinksAsWell() throws Exception {
        super.shouldHasLinksAsWell();
    }

}
