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
package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class InheritedTests {
    public static final String INHERITED_TEST_EPIC = "Inherited epic";
    public static final String INHERITED_TEST_FUTURE = "Inherited future";
    public static final String INHERITED_TEST_GRANDPARENT_STORY = "Inherited grandparent story";

    public static final String INHERITED_TEST_PARENT_STORY = "Inherited parent story";

    public static final String INHERITED_TEST_CHILD_STORY = "Inherited child story";

    public static final String TEST_DESCRIPTION = "Test description";

    public static final String TEST_LINK = "Test link";

    @Epic(INHERITED_TEST_EPIC)
    @Feature(INHERITED_TEST_FUTURE)
    public interface GrandparentTest {
        @Test
        @Description(TEST_DESCRIPTION)
        @Story(INHERITED_TEST_GRANDPARENT_STORY)
        @Link(TEST_LINK)
        default void grandparentTest() {
        }
    }

    public abstract static class ParentTest implements GrandparentTest {
        @Test
        @Description(TEST_DESCRIPTION)
        @Story(INHERITED_TEST_PARENT_STORY)
        @Link(TEST_LINK)
        void parentTest() {
        }

    }

    @Nested
    class ChildTest extends ParentTest {
        @Test
        @Description(TEST_DESCRIPTION)
        @Story(INHERITED_TEST_CHILD_STORY)
        @Link(TEST_LINK)
        void childTest() {
        }
    }
}
