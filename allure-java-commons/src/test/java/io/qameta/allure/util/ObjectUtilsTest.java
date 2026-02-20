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
package io.qameta.allure.util;

import io.qameta.allure.Issue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class ObjectUtilsTest {

    @Issue("191")
    @Test
    void shouldProcessToStringNpe() {
        final MyNpeClass myNpeClass = new MyNpeClass();
        final String string = ObjectUtils.toString(myNpeClass);
        assertThat(string)
                .isEqualTo("<NPE>");
    }

    public class MyNpeClass {

        Integer value = null;

        @Override
        public String toString() {
            return value.toString();
        }
    }

}
