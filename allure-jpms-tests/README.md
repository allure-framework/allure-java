# Allure JPMS end-to-end tests

This is a test-only Gradle project. It has no Maven publication and is excluded from the Allure BOM.
`allure-java-commons-test` remains the reusable test-helper library.

Each JUnit test creates an isolated consumer in a temporary directory:

1. Copy a small Java project from `src/test/resources/fixtures` into `src`.
2. Copy the scenario's built Allure JARs and framework dependencies into `lib`.
3. Run `javac` and `java` with argument lists chosen by the Java test.
4. Check process exit codes and the generated Allure JSON, metadata, steps, and attachment contents.

`JavaConsumer` handles files, subprocess timeouts, and command/output attachments. It uses the JDK
running the tests; the minimal-runtime case uses `jlink` from that JDK and launches the resulting runtime.
Child processes have their own working directory and explicit classpath or module
path; they do not inherit the host test's classpath, JVM options, Allure selection, or result destination.
JUnit cleans up temporary directories, and Allure retains the sources, commands, logs, and checked JSON
in the test report.

Gradle only builds/resolves the input JARs, copies them into test resources, and runs JUnit. There are
no Gradle tasks for compiling or launching individual consumers and no scenario-specific system
properties. Dependency choices, module-path/classpath flags, `--patch-module`, `--add-opens`, and
AspectJ agent options live in `JpmsIntegrationTest`.

The consumer cases cover core serialization and service discovery, Jupiter and TestNG on both paths,
JUnit 4, Spock with normal and SPI-off JARs, Kotlin API/context propagation, JDK HTTP client capture,
JsonUnit rendering, modular annotation-processor discovery, denied private-field access, patched tests,
optional Jupiter parameter support, AspectJ weaving, and a runtime without `java.desktop`, `java.sql`, or `java.xml`.
Compilation cases also check that ordinary Jupiter consumers need neither launcher nor engine JARs on their compile path.

`PublishedModulesTest` additionally inspects all 31 modular published JARs and the four SPI-off JARs.
It checks stable names, explicit descriptors, public exports, and matching public service metadata on both paths.
The jOOQ artifact requires Java 21, so its packaging case is visibly disabled on Java 17 and its JAR is only
built for this suite on Java 21+. Other cases retain the Java 17 baseline.
See [module support](../docs/modules.md) for consumer configuration and scope.

Run from the repository root:

```sh
allure agent --goal 'Verify JPMS consumer compatibility' \
  --expect-prefix io.qameta.allure.jpms. \
  -- ./gradlew --no-daemon :allure-jpms-tests:cleanTest :allure-jpms-tests:test
```

Set `JAVA_HOME` to test another JDK. The project is also included in the normal Gradle `test` task,
so the existing Java 17 and 25 CI matrix runs these cases without a separate workflow.
