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

class FailedCleanupSpec extends Specification {

    def cleanupSpec() {
        step "cleanup spec before failure"
        throw new RuntimeException("cleanup spec: exception")
    }

    def "feature with failed cleanup spec"() {
        when:
        step "feature body"

        then:
        true
    }
}
