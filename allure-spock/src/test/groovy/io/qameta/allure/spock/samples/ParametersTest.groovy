package io.qameta.allure.spock.samples

import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class ParametersTest extends Specification {

    def "Simple Test"() {
        expect:
        Math.max(a, b) == c

        where:
        a | b || c
        1 | 3 || 3
    }
}
