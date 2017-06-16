package io.qameta.allure.spock.samples

import io.qameta.allure.Step
import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class TestWithSteps extends Specification {

    def "testWithSteps"() {
        expect:
        step1()
        step2()
        step3()
    }

    @Step
    void step1() {
    }

    @Step
    void step2() {
    }

    @Step
    void step3() {
    }
}
