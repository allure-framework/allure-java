/*
 *  Copyright 2019 Qameta Software OÜ
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

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.junit.Test
import spock.lang.Specification

/**
 * @author vbragin
 */
class TestWithCustomAnnotations extends Specification {

    @Test
    @Epic("epic")
    @Feature("feature")
    @Story("story")
    @JiraIssue("AS-1")
    @XrayId("XRT-1")
    def "someTest"() {
        expect:
        true
    }
}
