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
package fixture.tests

import io.qameta.allure.Allure
import io.qameta.allure.Owner
import spock.lang.Specification

class SampleTest extends Specification {
    @Owner("module owner")
    def "module feature"() {
        when:
        Allure.step("operation")
        Allure.attachment("payload", "text/plain", "Spock module payload")

        then:
        2 + 2 == 4
    }
}
