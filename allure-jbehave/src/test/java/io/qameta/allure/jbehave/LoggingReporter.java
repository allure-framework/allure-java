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
package io.qameta.allure.jbehave;

import org.jbehave.core.model.GivenStories;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.NullStoryReporter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("unused")
class LoggingReporter extends NullStoryReporter {

    private final AtomicInteger indentSize = new AtomicInteger(0);

    @Override
    public void beforeStory(final Story story, final boolean givenStory) {
        log("beforeStory", story.getName(), givenStory);
    }

    @Override
    public void beforeGivenStories() {
        log("beforeGivenStories");
    }

    @Override
    public void givenStories(final GivenStories givenStories) {
        log("givenStories", givenStories.asString());
    }

    @Override
    public void afterGivenStories() {
        log("afterGivenStories");
    }

    @Override
    public void beforeScenario(final Scenario scenario) {
        log("beforeScenario", scenario.getTitle());
        indentSize.incrementAndGet();
    }

    @Override
    public void afterScenario() {
        log("afterScenario");
        indentSize.decrementAndGet();
    }

    @Override
    public void beforeStep(final String step) {
        log("step", step);
    }

    @Override
    public void example(final Map<String, String> tableRow, final int exampleIndex) {
        log("example", tableRow);
    }

    private void log(final Object... objects) {
        final int value = indentSize.get();
        final String string = Stream.concat(
                Stream.of(indent(value)),
                Stream.of(objects).map(Objects::toString)
        ).collect(Collectors.joining(" "));
        System.out.println(string);
    }

    private static String indent(final int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "    ")
                .collect(Collectors.joining());
    }
}
