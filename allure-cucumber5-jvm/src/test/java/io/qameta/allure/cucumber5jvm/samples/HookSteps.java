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
package io.qameta.allure.cucumber5jvm.samples;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.assertj.core.api.Assertions;

/**
 * @author letsrokk (Dmitry Mayer).
 */
public class HookSteps {

    @Before("@WithHooks")
    public void beforeHook(){
        // nothing
    }

    @After("@WithHooks")
    public void afterHook(){
        // nothing
    }

    @Before("@BeforeHookWithException")
    public void beforeHookWithException(){
        Assertions.fail("Exception in Hook step");
    }

    @After("@AfterHookWithException")
    public void afterHookWithException(){
        Assertions.fail("Exception in Hook step");
    }

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
