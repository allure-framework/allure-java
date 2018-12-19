package io.qameta.allure.spock.samples

import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class DataDrivenTest extends Specification {

    def "Simple Test"() {
        expect:
        Math.max(a, b) == c

        where:
        a | b || c
        1 | 3 || 3
        7 | 4 || 7
        0 | 0 || 0
    }
}
