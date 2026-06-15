# Allure Test Agent

Use Allure agent mode to design, review, validate, debug, and enrich tests in this project.

This file is project-specific guidance for `allure-java`. Durable test-design, expectation, and evidence rules live in the `allure-test-agent` skill. If that skill is available, use it together with this file. If it is unavailable, follow this file as the local fallback and keep conclusions conservative.

## Review Principle

Runtime first, source second.

- If a command executes tests and its result will be used for smoke checking, reasoning, review, coverage analysis, debugging, or any user-facing conclusion, run it through `allure agent`.
- Use agent-mode execution for smoke checks too, even when the change is small or mechanical.
- Only skip agent mode when it is impossible or when debugging agent mode itself.
- If agent output is missing or incomplete, debug that first and treat console-only conclusions as provisional.

## Local Capability Snapshot

Refresh this section when Allure, Gradle, test runners, Allure results paths, report generation, CI, or project wrappers change. Confirm local support with `allure --version`, `allure agent --help`, and `allure agent capabilities` before using optional agent-mode commands or flags.

- Allure wrapper: local `allure`; CI uses `npx -y allure@3 run` for the reporting flow.
- Capability snapshot last checked: 2026-06-15 from the local CLI help and capability map.
- Agent execution: supported with the current local CLI surface. If `allure --version` reports a version lower than `3.11.0`, treat the full runtime-evidence workflow as unsupported or limited until the CLI is upgraded.
- Output option: automatic temp output is supported by default; explicit output is `--output <dir>`.
- Expectation controls: inline `--goal`, `--task-id`, `--expect-tests`, `--expect-label`, `--expect-env`, `--expect-test`, `--expect-prefix`, `--forbid-label`, `--expect-step-containing`, `--expect-steps`, `--expect-attachments`, and `--expect-attachment` are supported; YAML or JSON files are supported with `--expectations <file>`.
- Latest/state directory recovery: `allure agent latest [--cwd <dir>]` and `allure agent state-dir [--cwd <dir>]` are supported. `ALLURE_AGENT_STATE_DIR=<dir>` overrides the state directory.
- Selection/rerun support: `allure agent select --latest`, `allure agent select --from <output-dir>`, `allure agent --rerun-latest -- <command>`, and `allure agent --rerun-from <output-dir> -- <command>` are supported.
- Query support: `allure agent query --latest summary`, `tests`, `findings`, and `test` views are supported, with filters reported by `allure agent capabilities`.
- Unsupported by the local capability map: discovery/configuration helpers, execution-signal detection, compare/flaky/duplicates/stale/suppressions/observe/interrupt/service helpers, and `--expect-evidence`.

## Local Test Surfaces

- Project wrapper: `./gradlew`.
- Test runner surface: Gradle `Test` tasks. The root build configures library modules to use JUnit Platform with the Allure Gradle adapter; adapter autodetection is disabled.
- Test roots: module `src/test/java`, `src/test/groovy`, `src/test/scala`, and `src/test/resources`; `allure-citrus/examples/**` has additional example builds and result directories when those example builds are executed.
- Main test frameworks and fixtures present in the repo: JUnit Platform/Jupiter, JUnit 4, TestNG, Spock 2, ScalaTest, Cucumber 7 JVM, JBehave 5, Karate, Citrus, Awaitility, AssertJ, Hamcrest, JsonUnit, REST Assured, HTTP client integrations, gRPC, Playwright, Selenide, Selenium BiDi, servlet API, Spring Web, and jOOQ.
- Allure results paths: module-local `build/allure-results` from `src/test/resources/allure.properties`.
- Generated report path in CI/report config: `build/allure-report`.
- Known Gradle selector support: use a module task such as `:allure-jupiter:test`; Gradle `--tests` may be used with class or method patterns for focused `Test` tasks.
- Known environments or services needed for tests: some modules start local fixtures such as WireMock, gRPC mock servers, embedded Postgres, Selenium/Testcontainers, Playwright Chromium, or framework-specific sample runners. Treat missing local services/browsers as environment limits and make skips or setup failures visible.

