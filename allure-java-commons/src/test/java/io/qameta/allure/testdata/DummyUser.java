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
package io.qameta.allure.testdata;

import java.util.Arrays;

/**
 * @author sskorol (Sergey Korol)
 */
public class DummyUser {

    private final DummyEmail[] emails;
    private final String password;
    private final DummyCard card;

    public DummyUser(final DummyEmail[] emails, final String password, DummyCard card) {
        this.emails = emails;
        this.password = password;
        this.card = card;
    }

    public DummyEmail[] getEmail() {
        return emails;
    }

    public String getPassword() {
        return password;
    }

    public DummyCard getCard() {
        return card;
    }

    @Override
    public String toString() {
        return "DummyUser{" +
                "emails='" + Arrays.toString(emails) + '\'' +
                ", password='" + password + '\'' +
                ", card=" + card +
                '}';
    }
}
