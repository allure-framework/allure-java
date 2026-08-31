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
package io.qameta.allure.testng.samples;

import io.qameta.allure.testng.Tag;
import io.qameta.allure.testng.Tags;
import org.testng.annotations.Test;

@Tags({@Tag(MetadataTagTest.ALLURE_LABEL_TAG), @Tag(MetadataTagTest.PLAIN_TAG)})
public class MetadataTagTest {

    public static final String ALLURE_LABEL_TAG = "allure.label.suite:TestNG_metadata";
    public static final String PLAIN_TAG = "smoke";

    @Test
    public void metadataTag() {
    }

}
