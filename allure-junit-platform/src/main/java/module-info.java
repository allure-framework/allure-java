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
module io.qameta.allure.junitplatform {
    requires transitive io.qameta.allure.commons;
    requires org.junit.platform.launcher;
    requires org.junit.jupiter.api;
    requires org.slf4j;

    exports io.qameta.allure.junitplatform;

    provides org.junit.platform.launcher.TestExecutionListener
            with io.qameta.allure.junitplatform.AllureJunitPlatform;
    provides org.junit.platform.launcher.PostDiscoveryFilter
            with io.qameta.allure.junitplatform.AllurePostDiscoveryFilter;
}
