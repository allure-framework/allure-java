package io.qameta.allure.spock.samples

import io.qameta.allure.Epic
import io.qameta.allure.Epics
import io.qameta.allure.Feature
import io.qameta.allure.Features
import io.qameta.allure.Flaky
import io.qameta.allure.Muted
import io.qameta.allure.Owner
import io.qameta.allure.Stories
import io.qameta.allure.Story
import org.junit.Test
import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class TestWithAnnotations extends Specification {

    @Test
    @Epic("epic1")
    @Epics([
            @Epic("epic2"),
            @Epic("epic3")
    ])

    @Features([
            @Feature("feature1"),
            @Feature("feature2")
    ])
    @Feature("feature3")
    @Story("story1")
    @Stories([
            @Story("story2"),
            @Story("story3")]
    )
    @Owner("some-owner")
    @Flaky
    @Muted
    def "someTest"() {
        expect:
        true
    }
}
