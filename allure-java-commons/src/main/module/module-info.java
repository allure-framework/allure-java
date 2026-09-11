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
// Friend modules depend on commons and cannot be present while this descriptor is compiled.
@SuppressWarnings("module") module io.qameta.allure.commons {
    requires transitive io.qameta.allure.model;
    requires org.slf4j;
    requires static org.aspectj.runtime;
    requires java.management;
    // Preserve the bundled Jackson implementation's required and optional JDK dependencies.
    requires java.logging;
    requires static java.desktop;
    requires static java.sql;
    requires static java.xml;

    exports io.qameta.allure;
    exports io.qameta.allure.aspects;
    exports io.qameta.allure.http;
    exports io.qameta.allure.listener;
    exports io.qameta.allure.testfilter to
            io.qameta.allure.junitplatform,
            io.qameta.allure.junit4,
            io.qameta.allure.spock2,
            io.qameta.allure.testng;
    exports io.qameta.allure.internal.json to
            io.qameta.allure.commonstest,
            io.qameta.allure.jsonunit;
    exports io.qameta.allure.util;

    uses io.qameta.allure.listener.ContainerLifecycleListener;
    uses io.qameta.allure.listener.TestLifecycleListener;
    uses io.qameta.allure.listener.FixtureLifecycleListener;
    uses io.qameta.allure.listener.StepLifecycleListener;
}
