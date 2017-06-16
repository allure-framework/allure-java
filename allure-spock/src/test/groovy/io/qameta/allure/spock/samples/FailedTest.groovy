package io.qameta.allure.spock.samples

import io.qameta.allure.Issue
import io.qameta.allure.Issues
import io.qameta.allure.Link
import io.qameta.allure.Links
import io.qameta.allure.TmsLink
import io.qameta.allure.TmsLinks
import org.junit.Test
import spock.lang.Specification

/**
 * @author charlie (Dmitry Baev).
 */
class FailedTest extends Specification {

    @Test
    @Links([
            @Link("link-1"),
            @Link("link-2")
    ])
    @Issues([
            @Issue("issue-1"),
            @Issue("issue-2")
    ])
    @TmsLinks([
            @TmsLink("tms-1"),
            @TmsLink("tms-2")
    ])
    def "failedTest"() {
        expect:
        false
    }
}
