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

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("class-epic1")
@Epic("class-epic2")
@Feature("class-feature1")
@Feature("class-feature2")
@Story("class-story1")
@Story("class-story2")
public class BddAnnotationsTests {

    @Epic("epic1")
    @Epic("epic2")
    @Feature("feature1")
    @Feature("feature2")
    @Story("story1")
    @Story("story2")
    @Test
    public void shouldHasBddAnnotations() throws Exception {
    }
}
