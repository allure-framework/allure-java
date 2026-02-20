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
package io.qameta.allure.testng.samples;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.Link;
import io.qameta.allure.Links;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Link("testClass")
@Issue("testClassIssue")
@TmsLink("testClassTmsLink")
public class LinksOnTests {

    @Test
    @Link("a")
    @Link("b")
    @Link("c")
    public void shouldHasLinks() throws Exception {

    }

    @Test
    @Links({
            @Link("nested1"),
            @Link("nested2"),
    })
    @Link("nested3")
    @Issue("issue1")
    @Issues({
            @Issue("issue2"),
            @Issue("issue3")
    })
    @TmsLink("tms1")
    @TmsLinks({
            @TmsLink("tms2"),
            @TmsLink("tms3")
    })
    public void shouldHasLinksAsWell() throws Exception {

    }
}
