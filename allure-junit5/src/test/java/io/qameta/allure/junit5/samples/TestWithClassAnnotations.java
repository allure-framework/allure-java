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
@ExtendWith(AllureJunit5AnnotationProcessor.class)
public class TestWithClassAnnotations {

    @Test
    public void someTest() throws Exception {
    }

}
