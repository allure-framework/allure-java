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
package io.qameta.allure.testfilter;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTestPlanSupplierTest {

    @Test
    void shouldReadPlanFromPrimaryEnvironmentVariable() throws Exception {
        final ProbeResult result = Allure.step("Run the supplier probe with ALLURE_TESTPLAN_PATH", () ->
                runProbe("ALLURE_TESTPLAN_PATH")
        );

        recordProbe(result);
        Allure.step("Verify the probe selects the configured test plan entry", () -> {
            assertEquals(0, result.exitCode);
            assertEquals("selected=true", result.stdout.strip());
        });
    }

    @Test
    void shouldReadPlanFromLegacyEnvironmentVariable() throws Exception {
        final ProbeResult result = Allure.step("Run the supplier probe with AS_TESTPLAN_PATH", () ->
                runProbe("AS_TESTPLAN_PATH")
        );

        recordProbe(result);
        Allure.step("Verify the legacy environment alias still loads the plan", () -> {
            assertEquals(0, result.exitCode);
            assertEquals("selected=true", result.stdout.strip());
        });
    }

    private ProbeResult runProbe(final String variableName) throws IOException, InterruptedException {
        final Path plan = Files.createTempFile("allure-testplan-", ".json");
        Files.writeString(
                plan,
                "{"
                + "\"version\":\"1.0\","
                + "\"tests\":[{\"id\":\"A-1\",\"selector\":\"pkg.Test#name\"}]"
                + "}",
                StandardCharsets.UTF_8
        );

        final String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final ProcessBuilder builder = new ProcessBuilder(
                javaBin,
                "-cp",
                System.getProperty("java.class.path"),
                SupplierProbe.class.getName()
        );
        final Map<String, String> environment = builder.environment();
        environment.remove("ALLURE_TESTPLAN_PATH");
        environment.remove("AS_TESTPLAN_PATH");
        environment.put(variableName, plan.toString());

        final Process process = builder.start();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("supplier probe timed out");
        }

        final String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProbeResult(process.exitValue(), stdout, stderr);
    }

    private void recordProbe(final ProbeResult result) {
        Allure.addAttachment("probe-stdout", result.stdout);
        Allure.addAttachment("probe-stderr", result.stderr);
    }

    private static final class ProbeResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ProbeResult(final int exitCode, final String stdout, final String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    public static final class SupplierProbe {
        private SupplierProbe() {
        }

        public static void main(final String[] args) {
            final FileTestPlanSupplier supplier = new FileTestPlanSupplier();
            final String result = supplier.supply()
                    .filter(TestPlanV1_0.class::isInstance)
                    .map(TestPlanV1_0.class::cast)
                    .map(plan -> "selected=" + plan.isSelected("A-1", "pkg.Test#name"))
                    .orElse("selected=false");
            System.out.println(result);
        }
    }
}
