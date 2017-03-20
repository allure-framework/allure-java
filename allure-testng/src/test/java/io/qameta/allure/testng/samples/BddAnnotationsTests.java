package io.qameta.allure.testng.samples;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("class-epic1")
@Epic("class-epic2")
@Feature("class-feature1")
@Feature("class-feature2")
@Story("class-story1")
@Story("class-story2")
public class BddAnnotationsTests {

    @Epic("epic1")
    @Epic("epic2")
    @Feature("feature1")
    @Feature("feature2")
    @Story("story1")
    @Story("story2")
    @Test
    public void shouldHasBddAnnotations() throws Exception {
    }
}
