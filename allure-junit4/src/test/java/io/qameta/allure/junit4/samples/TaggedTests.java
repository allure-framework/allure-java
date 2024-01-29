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
package io.qameta.allure.junit4.samples;

import io.qameta.allure.junit4.Tag;
import io.qameta.allure.junit4.Tags;
import org.junit.Test;

/**
 * @author jkttt on 05.07.17.
 */
@Tags({@Tag(TaggedTests.CLASS_TAG1), @Tag(TaggedTests.CLASS_TAG2)})
public class TaggedTests {

    public static final String METHOD_TAG2 = "method_tag1";
    public static final String METHOD_TAG1 = "method_tag2";
    public static final String CLASS_TAG1 = "class_tag1";
    public static final String CLASS_TAG2 = "class_tag2";

    @Test
    @Tags({@Tag(METHOD_TAG1),
            @Tag(METHOD_TAG2)})
    public void taggedTest() {}
}