## Allure Integrations

- Gradle plugin: the root build applies `io.qameta.allure`; library modules also apply it.
- Gradle adapter config: `adapter.autoconfigure=false`, `aspectjWeaver=true`, and only the JUnit Platform framework is enabled for Gradle test execution with listener autoconfiguration.
- Result-path configuration: module `src/test/resources/allure.properties` files set `allure.results.directory=build/allure-results`.
- Project labels: most modules set `allure.label.epic=#project.description#` and `allure.label.module=<module-name>` in `allure.properties`.
- Issue-link patterns: some modules define `allure.link.issue.pattern=https://github.com/allure-framework/allure-java/issues/{}`.
- Report config: `allurerc.mjs` configures the generated report name, output, grouping by module, publishing, and optional Allure Service token.
- CI Allure execution: `.github/workflows/build.yml` runs `npx -y allure@3 run --config ./allurerc.mjs --rerun 2 --environment=<matrix-env> --dump=<dump-name> -- ./gradlew --no-build-cache cleanTest test`.
- CI report generation: `.github/workflows/build.yml` downloads Allure dumps, runs `npx -y allure@3 generate --config ./allurerc.mjs --dump="allure-results-*.zip" --output=./build/allure-report`, and posts a PR summary for same-repository pull requests.

## Project Test-Design Conventions

- Prefer focused regular tests over dynamic factories when a factory hides many behavioral checks. Split distinct report-behavior assertions into separate tests when that makes runtime evidence easier to review.
- Keep assertions readable: introduce named local variables for results, steps, attachments, and payloads before asserting. Avoid deeply chained expressions such as `assertThat(runSomething().get(0).getSteps().get(3).getName())`.
- For tests that need an Allure description, put a high-level, consumer-oriented intent in the method Javadoc and add a bare `@Description` annotation. An empty `@Description` enables the Javadoc description for the annotated method.
- Test descriptions should state the behavior or feature being verified, not the implementation steps. They must be detailed enough for an agent to compare runtime evidence against the intended behavior.
- Keep intent metadata inline with the test method. Reusable helpers may handle mechanics, but should not hide descriptions, labels, links, parameters, or intent-defining step names behind lookup tables keyed by test name.
- Use AssertJ for fluent assertions where existing tests do so; JUnit assertions are also present and acceptable when they match nearby style.
- Use parameterized or dynamic tests only when each visible case remains understandable in the runner and Allure evidence.
- When tests intentionally skip, use the framework's visible skip/assumption mechanism, such as JUnit assumptions, `@Disabled`, JUnit 4 `@Ignore`, ScalaTest ignore, or the framework-specific equivalent already used nearby.

## Run Profiles

Use `allure agent` output defaults unless you need an explicit one-off path to compare grouped runs. Agent output is not the same thing as framework `build/allure-results`.

| Profile | Command or profile intent | Expected use | Confidence limits |
| --- | --- | --- | --- |
| focused test | `allure agent --goal "<claim>" -- ./gradlew --no-daemon :<module>:test --tests <class-or-method-pattern>` | Validate one class, method, or narrowly scoped behavior | Only proves the selected Gradle test scope |
| module | `allure agent --goal "<claim>" -- ./gradlew --no-daemon :<module>:test` | Validate one module after localized changes | Does not cover cross-module impact |
| module with fresh execution | `allure agent --goal "<claim>" -- ./gradlew --no-daemon --rerun-tasks :<module>:test` | Recheck a module when Gradle up-to-date state could hide runtime evidence | Still limited to the module |
| full local tests | `allure agent --goal "full local test run" -- ./gradlew --no-build-cache cleanTest test` | Broad repo test validation | Can be expensive; local environment may differ from CI |
| CI-like tests | Follow `.github/workflows/build.yml`: build without tests, then run the Allure-wrapped clean test task with CI exclusions where applicable | Reproduce the main CI test shape | JDK matrix and CI exclusions matter |

