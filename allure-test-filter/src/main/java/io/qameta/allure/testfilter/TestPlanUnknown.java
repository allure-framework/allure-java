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
package io.qameta.allure.testfilter;

import java.io.Serializable;

/**
 * Fallback model for an Allure test plan with an unknown schema.
 *
 * <p>Use this implementation when a plan file exists but cannot be parsed as a supported version. Integrations can still treat the plan as present while avoiding unsafe filtering decisions.</p>
 */
public class TestPlanUnknown implements TestPlan, Serializable {

    private static final long serialVersionUID = 1L;

}
