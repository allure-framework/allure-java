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
package io.qameta.allure.cucumber5jvm.samples;

import io.cucumber.java.Scenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;

public class AttachmentSteps
{
    private Scenario scenario;

    @Before("@attachments")
    public void setup(Scenario scenario)
    {
        this.scenario = scenario;
    }

    @Given("step with scenario write")
    public void stepWithScenarioWrite()
    {
        scenario.write("text attachment");
    }

    @Given("step with scenario embed")
    public void stepWithScenarioEmbed()
    {
        scenario.embed("image attachment".getBytes(), "image/png","ImageAttachment");
    }
}
