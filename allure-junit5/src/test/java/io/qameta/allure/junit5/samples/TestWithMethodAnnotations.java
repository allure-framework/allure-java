package io.qameta.allure.junit5.samples;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import io.qameta.allure.junit5.AllureJunit5AnnotationProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestWithMethodAnnotations {

    @Test
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
    public void someTest() throws Exception {
    }
}
