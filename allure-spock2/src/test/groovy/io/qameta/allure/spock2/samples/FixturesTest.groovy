/*
 *  Copyright 2016-2026 Qameta Software Inc
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


import spock.lang.Specification

import static io.qameta.allure.Allure.step

/**
 * @author charlie (Dmitry Baev).
 */
class FixturesTest extends Specification {

    def setupSpec() {
        step "setupSpec step 1"
        step "setupSpec step 2"
    }

    def cleanupSpec() {
        step "cleanupSpec step 1"
        step "cleanupSpec step 2"
    }

    def setup() {
        step "setup step 1"
        step "setup step 2"
    }

    def cleanup() {
        step "cleanup step 1"
        step "cleanup step 2"
    }

    def "First Test"() {
        expect:
        true
    }

    def "Second Test"() {
        expect:
        true
    }
}
