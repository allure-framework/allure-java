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
package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author jkttt on 05.07.17.
 */
@Tag(TaggedTests.CLASS_TAG)
public class TaggedTests {

    public static final String CLASS_TAG = "class_tag";
    public static final String METHOD_TAG = "single_tag";

    @Test
    @Tag(METHOD_TAG)
    void taggedTest() {}
}
