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

import spock.lang.Specification

import static io.qameta.allure.Allure.step

/**
 * @author charlie (Dmitry Baev).
 */
class TestWithSteps extends Specification {

    def "testWithSteps"() {
        setup: "setup. Param1: #param1 Param2: #param2"

        when: "when1"
        and: "and1"
        then: "then1"
        and: "and2"

        when: "when2"
        and: "and3"
        then: "then2"
        and: "and4"

        expect: "expect"
        step "step1"
        step "step2"
        step "step3"

        where: "where"
            param1 = "param1"
            param2 = "param2"
    }
}