For non-test quality checks such as `spotlessCheck`, `checkstyleMain`, `pmdMain`, or `spotbugsMain`, run the command normally unless it executes tests or its test evidence will be part of the conclusion.

## Execution Signal And CI Trust

- Default local test command: `./gradlew test`; use a narrower module task whenever possible.
- CI build job: `.github/workflows/build.yml` runs on pull requests, pushes to `main` and `hotfix-*`, and manual dispatch.
- CI Java matrix: JDK 17 and JDK 25.
- CI command exclusions: on JDK 17, the workflow excludes `:allure-jooq` and `:allure-karate` build/test tasks.
- CI tests: `Run tests with Allure` is not marked `continue-on-error`; it runs with `if: always()` after the build step and produces Allure dumps.
- CI report job: runs with `if: always()` and downloads artifacts with `continue-on-error`; do not treat a report-posting result alone as proof that tests passed.
- CI gating status: branch protection requirements are unknown from the repository files. Do not claim a PR is gated unless GitHub branch protection confirms it.
- Known skipped/ignored tests exist in framework feature samples and environment-dependent modules. Treat visible skipped tests as part of the execution signal, not as passed coverage.

If CI or local execution excludes important tests, swallows failures, skips for environment reasons, or only reports advisory output, call that out before using the run as proof.

## Local Expectation Controls

Before each validation run, decide whether expectations reduce a real risk for the intended conclusion. When they do, use the smallest fresh inline options supported by local `allure agent --help`.

- Supported expectation mechanism: inline CLI options and YAML/JSON files via `--expectations <file>`.
- Exact test/file/suite/label/profile support: full test names, full-name prefixes, labels, environments, and test count are supported by agent expectations; Gradle class or method selection is handled by the Gradle command.
- Excluded-scope controls: forbidden labels are supported; forbidden environments, full names, and prefixes are not reported as supported.
- Evidence expectation controls: minimum steps, minimum attachments, step-name substrings, and attachment filters by name or content type are supported.
- Broad-audit fallback: when exact expectations are not practical, set a clear `--goal`, run the narrowest practical Gradle scope, review observed scope from manifests, and state what the run can and cannot prove.

Example focused run:

```bash
allure agent \
  --goal "verify Awaitility listener step reporting" \
  --expect-prefix "io.qameta.allure.awaitility." \
  --expect-step-containing "Awaitility:" \
  -- ./gradlew --no-daemon :allure-awaitility:test
```

Use `--expectations <file>` only when the contract is too large, generated, or policy-controlled.

## Core Loops

### Test Review Loop

1. Identify the exact review scope and validation depth.
2. Create the smallest meaningful expectations using local supported controls when they protect the review conclusion.
3. Run only that scope through `allure agent`.
4. Print the run's `index.md` path.
5. Review `index.md`, `manifest/run.json`, `manifest/test-events.jsonl`, `manifest/tests.jsonl`, `manifest/findings.jsonl`, and relevant per-test markdown.
6. Inspect source code only after runtime evidence explains what executed.
7. Call out weak scope, weak evidence, execution-signal limits, or partial runtime modeling.

### Test Authoring Loop

1. Understand the feature, issue, expected behavior, and risk.
2. Read the `allure-test-agent` skill's test-design guidance when available.
3. Create the smallest meaningful expectations for the intended scope when they reduce a real validation risk.
4. Write or update focused tests without weakening useful coverage.
5. Run the intended scope through agent mode.
6. Review scope, checks, evidence, and execution signal before claiming validation.
7. Enrich tests when evidence is weak, then rerun with fresh agent output.

### Evidence And Metadata Enrichment Loop

