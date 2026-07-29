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

import java.util.List;

/**
 * The interface for model objects that carry test metadata: labels, links, parameters, and description.
 *
 * <p>Shared by {@link TestResult} and {@link ScopeResult} so runtime APIs can apply metadata to whichever is the
 * current target without branching on the concrete type.</p>
 *
 * @see TestResult
 * @see ScopeResult
 * @since 3.0
 */
public interface WithMetadata extends WithLinks, WithParameters {

    /**
     * Gets labels.
     *
     * @return the labels
     */
    List<Label> getLabels();

    /**
     * Gets description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Sets description.
     *
     * @param value the description
     * @return self for chaining
     */
    WithMetadata setDescription(String value);

    /**
     * Gets HTML description.
     *
     * @return the HTML description
     */
    String getDescriptionHtml();

    /**
     * Sets HTML description.
     *
     * @param value the HTML description
     * @return self for chaining
     */
    WithMetadata setDescriptionHtml(String value);

}
