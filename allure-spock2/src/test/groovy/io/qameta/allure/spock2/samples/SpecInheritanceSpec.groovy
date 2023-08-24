/*
 *  Copyright 2022 Qameta Software OÜ
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

abstract class BaseSpec extends Specification {
    def setupSpec() { step 'base setupSpec()' }

    def cleanupSpec() { step 'base cleanupSpec()' }

    def setup() { step 'base setup()' }

    def cleanup() { step 'base cleanup()' }

    def baseSpecMethod() {
        setup:
        step 'base spec method'
    }
}

class DerivedSpec extends BaseSpec {
    def setupSpec() { step 'derived setupSpec()' }

    def cleanupSpec() { step 'derived cleanupSpec()' }

    def setup() { step 'derived setup()' }

    def cleanup() { step 'derived cleanup()' }

    def derivedSpecMethod() {
        setup:
        step 'derived spec method'
    }
}