1. Identify weak evidence, missing checks, missing setup state, missing artifacts, or noisy metadata.
2. Prefer framework integrations and helper-boundary instrumentation over wrapping every line.
3. Add useful steps, attachments, parameters, descriptions, labels, or links using project conventions.
4. Redact sensitive values while preserving useful artifact shape.
5. Rerun the same intended scope and report evidence changes.

## Runtime Artifact Review

After each agent-mode run:

- print the run's `index.md` path
- read `manifest/run.json`
- read `manifest/test-events.jsonl`
- read `manifest/tests.jsonl`
- read `manifest/findings.jsonl`
- read relevant per-test markdown before inspecting source
- inspect global stderr/log artifacts when runner-visible failures are not represented as logical tests

Use focused query helpers when they make review faster:

```bash
allure agent latest
allure agent query --latest summary
allure agent query --latest tests
allure agent query --latest findings
```

## Output, State, And Reruns

Do not create persistent agent output or expectation paths in the repository. Modern `allure agent` creates and prints a temp output directory when no output is provided; use that default unless a specific temporary path is needed.

- Agent output policy: CLI-provided temp directory by default; one-off explicit paths use `--output <dir>`.
- Framework results policy: module `build/allure-results` directories are stable Allure adapter outputs and are separate from agent-mode output.
- Latest output recovery: `allure agent latest`.
- State directory override: `ALLURE_AGENT_STATE_DIR=<dir>`.
- Rerun from latest/prior output: `allure agent --rerun-latest -- <command>` or `allure agent --rerun-from <output-dir> -- <command>`.
- Selection/test plan support: `allure agent select --latest` or `--from <output-dir>`; rerun transport uses `ALLURE_TESTPLAN_PATH`.
- Parallel-run rule: output paths and expectation state must not be shared.
- CI artifact retention: CI retains Allure dump zip artifacts and generates `build/allure-report`; agent-mode output retention is not configured in CI.

## Project Metadata Conventions

- Module metadata: module `allure.properties` files set `allure.label.module=<module-name>`.
- Epic metadata: most module `allure.properties` files set `allure.label.epic=#project.description#`.
- Issue links: use `@Issue` where nearby tests use it; configured issue-link patterns point to GitHub issues in some modules.
- Feature/story/owner/severity metadata: present in framework metadata tests and sample fixtures. Do not add decorative labels to unrelated tests just to fill report fields.
- Test descriptions: for Java tests that should expose a description, prefer method Javadoc plus bare `@Description` so Javadoc is the single source of descriptive intent.
- Parameter naming and dynamic-history exclusions: no broad repository convention confirmed beyond framework-specific tests.

## Project Evidence Conventions

- Steps: use `Allure.step` around meaningful setup, actions, state transitions, and verification points. Avoid empty wrapper steps.
- Attachments: HTTP exchange, gRPC exchange, JSON patch, Cucumber text/image attachments, and integration-specific artifacts are verified in modules that produce them.
- Attachment assertions: shared test support exposes recursive attachment metadata and content helpers through `AllureResults`.
- Step naming: use behavior-oriented names that explain what is being executed or verified, such as `Execute ... and collect Allure results` or `Verify ... evidence`.
- Assertion/check visibility: prefer assertion steps or clearly named verification steps when evidence would otherwise be hard to understand from the report.
- Fixture/setup evidence: add visible setup steps when the setup matters to the behavior under test.
- Sensitive data redaction: integration-specific redaction tests exist for some HTTP headers; broader repository policy is unknown.

## Acceptance Rules

Accept a run only when:

- observed scope matches the intended scope, or drift is explained
- coverage remains meaningful for the stated conclusion
- important checks are visible through supported reporting, documented step-name conventions, or source review covers the gap
- evidence is strong enough to explain what happened
- execution-signal limits are explicit
- no high-confidence placeholder or noop evidence findings remain
- partial runtime modeling is called out

Console-only conclusions are provisional when agent output is absent or incomplete.
