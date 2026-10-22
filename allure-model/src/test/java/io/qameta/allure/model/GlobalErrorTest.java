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

import static org.assertj.core.api.Assertions.assertThat;

class GlobalErrorTest {

    @Test
    void shouldDefaultGlobalErrorToBroken() {
        assertThat(new GlobalError().getStatus()).isEqualTo(Status.BROKEN);
    }

    @Test
    void shouldKeepExplicitGlobalErrorStatus() {
        final GlobalError error = new GlobalError();
        assertThat(error.setStatus(Status.FAILED)).isSameAs(error);
        assertThat(error.getStatus()).isEqualTo(Status.FAILED);
        assertThat(error.setStatus(Status.BROKEN)).isSameAs(error);
        assertThat(error.getStatus()).isEqualTo(Status.BROKEN);
    }

    @Test
    void shouldIncludeStatusInGlobalErrorValueSemantics() {
        final GlobalError failed = globalError("error", 123L).setStatus(Status.FAILED);
        final GlobalError equalError = globalError("error", 123L).setStatus(Status.FAILED);
        final GlobalError broken = globalError("error", 123L).setStatus(Status.BROKEN);

        assertThat(failed).isEqualTo(equalError).isNotEqualTo(broken);
        assertThat(failed.hashCode()).isEqualTo(equalError.hashCode());
    }

    @Test
    void shouldCompareStatusDetailsAndTimestamp() {
        final GlobalError error = globalError("global setup failed", 123L);
        final GlobalError equalError = globalError("global setup failed", 123L);

        assertThat(error)
                .isEqualTo(error)
                .isEqualTo(equalError)
                .isNotEqualTo(globalError("different error", 123L))
                .isNotEqualTo(globalError("global setup failed", 456L))
                .isNotEqualTo(globalError("global setup failed", 123L).setKnown(false))
                .isNotEqualTo(globalError("global setup failed", 123L).setMuted(false))
                .isNotEqualTo(globalError("global setup failed", 123L).setFlaky(false))
                .isNotEqualTo(new StatusDetails().setMessage("global setup failed"))
                .isNotEqualTo(null);
        assertThat(error.hashCode()).isEqualTo(equalError.hashCode());
    }

    private static GlobalError globalError(final String message, final long timestamp) {
        return new GlobalError()
                .setKnown(true)
                .setMuted(true)
                .setFlaky(true)
                .setMessage(message)
                .setTrace("stack trace")
                .setActual("actual")
                .setExpected("expected")
                .setTimestamp(timestamp);
    }
}
