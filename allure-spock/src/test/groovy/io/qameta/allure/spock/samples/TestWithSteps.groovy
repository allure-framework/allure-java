package io.qameta.allure.spock.samples

import spock.lang.Specification

import static io.qameta.allure.Allure.step

/**
 * @author charlie (Dmitry Baev).
 */
class TestWithSteps extends Specification {

    def "testWithSteps"() {
        expect:
        step "step1"
        step "step2"
        step "step3"
    }
}
