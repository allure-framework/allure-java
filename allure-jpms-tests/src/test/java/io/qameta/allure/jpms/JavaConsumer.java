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
package io.qameta.allure.jpms;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A consumer workspace with its own sources, JARs, classes, results, and process logs.
 * Child processes receive only the arguments supplied by the test, without the host test classpath.
 */
final class JavaConsumer {

    private final Path directory;
    private int commandIndex;

    JavaConsumer(final Path directory, final String fixture) throws Exception {
        this.directory = directory;
        copyFixture(fixture);
        Files.createDirectories(file("lib"));
        Files.createDirectories(results());
    }

    Path file(final String path) {
        return directory.resolve(path);
    }

    Path results() {
        return file("results");
    }

    List<String> sources(final boolean modular) throws IOException {
        try (Stream<Path> sources = Files.walk(file("src"))) {
            return sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> modular || !path.getFileName().toString().equals("module-info.java"))
                    .map(directory::relativize).map(Path::toString).sorted().toList();
        }
    }

    @Step("Prepare consumer sources: {fixture}")
    private void copyFixture(final String fixture) throws Exception {
        final Path source = resource("fixtures/" + fixture);
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path input : paths.filter(Files::isRegularFile).sorted().toList()) {
                final Path relative = source.relativize(input);
                final Path target = file("src").resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(input, target);
                Allure.attachment("src/" + relative, "text/plain", Files.readString(target));
            }
        }
    }

    @Step("Prepare consumer JARs: {artifacts}")
    String jars(final String... artifacts) throws Exception {
        final List<String> paths = new ArrayList<>();
        for (String artifact : artifacts) {
            final boolean spiOff = artifact.endsWith(":spi-off");
            final String name = artifact.replace(":spi-off", "");
            final Pattern filename = Pattern.compile(Pattern.quote(name) + "-[0-9].*\\.jar");
            final List<Path> matches;
            try (Stream<Path> jars = Files.list(resource("jars"))) {
                matches = jars.filter(path -> filename.matcher(path.getFileName().toString()).matches())
                        .filter(path -> path.getFileName().toString().endsWith("-spi-off.jar") == spiOff).toList();
            }
            assertThat(matches).as("One built JAR for %s", artifact).hasSize(1);
            final Path jar = matches.get(0);
            assertThat(jar).isRegularFile();
            if (name.startsWith("allure-")) {
                final var modules = ModuleFinder.of(jar).findAll();
                assertThat(modules).hasSize(1);
                final var descriptor = modules.iterator().next().descriptor();
                assertThat(descriptor.isAutomatic()).as("Explicit module for %s", artifact).isFalse();
                if (spiOff) {
                    assertThat(descriptor.provides()).as("No service providers in %s", artifact).isEmpty();
                }
            }
            final Path target = file("lib").resolve(jar.getFileName());
            Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
            paths.add(directory.relativize(target).toString());
        }
        return String.join(File.pathSeparator, paths);
    }

    void javac(final String... arguments) throws Exception {
        execute("javac", Path.of(System.getProperty("java.home")), 0, arguments);
    }

    String javacFailure(final String... arguments) throws Exception {
        return execute("javac", Path.of(System.getProperty("java.home")), 1, arguments);
    }

    void jlink(final String... arguments) throws Exception {
        execute("jlink", Path.of(System.getProperty("java.home")), 0, arguments);
    }

    String java(final String... arguments) throws Exception {
        return java(Path.of(System.getProperty("java.home")), arguments);
    }

    String java(final Path runtime, final String... arguments) throws Exception {
        return execute("java", runtime, 0, arguments);
    }

    @Step("Run {tool}")
    private String execute(final String tool, final Path runtime, final int expectedExitCode,
                           final String... arguments)
            throws Exception {
        final List<String> command = new ArrayList<>();
        command.add(runtime.resolve("bin").resolve(tool).toString());
        command.addAll(List.of(arguments));
        final Path output = file(tool + "-" + ++commandIndex + ".log");
        Allure.attachment(
                output.getFileName() + " command", "text/plain",
                "Working directory: " + directory + "\nArguments (one per line):\n" + String.join("\n", command)
        );
        // Record the actual descriptor, including edits made by the scenario.
        if (Files.exists(file("src/module-info.java"))) {
            Allure.attachment("module-info.java", "text/plain", Files.readString(file("src/module-info.java")));
        }
        final ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        // Keep the parent Allure run's selection and result destination out of the child.
        builder.environment().remove("ALLURE_TESTPLAN_PATH");
        builder.environment().remove("ALLURE_RESULTS_DIRECTORY");
        // JVM options from the invoking shell must not silently open modules or attach agents.
        builder.environment().remove("JDK_JAVA_OPTIONS");
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("_JAVA_OPTIONS");
        builder.environment().remove("CLASSPATH");
        final Process process = builder.start();
        final boolean finished;
        try {
            finished = process.waitFor(60, TimeUnit.SECONDS);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
            Allure.attachment(output.getFileName().toString(), "text/plain", Files.readString(output));
        }
        final String log = Files.readString(output);
        assertThat(finished).as("Child process completed: %s", command).isTrue();
        assertThat(process.exitValue()).as("Child process output: %s", log).isEqualTo(expectedExitCode);
        return log;
    }

    private static Path resource(final String name) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(JavaConsumer.class.getResource("/" + name), name).toURI());
    }
}
