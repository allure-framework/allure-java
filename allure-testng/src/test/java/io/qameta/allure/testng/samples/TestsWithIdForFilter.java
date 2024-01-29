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

import io.qameta.allure.AllureId;
import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class TestsWithIdForFilter {

    @Test
    @AllureId("1")
    public void test1() {
    }

    @Test
    @AllureId("2")
    public void test2() {
    }

    @Test
    public void test3() {
    }

    @Test
    @AllureId("4")
    public void test4() {
    }

    @Test(enabled = false)
    @AllureId("5")
    public void skipped() {
    }

    @Test
    @AllureId("6")
    public void test6() {
        assertThat(true).isFalse();
    }
}
