# Project Guide

Never create pull requests or push git branches without explicit confirmation from the user.

## Test Work

Use [Allure Test Agent](docs/allure-test-agent.md) for test-related work in this repository.

- Read `docs/allure-test-agent.md` before designing, writing, reviewing, validating, debugging, or enriching tests.
- Use the `$allure-test-agent` skill as the durable behavior guide when it is installed; this project file contains local commands and conventions.
- If a command executes tests and its result will be used for smoke checking, reasoning, review, coverage analysis, debugging, or a user-facing conclusion, run it through `allure agent`.
- Use agent-mode execution for smoke checks too, even when the change is small or mechanical.
- If agent output is missing or incomplete, debug that first and treat console-only conclusions as provisional.
