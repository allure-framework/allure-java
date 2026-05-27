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
package io.qameta.allure;

/**
 * Signals that Allure results could not be written.
 *
 * <p>The file-system writer throws this unchecked exception when creating directories, writing JSON files, or copying attachment streams fails. Let it fail the current run because report output is incomplete.</p>
 */
public class AllureResultsWriteException extends RuntimeException {

    /**
     * Creates an Allure results write exception with the supplied values.
     *
     * @param message the message
     * @param cause the failure cause reported by the framework
     */
    public AllureResultsWriteException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
