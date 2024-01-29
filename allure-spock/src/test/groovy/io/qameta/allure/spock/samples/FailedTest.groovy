/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
