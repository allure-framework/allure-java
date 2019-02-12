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
package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.Link;
import io.qameta.allure.Links;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import org.junit.jupiter.api.Test;

public class TestWithMethodLinks {

    @Test
    @Link(name = "LINK-1")
    @Links({
            @Link(name = "LINK-2", url = "https://example.org/link/2"),
            @Link(url = "https://example.org/some-custom-link")
    })
    @TmsLink("TMS-1")
    @TmsLinks({
            @TmsLink("TMS-2"),
            @TmsLink("TMS-3")
    })
    @Issue("ISSUE-1")
    @Issues({
            @Issue("ISSUE-2"),
            @Issue("ISSUE-3")
    })
    void someTest() {
    }

}
