# Skipping muted tests

Supported test-runner adapters read `excludedTests` from the version 1.0 test plan referenced by
`ALLURE_TESTPLAN_PATH`. A match prevents the test body from executing; it is not just a reporting flag.
No TestOps connection or additional adapter registration is required.

```json
{
  "version": "1.0",
  "excludedTests": [
    { "id": 123 },
    { "selector": "com.example.PaymentTest.refund" }
  ]
}
```

```shell
ALLURE_TESTPLAN_PATH=/absolute/path/to/testplan.json ./gradlew test
```

## Adapter behavior

| Adapter | Matched test behavior |
| --- | --- |
| TestNG | Native skip; Allure result has `status: skipped` and `statusDetails.muted: true` |
| Spock 2 | Native abort per iteration; skipped-and-muted results retain iteration parameters |
| Cucumber JVM 7 and 8 | Automatic backend before hook aborts the scenario; skipped-and-muted result |
| Karate | Top-level scenario execution veto; skipped-and-muted result |
| JUnit 4 | Excluded by the existing Allure test-plan filter; no Allure result |
| JUnit Platform, including Jupiter | Excluded during discovery; no Allure result |
| ScalaTest, JBehave 5, Citrus | Not supported through the existing reporter/listener registration |

JUnit 4 still needs its normal test-plan filter registration, either explicit filtering or the
`allure-junit4-aspect` integration. JUnit Platform still needs its normal Allure post-discovery filter.
The Jupiter invocation extension alone does not provide muted-test execution control.

ScalaTest, JBehave, and Citrus reporting callbacks do not offer a safe execution veto. Their results
are not silently suppressed as a substitute for preventing execution. HTTP, browser, assertion, and
other non-runner integrations rely on their hosting test runner.

## Matching and selection

- `excludedTests` and the existing `tests` selection list are independent. A test must be selected before
  muting can affect its execution.
- An omitted, null, or empty `tests` list imposes no selection restriction, so muted-only plans work
  with `tests: []` too. Muting still applies independently.
- Cucumber and Karate use `excludedTests` without introducing ordinary `tests` selection in those adapters.
- IDs match IDs; selectors match selectors. Either field can match. An unmatched explicit ID does not
  prevent a matching selector from taking effect.
- Numeric IDs in JSON are normalized to strings. Null entries and missing or empty identities do not
  match. Selectors are exact strings, not patterns or prefixes.
- Existing missing/unreadable/invalid-plan and unknown-version handling is unchanged.
- The `@Muted` annotation and existing muted tags remain reporting metadata; they do not independently
  enable execution skipping without a matching test-plan entry.

Use the adapter's existing identity, normally the Allure result's `fullName`, as the selector:

| Adapter | Selector |
| --- | --- |
| JUnit 4, TestNG | Declaring class canonical name + `.` + method name |
| JUnit Platform | Existing source-derived full name or the platform unique ID |
| Spock 2 | Specification class name + `.` + feature name (not the expanded iteration title) |
| Cucumber JVM | Existing normalized scenario URI + `:` + scenario location line |
| Karate | Feature qualified name + `:` + scenario line |

Cucumber URI normalization is shared with reporting, including working-directory stripping. Outline
rows retain their existing scenario location identities. Karate matching applies to top-level
scenarios; called features continue to be reported and executed as nested steps.

## Lifecycle and exit behavior

Native skipped results preserve normal names, history identity, labels, and available parameters.
Framework setup, cleanup, dependency, and user-skip behavior still follows the runner's lifecycle;
muting is not a guarantee that every fixture is bypassed. Genuine fixture failures remain failures.
Cucumber's automatically discovered before hook is registered only when a known plan contains muted
entries. It runs at the earliest hook order and may appear as a fixture in Allure results.

Successful muted-only runs are supported without converting skips into failures. Native CLI exit
conventions are not normalized: for example, TestNG's standalone CLI can return its skipped-tests
status even when there are no failures. Gradle/Maven retain their normal handling of native skips;
any separate build-tool policy that rejects an empty discovered suite still applies to exclusion.

Classpath and JPMS consumers use the same matching rules. Cucumber's backend is registered through
both `META-INF/services` and its module descriptor. Cucumber 8 module-path launches may need
`--add-modules ALL-MODULE-PATH` to resolve upstream runtime modules such as tag expressions.
