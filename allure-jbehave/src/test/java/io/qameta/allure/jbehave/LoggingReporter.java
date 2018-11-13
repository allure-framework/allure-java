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
    public void example(final Map<String, String> tableRow) {
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
