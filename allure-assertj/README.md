# allure-assertj

AssertJ assertion step integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-assertj`

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-assertj")
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

Enable the AspectJ weaver for the test JVM. The module weaves AssertJ calls and reports assertion chains as Allure steps.

## Notes

- Works with AssertJ `Assertions.assertThat(...)` and BDD `then(...)` entry points.
- Failed assertions update the corresponding Allure step status.
- For large classpaths, limit weaving with `META-INF/aop.xml`.
