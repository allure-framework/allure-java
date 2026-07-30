# allure-karate

Karate runtime listener integration for Allure Java.

Use this module when your API tests run on Karate 2 and you want Karate features, scenarios, steps, tags, and runtime attachments to appear in Allure Report.

## Supported Versions

- This module targets Karate 2.x.
- The current build validates against `io.karatelabs:karate-core:2.0.10`.
- Karate 2.0.10 is built for Java 21, so this module requires Java 21 or newer.

## Installation

Gradle:

```kotlin
dependencies {
    testImplementation(platform("io.qameta.allure:allure-bom:<allure-version>"))
    testImplementation("io.qameta.allure:allure-karate")
}
```

Maven, with `allure-bom` imported in dependency management:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-karate</artifactId>
    <scope>test</scope>
</dependency>
```

## Setup

Register `io.qameta.allure.karate.AllureKarate` as a Karate runtime listener:

```java
Runner.builder()
        .path("classpath:features")
        .listener(new AllureKarate())
        .parallel(4);
```

## Report Output

- One Allure test result for each top-level Karate scenario.
- Karate steps, including `call` and `callonce`.
- Called scenarios represented as nested steps under the calling step, including nested embeds and HTTP traffic.
- Tags mapped to Allure labels and links where supported.
- Runtime attachments produced by Karate steps, including attachments emitted by a failed step.
- Request and response data in Allure's rich HTTP exchange format.
- Karate 2 multipart embeds. Image comparison parts (`baseline`, `current`, and `diff`) use Allure's image-diff format; other inline parts, URL references, and metadata are preserved as attachments.

The listener is safe to reuse with Karate's parallel runner. Evidence remains associated with the scenario and step that produced it.

## Sensitive Output

The integration follows Karate's reporting privacy settings:

- A scenario tagged `@report=false` still produces its top-level Allure result and status, but its description, example parameters, steps, embeds, HTTP exchanges, and raw failure details are omitted. A failed scenario uses Karate's redacted failure message.
- Allure's standard HTTP redaction protects common authentication and cookie fields.
- A Karate `configure logging = { mask: ... }` configuration is also applied to HTTP exchange attachments. Header rules, JSON paths, regex patterns, custom replacements, and `enableForUri` are honored before the attachment is written.

For example:

```gherkin
* configure logging =
  """
  {
    mask: {
      headers: ['X-Api-Key'],
      jsonPaths: ['$.credentials.secret'],
      replacement: '***'
    }
  }
  """
```

## JUnit Platform

When `allure-junit-platform` and `allure-karate` are both present, the JUnit Platform listener ignores the dynamic nodes produced by Karate's legacy JUnit 5 annotation and the current `io.karatelabs.junit6.Karate.Test` annotation. The dedicated Karate listener remains the single source of Karate results, while ordinary Jupiter tests continue to be reported.

This duplicate suppression does not register the Karate runtime listener. Continue to configure `AllureKarate` through `Runner.builder().listener(...)` as shown above, or through an equivalent Karate runner configuration.
