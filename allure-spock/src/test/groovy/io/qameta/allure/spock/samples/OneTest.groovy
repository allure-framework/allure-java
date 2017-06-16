package io.qameta.allure.spock.samples

import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class OneTest extends Specification {

    def "Simple Test"() {
        expect:
        true
    }
}
