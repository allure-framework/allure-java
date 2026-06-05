# allure-jupiter-assert

JUnit Jupiter assertion step integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-jupiter-assert`

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-jupiter")
    testImplementation("io.qameta.allure:allure-jupiter-assert")
    testRuntimeOnly("org.aspectj:aspectjrt:<aspectj-version>")
    aspectjAgent("org.aspectj:aspectjweaver:<aspectj-version>")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${aspectjAgent.singleFile}")
    }
}
```

## Use

Enable the AspectJ weaver for the test JVM. The module weaves JUnit Jupiter assertions and reports them as nested Allure steps.

## Notes

- Use this only when assertion-level reporting is desired. `allure-jupiter` is enough for ordinary JUnit Jupiter reporting.
- The old `allure-junit5-assert` alias is removed in Allure Java 3.x.
- For large classpaths, consider an AspectJ `META-INF/aop.xml` that limits weaving to Allure, JUnit, and your test packages.
