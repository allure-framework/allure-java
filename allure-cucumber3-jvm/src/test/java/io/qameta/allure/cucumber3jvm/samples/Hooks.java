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
package io.qameta.allure.cucumber3jvm.samples;

import cucumber.api.java.After;
import cucumber.api.java.Before;

public class Hooks {

    @Before("@bp")
    public void beforePassed() {
    }

    @Before("@bf")
    public void beforeFailed() {
        throw new AssertionError("This hook should fail");
    }

    @After("@ap")
    public void afterPassed() {
    }

    @After("@af")
    public void afterFailed() {
        throw new AssertionError("This hook should fail");
    }
}
