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
package io.qameta.allure;

import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 *
 * @deprecated scheduled to remove in 3.0
 */
@Deprecated
public interface AllureResultsReader {

    Stream<TestResult> readTestResults();

    Stream<TestResultContainer> readTestResultsContainers();

    Stream<String> findAllAttachments();

    InputStream readAttachment(String source) throws IOException;

    List<ReadError> getErrors();

}
