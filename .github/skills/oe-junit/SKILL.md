---
name: oe-junit
description: Use when writing or fixing Java tests in the FEMS/OpenEMS OSGi/bnd workspace, including JUnit 4 and JUnit Jupiter tests.
---

# FEMS/OpenEMS Java Test Skill

Use this skill when writing or fixing Java Edge/Backend tests.

## Framework Choice

- Inspect nearby tests in the same class, bundle, or feature first.
- Match the existing framework and style when editing a test class.
- Many existing tests use JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`); keep that style in those files.
- Use JUnit Jupiter (`org.junit.jupiter.api.*`) for brand-new test classes when it fits the surrounding bundle.
- Do not mix JUnit 4 and Jupiter imports in the same test class.
- Do not add unnecessary test dependencies.

## Project Patterns

- Prefer existing FEMS/OpenEMS helpers over custom fixtures.
- Use `ComponentTest` patterns for Edge components where nearby tests do so.
- Build configs through existing `MyConfig.create().setId("id0").build()` style builders.
- Preserve intentional trailing `//` in fluent test chains such as `.next(new TestCase() //`.
- Keep tests small, deterministic, and close to the changed behavior.
- Avoid starting a full OSGi runtime unless the existing test style requires it.

## Controller Test Focus

For controller tests, check:

- scheduler/order side effects
- ESS/grid/PV/load sign conventions
- channel values and invalid/optional channel states
- min/max power edge cases
- config activation/update behavior

## Validation

Use the Gradle wrapper and the narrowest relevant task. On Windows PowerShell, prefer:

```powershell
.\gradlew.bat :bundle.name:test
.\gradlew.bat :bundle.name:checkstyleMain
```
