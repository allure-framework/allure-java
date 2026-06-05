# allure-hamcrest

Hamcrest assertion step integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-hamcrest`

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-hamcrest")
    testRuntimeOnly("org.aspectj:aspectjrt:<aspectj-version>")
    aspectjAgent("org.aspectj:aspectjweaver:<aspectj-version>")
}

tasks.test {
    doFirst {
        jvmArgs("-javaagent:${aspectjAgent.singleFile}")
    }
}
```

## Use

Enable the AspectJ weaver for the test JVM. The module weaves Hamcrest `MatcherAssert.assertThat(...)` calls and reports them as Allure steps.

## Notes

- Works with standard Hamcrest matchers and custom matchers with useful mismatch descriptions.
- Failed assertions update the Allure step status and status details.
- For large classpaths, limit weaving with `META-INF/aop.xml`.
