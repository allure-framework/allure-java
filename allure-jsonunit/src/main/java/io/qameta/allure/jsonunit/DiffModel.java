/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.jsonunit;

/**
 * The class represents diff model containing patch, actual and expected data.
 */
public class DiffModel {

    private final String patch;
    private final String actual;
    private final String expected;

    public DiffModel(final String actual, final String expected, final String patch) {
        this.actual = actual;
        this.expected = expected;
        this.patch = patch;
    }

    public String getPatch() {
        return patch;
    }

    public String getActual() {
        return actual;
    }

    public String getExpected() {
        return expected;
    }
}
