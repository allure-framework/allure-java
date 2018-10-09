package io.qameta.allure.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author charlie (Dmitry Baev).
 */
public final class DemoResults {

    public static final long FOUR_DAYS = TimeUnit.DAYS.toMillis(4);

    DemoResults() {
        throw new IllegalStateException("Do not instance");
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = Allure2ModelJackson.createMapper();
        TestResult testResult = randomTestResult();
        String string = mapper.writeValueAsString(testResult);
        System.out.println(string);
    }

    public static TestResult randomTestResult() throws IOException {
        long start = randomStart();
        return new TestResult()
                .withName(randomTestResultName())
                .withStart(start)
                .withStop(start + randomDuration());
    }

    public static StepResult randomStepResult() throws IOException {
        return new StepResult()
                .withName(randomStepResultName());
    }

    public static long randomStart() {
        return System.currentTimeMillis() - randomDuration();
    }

    public static long randomDuration() {
        return ThreadLocalRandom.current().nextLong(FOUR_DAYS);
    }

    public static String randomTestResultName() throws IOException {
        return randomLine("testNames.txt");
    }

    public static String randomStepResultName() throws IOException {
        return randomLine("stepNames.txt");
    }

    private static String randomLine(String resourceName) throws IOException {
        List<String> strings = readResource(resourceName);
        int index = ThreadLocalRandom.current().nextInt(strings.size());
        return strings.get(index);
    }

    private static List<String> readResource(String name) throws IOException {
        try (InputStream is = DemoResults.class.getClassLoader().getResourceAsStream(name)) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toList());
        }
    }
}
