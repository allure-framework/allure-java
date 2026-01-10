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
package io.qameta.allure.testng.samples;

import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class LinksOnTestsInherited extends LinksOnTests {

    @Override
    public void shouldHasLinks() throws Exception {
        super.shouldHasLinks();
    }

    @Override
    @Test
    @Link("inheritedLink1")
    @Link("inheritedLink2")
    @Issue("inheritedIssue")
    @TmsLink("inheritedTmsLink")
    public void shouldHasLinksAsWell() throws Exception {
        super.shouldHasLinksAsWell();
    }

}
