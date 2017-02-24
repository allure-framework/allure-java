package io.qameta.allure.samples;

import io.qameta.allure.Link;
import io.qameta.allure.Links;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Link("testClass")
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
    public void shouldHasLinksAsWell() throws Exception {

    }
}
