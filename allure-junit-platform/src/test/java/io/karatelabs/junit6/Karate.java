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
package io.karatelabs.junit6;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;

/**
 * Minimal test fixture for the current Karate JUnit package.
 */
public final class Karate implements Iterable<DynamicNode> {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @TestFactory
    public @interface Test {
    }

    private Karate() {
    }

    public static Karate run() {
        return new Karate();
    }

    @Override
    public Iterator<DynamicNode> iterator() {
        final DynamicTest scenario = DynamicTest.dynamicTest("[1:3] Current scenario", () -> {
        });
        final DynamicContainer feature = DynamicContainer.dynamicContainer("Current web page", List.of(scenario));
        return List.<DynamicNode>of(feature).iterator();
    }
}
