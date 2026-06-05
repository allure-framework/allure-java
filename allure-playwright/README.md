# allure-playwright

Playwright Java integration for Allure Java.

## Coordinates

`io.qameta.allure:allure-playwright`

```kotlin
val aspectjAgent by configurations.creating

dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-playwright")
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

Enable the AspectJ weaver for automatic Playwright action steps. Use `AllurePlaywright` utility methods to register pages or contexts and attach screenshots, page source, traces, videos, console messages, and page errors.

```java
AllurePlaywright.register(page);
AllurePlaywright.attachScreenshot("Checkout page", page);
```

## Notes

- The AspectJ integration reports Playwright actions as Allure steps.
- Register pages or browser contexts when you want failure diagnostics attached automatically.
