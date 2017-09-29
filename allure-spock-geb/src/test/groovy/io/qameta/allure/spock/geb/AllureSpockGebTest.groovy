package io.qameta.allure.spock.geb

import geb.spock.GebReportingSpec
import spock.lang.Stepwise

@Stepwise
public class AllureSpockGebTest extends GebReportingSpec {

    def 'every step ends with report method execution'() {
        when:
        go 'http://www.gebish.org/'

        then:
        title == 'Geb - Very Groovy Browser Automation'
    }

    def 'check results directory after first step'() {
        when:
        def resultsDir = new File('build/allure-results').listFiles()

        then:
        resultsDir[0].name.endsWith 'attachment.html'
    }

}