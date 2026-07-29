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

import io.qameta.allure.Step;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

public class MixedGroupFixtures {

    @BeforeGroups(groups = "a")
    public void beforeGroupA() {
        step();
    }

    @BeforeGroups(groups = {"a", "b"})
    public void beforeGroupsAB() {
        step();
    }

    @BeforeGroups(groups = {"b", "a"})
    public void beforeGroupsBA() {
        step();
    }

    @BeforeGroups(groups = "b")
    public void beforeGroupB() {
        step();
    }

    @Test(groups = "a")
    public void testA() {
        step();
    }

    @Test(groups = "b")
    public void testB() {
        step();
    }

    @Test(groups = {"a", "b"})
    public void testAB() {
        step();
    }

    @Test(groups = {"b", "a"})
    public void testBA() {
        step();
    }

    @Step
    public void step() {

    }

}
