# Project Guide

Never create pull requests or push git branches without explicit confirmation from the user.

## Test Work

Use [Allure Test Agent](docs/allure-test-agent.md) for test-related work in this repository.

- Read `docs/allure-test-agent.md` before designing, writing, reviewing, validating, debugging, or enriching tests.
- Use the `$allure-test-agent` skill as the durable behavior guide when it is installed; this project file contains local commands and conventions.
- If a command executes tests and its result will be used for smoke checking, reasoning, review, coverage analysis, debugging, or a user-facing conclusion, run it through `allure agent`.
- Use agent-mode execution for smoke checks too, even when the change is small or mechanical.
- If agent output is missing or incomplete, debug that first and treat console-only conclusions as provisional.

## Validation

After making changes, run the applicable validation checks before reporting done.

- For Java, Groovy, or Scala production/test changes, run the relevant module-scoped quality checks when practical: `:<module>:spotlessCheck`, `:<module>:checkstyleMain`, `:<module>:pmdMain`, and `:<module>:spotbugsMain`.
- For shared test-support, build logic, root configuration, or broad cross-module changes, run the aggregate quality command: `./gradlew --no-daemon spotlessCheck checkstyleMain pmdMain spotbugsMain`.
- If Spotless reports formatting issues, run the matching `spotlessApply` task, such as `:<module>:spotlessApply` or `./gradlew --no-daemon spotlessApply`, then rerun `spotlessCheck`.
- If a quality check fails, fix the smallest relevant issue and rerun the failed check or the original quality command until it passes.
- For docs-only changes, run `git diff --check` and any configured documentation lint before reporting completion.
- If validation cannot be run, state exactly which checks were skipped and why.
