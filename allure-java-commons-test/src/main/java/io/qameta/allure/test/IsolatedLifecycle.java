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
package io.qameta.allure.test;

import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks tests that run an isolated Allure lifecycle through {@link RunUtils}. The harness swaps the process-wide
 * lifecycle so the facade is exercised exactly as in production — same instance from any thread — which means such
 * tests must never run concurrently with each other. This composed {@link ResourceLock} lets the JUnit Platform
 * schedule them exclusively while unrelated tests stay parallel. Every test class using {@link RunUtils} (directly
 * or through a module harness) must carry this annotation.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ResourceLock(IsolatedLifecycle.RESOURCE)
public @interface IsolatedLifecycle {

    /**
     * The resource name representing the process-wide Allure lifecycle.
     */
    String RESOURCE = "io.qameta.allure.Allure.lifecycle";
}
