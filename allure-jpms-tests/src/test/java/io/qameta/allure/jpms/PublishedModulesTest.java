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
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PublishedModulesTest {

    /**
     * Every migrated library must publish a real descriptor under its established module name.
     * Public service providers must agree with classpath metadata, so switching launch modes preserves discovery.
     */
    @ParameterizedTest(name = "{0} publishes {1}")
    @CsvSource(
        {
                "allure-assertj, io.qameta.allure.assertj",
                "allure-awaitility, io.qameta.allure.awaitility",
                "allure-cucumber7-jvm, io.qameta.allure.cucumber7jvm",
                "allure-descriptions-javadoc, io.qameta.allure.description",
                "allure-grpc, io.qameta.allure.grpc",
                "allure-hamcrest, io.qameta.allure.hamcrest",
                "allure-httpclient, io.qameta.allure.httpclient",
                "allure-httpclient5, io.qameta.allure.httpclient5",
                "allure-java-commons, io.qameta.allure.commons",
                "allure-java-commons-test, io.qameta.allure.commonstest",
                "allure-java-httpclient, io.qameta.allure.javahttpclient",
                "allure-jax-rs, io.qameta.allure.jaxrs",
                "allure-jbehave5, io.qameta.allure.jbehave5",
                "allure-jsonunit, io.qameta.allure.jsonunit",
                "allure-junit-platform, io.qameta.allure.junitplatform",
                "allure-junit4, io.qameta.allure.junit4",
                "allure-junit4-aspect, io.qameta.allure.junit4aspect",
                "allure-jupiter, io.qameta.allure.jupiter",
                "allure-jupiter-assert, io.qameta.allure.jupiterassert",
                "allure-kotlin-coroutines, io.qameta.allure.kotlin.coroutines",
                "allure-kotlin-extensions, io.qameta.allure.kotlin.extensions",
                "allure-model, io.qameta.allure.model",
                "allure-okhttp3, io.qameta.allure.okhttp3",
                "allure-playwright, io.qameta.allure.playwright",
                "allure-rest-assured, io.qameta.allure.restassured",
                "allure-selenium-bidi, io.qameta.allure.seleniumbidi",
                "allure-servlet-api, io.qameta.allure.servletapi",
                "allure-spock2, io.qameta.allure.spock2",
                "allure-spring-web, io.qameta.allure.springweb",
                "allure-testng, io.qameta.allure.testng"
        }
    )
    @Description
    void shouldPublishExplicitModule(final String artifact, final String moduleName) throws Exception {
        verifyDescriptor(artifact, moduleName, "published-jars", false);
    }

    /**
     * The jOOQ adapter retains its Java 21 baseline and publishes a descriptor with its established name.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    @Description
    void shouldPublishJooqModule() throws Exception {
        verifyDescriptor("allure-jooq", "io.qameta.allure.jooq", "published-jars", false);
    }

    /**
     * Alternate adapter JARs must keep the same named-module API while disabling service registration
     * on both paths, without accidentally inheriting the normal JAR's provides declarations.
     */
    @ParameterizedTest
    @ValueSource(strings = {"junit-platform", "jupiter", "testng", "spock2"})
    @Description
    void shouldPublishSpiOffModule(final String adapter) throws Exception {
        verifyDescriptor("allure-" + adapter, "io.qameta.allure." + adapter.replace("-", ""), "jars", true);
    }

    @Step("Inspect published module: {artifact}")
    private static void verifyDescriptor(final String artifact, final String moduleName,
                                         final String resources, final boolean spiOff)
            throws Exception {
        final Path directory = Path.of(
                Objects.requireNonNull(
                        PublishedModulesTest.class.getResource("/" + resources)
                ).toURI()
        );
        final Pattern name = Pattern.compile(Pattern.quote(artifact) + "-[0-9].*\\.jar");
        final List<Path> matches;
        try (Stream<Path> files = Files.list(directory)) {
            matches = files.filter(path -> name.matcher(path.getFileName().toString()).matches())
                    .filter(path -> path.getFileName().toString().endsWith("-spi-off.jar") == spiOff).toList();
        }
        assertThat(matches).as("Published JAR for %s", artifact).hasSize(1);
        final Path jarPath = matches.get(0);
        final var modules = ModuleFinder.of(jarPath).findAll();
        assertThat(modules).hasSize(1);
        final ModuleDescriptor descriptor = modules.iterator().next().descriptor();
        Allure.attachment("Module descriptor", "text/plain", descriptor.toString());
        assertThat(descriptor.name()).isEqualTo(moduleName);
        assertThat(descriptor.isAutomatic()).isFalse();
        assertThat(descriptor.exports()).as("Public API exports").isNotEmpty();
        final Map<String, List<String>> classpathProviders = new HashMap<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            final String automaticName = jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name");
            assertThat(automaticName).as("The descriptor replaces the automatic name").isNull();
            final var services = jar.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().startsWith("META-INF/services/"))
                    // Bundled Jackson's relocated SPI is encapsulated and is not an Allure extension point.
                    .filter(entry -> !entry.getName().startsWith("META-INF/services/io.qameta.allure.internal.shadowed."))
                    .toList();
            for (var service : services) {
                final String content;
                try (var input = jar.getInputStream(service)) {
                    content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                Allure.attachment(service.getName(), "text/plain", content);
                classpathProviders.put(
                        service.getName().substring("META-INF/services/".length()),
                        content.lines().map(line -> line.split("#", 2)[0].trim())
                                .filter(line -> !line.isEmpty()).sorted().toList()
                );
            }
        }
        final Map<String, List<String>> moduleProviders = descriptor.provides().stream().collect(
                Collectors.toMap(
                        ModuleDescriptor.Provides::service, provider -> provider.providers().stream().sorted().toList()
                )
        );
        assertThat(moduleProviders).as("Consistent module-path and classpath service discovery")
                .isEqualTo(classpathProviders);
        if (spiOff) {
            assertThat(moduleProviders).isEmpty();
        }
    }
}
