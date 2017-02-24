package io.qameta.allure.samples;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Story("story-inherited")
public class BddAnnotationsTestsInherited extends BddAnnotationsTests {

    @Override
    @Epic("epic-inherited")
    @Test
    public void shouldHasBddAnnotations() throws Exception {
    }
}
