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
package io.qameta.allure.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalsTest {

    @Test
    void shouldHaveMutableEmptyCollectionsByDefault() {
        final Globals globals = new Globals();
        final GlobalAttachment attachment = new GlobalAttachment();
        final GlobalError error = new GlobalError();

        assertThat(globals.getAttachments()).isEmpty();
        assertThat(globals.getErrors()).isEmpty();
        assertThat(globals.getAttachments().add(attachment)).isTrue();
        assertThat(globals.getErrors().add(error)).isTrue();
        assertThat(globals.getAttachments()).containsExactly(attachment);
        assertThat(globals.getErrors()).containsExactly(error);
    }

    @Test
    void shouldSetAndCompareGlobalValues() {
        final GlobalAttachment attachment = new GlobalAttachment()
                .setSource("setup.log")
                .setTimestamp(123L);
        final GlobalError error = new GlobalError()
                .setMessage("setup failed")
                .setTimestamp(456L);
        final Globals globals = new Globals()
                .setAttachments(List.of(attachment))
                .setErrors(List.of(error));
        final Globals equalGlobals = new Globals()
                .setAttachments(
                        List.of(
                                new GlobalAttachment()
                                        .setSource("setup.log")
                                        .setTimestamp(123L)
                        )
                )
                .setErrors(
                        List.of(
                                new GlobalError()
                                        .setMessage("setup failed")
                                        .setTimestamp(456L)
                        )
                );

        assertThat(globals.getAttachments()).containsExactly(attachment);
        assertThat(globals.getErrors()).containsExactly(error);
        assertThat(globals)
                .isEqualTo(globals)
                .isEqualTo(equalGlobals)
                .isNotEqualTo(new Globals().setAttachments(List.of(new GlobalAttachment())))
                .isNotEqualTo(new Globals().setErrors(List.of(new GlobalError())))
                .isNotEqualTo(null);
        assertThat(globals.hashCode()).isEqualTo(equalGlobals.hashCode());
    }
}
