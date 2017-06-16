package io.qameta.allure.spock.samples

import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class BrokenTest extends Specification {

    def "brokenTest"() throws Exception {
        expect:
        throw new RuntimeException("Hello, everybody")
    }
}
