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
package io.qameta.allure.spock2.samples

import spock.lang.Ignore;

import io.qameta.allure.AllureId;
import spock.lang.Specification;

class TestsWithIdForFilter extends Specification {

    @AllureId("1")
    def "test 1"() {
        expect:
        true
    }

    @AllureId("2")
    def "test 2"() {
        expect:
        true
    }

    def "test 3"() {
        expect:
        true
    }

    @AllureId("4")
    def "test 4"() {
        expect:
        true
    }

    @Ignore
    @AllureId("5")
    def "test 5"() {
        expect:
        true
    }

    @AllureId("6")
    def "test 6"() {
        expect:
        false
    }
}
