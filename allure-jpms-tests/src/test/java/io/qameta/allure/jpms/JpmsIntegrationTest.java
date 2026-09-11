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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class JpmsIntegrationTest {

    @TempDir
    Path directory;

    /**
     * A JUnit 4 consumer can register the adapter from a named module and retain test metadata,
     * steps, and attachments without opening public test classes for deep reflection.
     */
    @Test
    @Description
    void shouldReportJunit4FromNamedModule() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "junit4");
        final String dependencies = String.join(
                File.pathSeparator, coreDependencies(consumer),
                consumer.jars("allure-junit4", "junit", "hamcrest")
        );
        runFixture(consumer, dependencies, true);
        final JsonNode result = readOnlyResult(consumer.results(), "-result.json");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(labels(result)).contains(tuple("owner", "module owner"));
        assertThat(result.path("steps").get(0).path("name").asText()).isEqualTo("operation");
        assertAttachment(consumer.results(), result, "JUnit 4 module payload");
    }

    /**
     * A named JVM consumer can call the Kotlin extensions and coroutine context API. Restoring the
     * captured context on another thread must preserve ownership of its Kotlin step and attachment.
     */
    @Test
    @Description
    void shouldReportThroughKotlinModules() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "kotlin");
        final String dependencies = String.join(
                File.pathSeparator, coreDependencies(consumer),
                consumer.jars(
                        "allure-kotlin-extensions", "allure-kotlin-coroutines", "kotlin-stdlib",
                        "kotlinx-coroutines-core-jvm"
                )
        );
        runFixture(consumer, dependencies, true);
        final JsonNode result = readOnlyResult(consumer.results(), "-result.json");
        assertThat(result.path("name").asText()).isEqualTo("Kotlin modules");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        final JsonNode steps = result.path("steps");
        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).path("name").asText()).isEqualTo("Kotlin operation");
        assertAttachment(consumer.results(), result, "Kotlin module payload");
    }

    /**
     * A named JsonUnit consumer can compare different JSON documents and render its HTML attachment
     * through Jackson and FreeMarker, including reflective access to the public difference model.
     */
    @Test
    @Description
    void shouldRenderJsonDifferenceFromNamedModule() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "jsonunit");
        final String dependencies = String.join(
                File.pathSeparator, coreDependencies(consumer),
                consumer.jars(
                        "allure-jsonunit", "json-unit", "json-unit-core", "hamcrest", "opentest4j",
                        "jackson-annotations", "jackson-core", "jackson-databind", "freemarker"
                )
        );
        runFixture(consumer, dependencies, true);
        final JsonNode result = readOnlyResult(consumer.results(), "-result.json");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        final List<JsonNode> attachments = result.findValues("attachments").stream()
                .flatMap(node -> java.util.stream.StreamSupport.stream(node.spliterator(), false)).toList();
        assertThat(attachments).hasSize(1);
        final JsonNode attachment = attachments.get(0);
        assertThat(attachment.path("name").asText()).isEqualTo("JSON difference");
        assertThat(attachment.path("type").asText()).isEqualTo("text/html");
        final String html = Files.readString(consumer.results().resolve(attachment.path("source").asText()));
        Allure.attachment("JSON difference", "text/html", html);
        assertThat(html).contains(
                "var left = {\"value\":2};", "var right = {\"value\":1};", "var delta = {\"value\":[1,2]};"
        );
    }

    /**
     * javac discovers the Javadoc processor on the processor module path using its provides
     * declaration, and writes the rendered description for an annotated method in a named module.
     */
    @Test
    @Description
    void shouldDiscoverModularAnnotationProcessor() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "processor");
        final String dependencies = coreDependencies(consumer);
        final String processor = consumer.jars("allure-descriptions-javadoc");
        final List<String> arguments = new ArrayList<>(
                List.of(
                        "--release", "17", "--module-path", dependencies,
                        "--processor-module-path", processor, "-d", "classes"
                )
        );
        arguments.addAll(consumer.sources(true));
        consumer.javac(arguments.toArray(String[]::new));
        final List<Path> descriptions;
        try (Stream<Path> files = Files.list(consumer.file("classes/META-INF/allureDescriptions"))) {
            descriptions = files.toList();
        }
        assertThat(descriptions).hasSize(1);
        final String description = Files.readString(descriptions.get(0));
        Allure.attachment("Generated description", "text/html", description);
        assertThat(description).contains("Describes a test compiled with a modular annotation processor.");
    }

    /**
     * A compiled Groovy specification runs in a named module and is reported through Spock's global
     * extension discovery. The SPI-off JAR must run the same feature with no Allure report generated.
     */
    @ParameterizedTest(name = "Spock module reporting, SPI off = {0}")
    @ValueSource(booleans = {false, true})
    @Description
    void shouldReportSpockFromNamedModule(final boolean spiOff) throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "spock");
        final String dependencies = String.join(
                File.pathSeparator, coreDependencies(consumer),
                consumer.jars(spiOff ? "allure-spock2:spi-off" : "allure-spock2"),
                consumer.jars(
                        "spock-core", "groovy", "junit-platform-engine", "junit-platform-commons",
                        "junit-platform-launcher", "apiguardian-api", "jspecify", "opentest4j", "hamcrest"
                )
        );
        consumer.java(
                "--class-path", dependencies, "org.codehaus.groovy.tools.FileSystemCompiler",
                "--classpath", dependencies, "-d", "classes", "src/fixture/tests/SampleTest.groovy"
        );
        final List<String> arguments = new ArrayList<>(
                List.of(
                        "--release", "17", "--module-path", dependencies,
                        "--patch-module", "allure.fixture=classes", "-d", "classes"
                )
        );
        arguments.addAll(consumer.sources(true));
        consumer.javac(arguments.toArray(String[]::new));
        consumer.java(
                "-Dallure.results.directory=results", "--module-path",
                "classes" + File.pathSeparator + dependencies, "--module", "allure.fixture/fixture.Main"
        );
        if (spiOff) {
            assertThat(resultFiles(consumer.results(), "-result.json")).isEmpty();
        } else {
            final JsonNode result = readOnlyResult(consumer.results(), "-result.json");
            assertThat(result.path("status").asText()).isEqualTo("passed");
            assertThat(labels(result)).contains(tuple("owner", "module owner"));
            assertAttachment(consumer.results(), result, "Spock module payload");
        }
    }

    /**
     * A named HTTP client consumer needs only the Allure integration in its descriptor. The public
     * JDK HTTP API and Allure API remain readable, and a real request produces a structured exchange.
     */
    @Test
    @Description
    void shouldCaptureHttpExchangeFromNamedModule() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "httpclient");
        final String dependencies = String.join(
                File.pathSeparator, coreDependencies(consumer), consumer.jars("allure-java-httpclient")
        );
        runFixture(consumer, dependencies, true);
        final JsonNode result = readOnlyResult(consumer.results(), "-result.json");
        assertThat(result.path("name").asText()).isEqualTo("modular HTTP");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        final JsonNode steps = result.path("steps");
        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).path("name").asText()).isEqualTo("HTTP exchange");
        final JsonNode attachments = steps.get(0).path("attachments");
        assertThat(attachments).hasSize(1);
        final JsonNode attachment = attachments.get(0);
        assertThat(attachment.path("type").asText()).isEqualTo("application/vnd.allure.http+json");
        final String exchange = Files.readString(consumer.results().resolve(attachment.path("source").asText()));
        Allure.attachment("HTTP exchange", "application/vnd.allure.http+json", exchange);
        final JsonNode payload = new ObjectMapper().readTree(exchange);
        assertThat(payload.path("request").path("method").asText()).isEqualTo("GET");
        assertThat(payload.path("response").path("status").asInt()).isEqualTo(200);
        assertThat(payload.path("response").path("body").path("value").asText()).isEqualTo("module response");
    }

    /**
     * A named consumer can use the core API without AspectJ or external Jackson. All four lifecycle
     * services are discovered from its descriptor and can modify the serialized report artifacts.
     */
    @Test
    @Description
    void shouldReportCoreLifecycleFromNamedModule() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "core");
        final String modulePath = coreDependencies(consumer);
        assertThat(modulePath).doesNotContain("aspectj", "jackson-");
        runFixture(consumer, modulePath, true);
        final Path results = consumer.results();

        final JsonNode result = readOnlyResult(results, "-result.json");
        assertThat(result.path("name").asText()).isEqualTo("core");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(result.path("steps").get(0).path("name").asText()).isEqualTo("step listener: operation");
        assertThat(labels(result)).contains(tuple("listener", "test"), tuple("custom service", "loaded"));
        assertAttachment(results, result, "module payload");
        final JsonNode container = readOnlyResult(results, "-container.json");
        assertThat(container.path("name").asText()).isEqualTo("container listener");
        assertThat(container.path("befores").get(0).path("description").asText()).isEqualTo("fixture listener");
    }

    /**
     * A minimal Java runtime can write Allure results, steps, and attachments without the desktop,
     * SQL, or XML modules used only by optional features of the bundled Jackson implementation.
     */
    @Test
    @Description
    void shouldReportWithoutOptionalJdkModules() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "core");
        final String dependencies = coreDependencies(consumer);
        compileFixture(consumer, dependencies, true);
        consumer.jlink("--add-modules", "java.base,java.logging,java.management", "--output", "runtime");
        final Path runtime = consumer.file("runtime");
        final String modules = consumer.java(runtime, "--list-modules");
        assertThat(modules.lines().map(line -> line.split("@")[0]).toList())
                .contains("java.base", "java.logging", "java.management")
                .doesNotContain("java.desktop", "java.sql", "java.xml");

        consumer.java(
                runtime, "-Dallure.results.directory=results",
                "--module-path", "classes" + File.pathSeparator + dependencies,
                "--module", "allure.fixture/fixture.Main"
        );
        final Path results = consumer.results();
        final JsonNode result = readOnlyResult(results, "-result.json");
        assertThat(result.path("name").asText()).isEqualTo("core");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(result.path("steps").get(0).path("name").asText()).isEqualTo("step listener: operation");
        assertAttachment(results, result, "module payload");
    }

    /**
     * Jupiter and the Allure extension discover one parameterized test through their normal SPIs.
     * Both launch modes preserve parameters, repeated custom annotations, steps, and attachments.
     */
    @ParameterizedTest(name = "Jupiter reporting, module path = {0}")
    @ValueSource(booleans = {true, false})
    @Description
    void shouldReportJupiter(final boolean modular) throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "jupiter");
        runFixture(consumer, jupiterDependencies(consumer, false), modular);
        final Path results = consumer.results();
        final JsonNode result = readOnlyResult(results, "-result.json");

        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(labels(result)).contains(
                tuple("owner", "module owner"), tuple("team", "first team"), tuple("team", "second team")
        );
        assertThat(result.path("parameters").findValuesAsText("value")).contains("argument value");
        assertThat(result.path("steps").get(0).path("name").asText()).isEqualTo("operation");
        assertAttachment(results, result, "argument value");
    }

    /**
     * Ordinary consumers can compile tests using the Jupiter extension without the launcher or
     * engine on their compile path. Those dependencies belong to the runner or a custom listener.
     */
    @ParameterizedTest(name = "Jupiter API without launcher, module path = {0}")
    @ValueSource(booleans = {true, false})
    @Description
    void shouldCompileJupiterConsumerWithoutLauncher(final boolean modular) throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "jupiter-api");
        final String dependencies = String.join(
                File.pathSeparator, coreDependencies(consumer),
                consumer.jars(
                        "allure-jupiter", "allure-junit-platform", "junit-jupiter-api",
                        "junit-platform-commons", "apiguardian-api", "opentest4j", "jspecify"
                )
        );
        compileFixture(consumer, dependencies, modular);
        assertThat(consumer.file("classes/fixture/SampleTest.class")).isRegularFile();
    }

    /**
     * TestNG discovers Allure through its listener SPI on either path. A private instance parameter
     * is included when the test package permits the adapter to access it.
     */
    @ParameterizedTest(name = "TestNG reporting, module path = {0}")
    @ValueSource(booleans = {true, false})
    @Description
    void shouldReportTestNg(final boolean modular) throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "testng");
        runFixture(consumer, testNgDependencies(consumer, false), modular);
        final Path results = consumer.results();
        final JsonNode result = readOnlyResult(results, "-result.json");

        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(labels(result)).contains(tuple("owner", "module owner"));
        assertThat(result.path("parameters").findValuesAsText("value")).contains("instance value");
        assertThat(result.path("steps").get(0).path("name").asText()).isEqualTo("operation");
        assertAttachment(results, result, "module payload");
    }

    /**
     * An inaccessible private instance parameter must not prevent TestNG from running the test or
     * Allure from writing its result. The diagnostic must identify the package that needs opening.
     */
    @Test
    @Description
    void shouldReportTestNgWithoutAccessToPrivateParameter() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "testng");
        final Path descriptor = consumer.file("src/module-info.java");
        Files.writeString(
                descriptor, Files.readString(descriptor)
                        .replace("org.testng, io.qameta.allure.testng", "org.testng")
        );
        final String output = runFixture(consumer, testNgDependencies(consumer, false), true);
        final Path results = consumer.results();
        final JsonNode result = readOnlyResult(results, "-result.json");

        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(result.path("parameters").findValuesAsText("value")).doesNotContain("instance value");
        assertThat(output)
                .contains("fixture.tests", "io.qameta.allure.testng");
    }

    /**
     * The SPI-off variants remain explicit named modules but do not automatically register Allure
     * listeners or extensions. The frameworks still execute their selected tests successfully.
     */
    @ParameterizedTest
    @ValueSource(strings = {"jupiter", "testng"})
    @Description
    void shouldDisableAutomaticRegistration(final String framework) throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, framework);
        final String dependencies = "jupiter".equals(framework)
                ? jupiterDependencies(consumer, true)
                : testNgDependencies(consumer, true);
        runFixture(consumer, dependencies, true);
        final Path results = consumer.results();

        assertThat(resultFiles(results, "-result.json")).isEmpty();
    }

    /**
     * Tests patched into an application module can use the same adapters without a second descriptor.
     * Runtime opens grant only the access needed by Jupiter and Allure.
     */
    @Test
    @Description
    void shouldReportTestsPatchedIntoAnApplicationModule() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "jupiter");
        final String dependencies = jupiterDependencies(consumer, false);
        final Path descriptor = consumer.file("src/module-info.java");
        Files.writeString(
                descriptor, Files.readString(descriptor)
                        .replace("    opens fixture.tests to org.junit.platform.commons, io.qameta.allure.commons, io.qameta.allure.jupiter;", "")
        );
        consumer.javac(
                "--release", "17", "-d", "application", "--module-path", dependencies,
                "src/module-info.java", "src/fixture/Main.java"
        );
        consumer.javac(
                "--release", "17", "-parameters", "-d", "tests",
                "--module-path", "application" + File.pathSeparator + dependencies,
                "--patch-module", "allure.fixture=src/fixture/tests", "src/fixture/tests/SampleTest.java"
        );
        consumer.java(
                "-Dallure.results.directory=results",
                "--module-path", "application" + File.pathSeparator + dependencies,
                "--patch-module", "allure.fixture=tests",
                "--add-opens", "allure.fixture/fixture.tests=org.junit.platform.commons,io.qameta.allure.commons,io.qameta.allure.jupiter",
                "--module", "allure.fixture/fixture.Main"
        );
        final Path results = consumer.results();

        final JsonNode result = readOnlyResult(results, "-result.json");
        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(result.path("parameters").findValuesAsText("value")).contains("argument value");
        assertThat(labels(result)).contains(tuple("team", "first team"), tuple("team", "second team"));
        assertAttachment(results, result, "argument value");
    }

    /**
     * AspectJ can weave an explicit consumer module. Allure resolves a private argument field from
     * an opened package and writes the annotated step and attachment through its bundled serializer.
     */
    @Test
    @Description
    void shouldWeaveStepsAndAttachmentsOnModulePath() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "weaving");
        final String dependencies = coreDependencies(consumer) + File.pathSeparator + consumer.jars("aspectjrt");
        compileFixture(consumer, dependencies, true);
        Files.createDirectories(consumer.file("classes/META-INF"));
        Files.copy(consumer.file("src/META-INF/aop.xml"), consumer.file("classes/META-INF/aop.xml"));
        consumer.java(
                "-Dallure.results.directory=results", "-javaagent:" + consumer.jars("aspectjweaver"),
                "--add-modules", "jdk.unsupported,java.sql",
                "--module-path", "classes" + File.pathSeparator + dependencies,
                "--module", "allure.fixture/fixture.Main"
        );
        final Path results = consumer.results();
        final JsonNode result = readOnlyResult(results, "-result.json");

        assertThat(result.path("status").asText()).isEqualTo("passed");
        final JsonNode steps = result.path("steps");
        assertThat(steps).as("Woven steps reported by the AspectJ agent").isNotEmpty();
        assertThat(steps.get(0).path("name").asText()).isEqualTo("operation for module user");
        assertAttachment(results, result, "woven payload");
    }

    /**
     * Ordinary Jupiter tests do not need junit-jupiter-params on either path. The Allure extension
     * still captures before fixtures when the optional parameter support module is absent.
     */
    @Test
    @Description
    void shouldRunJupiterWithoutOptionalParameterModule() throws Exception {
        final JavaConsumer consumer = new JavaConsumer(directory, "jupiter-plain");
        final String dependencies = Arrays.stream(jupiterDependencies(consumer, false).split(File.pathSeparator))
                .filter(path -> !Path.of(path).getFileName().toString().startsWith("junit-jupiter-params-"))
                .collect(Collectors.joining(File.pathSeparator));
        runFixture(consumer, dependencies, true);
        final Path results = consumer.results();
        final JsonNode result = readOnlyResult(results, "-result.json");

        assertThat(result.path("status").asText()).isEqualTo("passed");
        assertThat(result.path("parameters")).isEmpty();
        assertAttachment(results, result, "module payload");
        final List<String> beforeSteps = new ArrayList<>();
        for (Path container : resultFiles(results, "-container.json")) {
            final String json = Files.readString(container);
            Allure.attachment(container.getFileName().toString(), "application/json", json);
            beforeSteps.addAll(new ObjectMapper().readTree(json).path("befores").findValuesAsText("name"));
        }
        assertThat(beforeSteps).contains("prepare operation");
    }

    private static String runFixture(final JavaConsumer consumer, final String dependencies,
                                     final boolean modular)
            throws Exception {
        compileFixture(consumer, dependencies, modular);
        final List<String> arguments = new ArrayList<>(
                List.of(
                        "-Dallure.results.directory=results",
                        modular ? "--module-path" : "--class-path", "classes" + File.pathSeparator + dependencies
                )
        );
        if (modular) {
            arguments.addAll(List.of("--module", "allure.fixture/fixture.Main"));
        } else {
            arguments.add("fixture.Main");
        }
        return consumer.java(arguments.toArray(String[]::new));
    }

    private static void compileFixture(final JavaConsumer consumer, final String dependencies,
                                       final boolean modular)
            throws Exception {
        final List<String> arguments = new ArrayList<>(
                List.of(
                        "--release", "17", "-g", "-parameters", "-d", "classes",
                        modular ? "--module-path" : "--class-path", dependencies
                )
        );
        arguments.addAll(consumer.sources(modular));
        consumer.javac(arguments.toArray(String[]::new));
    }

    private static String coreDependencies(final JavaConsumer consumer) throws Exception {
        return consumer.jars("allure-model", "allure-java-commons", "slf4j-api");
    }

    private static String jupiterDependencies(final JavaConsumer consumer, final boolean spiOff) throws Exception {
        final String classifier = spiOff ? ":spi-off" : "";
        return String.join(
                File.pathSeparator,
                coreDependencies(consumer),
                consumer.jars("allure-jupiter" + classifier, "allure-junit-platform" + classifier),
                consumer.jars(
                        "junit-jupiter-api", "junit-jupiter-engine", "junit-jupiter-params",
                        "junit-platform-commons", "junit-platform-engine", "junit-platform-launcher",
                        "apiguardian-api", "opentest4j", "jspecify"
                )
        );
    }

    private static String testNgDependencies(final JavaConsumer consumer, final boolean spiOff) throws Exception {
        return String.join(
                File.pathSeparator,
                coreDependencies(consumer),
                consumer.jars(spiOff ? "allure-testng:spi-off" : "allure-testng"),
                consumer.jars("testng", "jcommander", "jquery", "slf4j-simple")
        );
    }

    private static List<Path> resultFiles(final Path results, final String suffix) throws IOException {
        try (Stream<Path> files = Files.list(results)) {
            return files.filter(path -> path.getFileName().toString().endsWith(suffix))
                    .collect(Collectors.toList());
        }
    }

    private static JsonNode readOnlyResult(final Path results, final String suffix) throws IOException {
        final List<Path> files = resultFiles(results, suffix);
        assertThat(files).as("Written Allure artifacts ending in %s", suffix).hasSize(1);
        final String json = Files.readString(files.get(0));
        Allure.attachment(files.get(0).getFileName().toString(), "application/json", json);
        return new ObjectMapper().readTree(json);
    }

    private static List<org.assertj.core.groups.Tuple> labels(final JsonNode result) {
        final List<org.assertj.core.groups.Tuple> labels = new ArrayList<>();
        result.path("labels").forEach(label -> labels.add(tuple(label.path("name").asText(), label.path("value").asText())));
        return labels;
    }

    private static void assertAttachment(final Path results, final JsonNode result,
                                         final String content)
            throws IOException {
        final List<JsonNode> attachments = result.findValues("attachments").stream()
                .flatMap(node -> java.util.stream.StreamSupport.stream(node.spliterator(), false))
                .collect(Collectors.toList());
        assertThat(attachments).hasSize(1);
        final JsonNode attachment = attachments.get(0);
        assertThat(attachment.path("name").asText()).isEqualTo("payload");
        assertThat(Files.readString(results.resolve(attachment.path("source").asText()))).isEqualTo(content);
    }
}
