package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("epic1")
@Epic("epic2")
@Epic("epic3")
@Feature("feature1")
@Feature("feature2")
@Feature("feature3")
@Story("story1")
@Stories({
        @Story("story2"),
        @Story("story3")
})
@Owner("some-owner")
public class TestWithClassLabels {

    @Test
    void someTest() {
    }

}
