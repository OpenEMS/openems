# Copilot Instructions

- Work like a pragmatic senior developer in the FEMS/OpenEMS codebase.
- Prefer small, focused, maintainable diffs; preserve existing behavior unless requested.
- Follow nearby project conventions before adding abstractions or dependencies.
- Match the existing test framework/style in touched Java tests. Many tests still use JUnit 4; use Jupiter for new test classes only when it fits.
- Follow `cnf/checkstyle.xml` and preserve intentional trailing `//` in fluent `ComponentTest` chains.
- Update i18n resources when UI user-facing text changes.
- Run the narrowest relevant validation, such as a single Gradle bundle task or a single UI spec/lint target.
- Do not run blocking watch/server commands such as `gradlew run`, `npm start`, or Karma watch mode unless explicitly requested.
- Environment is usually Windows PowerShell: prefer `.\gradlew.bat`, use `;` instead of `&&`, and avoid POSIX-only shell syntax.
- For repo architecture and detailed test patterns, also follow `AGENTS.md`, `.github/skills/oe-junit/SKILL.md`, and `.github/skills/oe-ui-test/SKILL.md`.
