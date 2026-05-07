# Project Guide

Use [Allure Agent Mode](docs/allure-agent-mode.md) for all test-related work in this repository.

- Read `docs/allure-agent-mode.md` before designing, writing, reviewing, validating, debugging, or enriching tests.
- Run test-executing commands through `allure run`, including smoke checks after small edits.
- Use `./gradlew` for repo-local test commands and scope runs to the smallest relevant module or task.
- If agent-mode output is missing or incomplete, debug that first rather than relying on console-only conclusions.
