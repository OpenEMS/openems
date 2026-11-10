---
name: oe-review
description: Base review skill for FEMS/OpenEMS code reviews. Always apply CLEAN_CODE and add language-specific checks from JAVA.md and TYPESCRIPT.md.
---

## Scope

Limit the review to files changed or newly added on the current branch (compared to `develop`, the PR target branch). Use this exact command (do not use "cd" to navigate to the project directory, the shell is already in the project directory):

```powershell
git --no-pager diff develop...
```

- Review only changed/new files unless the user explicitly asks for a broader audit.
- Avoid unrelated refactor requests unless they are needed to prevent a bug.

Before starting, read `AGENTS.md` and `.github/skills/oe-review/CLEAN_CODE.md` (background: the published coding guidelines at https://openems.github.io/openems.io/openems/latest/development/coding-guidelines.html), then the language guide per changed file: `.java` -> `JAVA.md` (Java 21/25 features, Optional/Stream, concurrency, exceptions, checklist); `.ts`/`.tsx`/`.js`/`.jsx` -> `TYPESCRIPT.md` (type safety, strictness, async, immutability, lint/tests). Apply all relevant guides for multi-language changes.

If any changed file is under `ui/`, additionally read `ui/AGENTS.md` and verify every changed UI file against it, running the checks it prescribes and reporting any violation.

---

# OpenEMS Base Review Skill

Use this skill for all code reviews. It defines the default review process and routes to language-specific guidance.

## Review Priority (Highest to Lowest)

1. Correctness and behavioral regressions
2. Security and data integrity risks
3. Concurrency, performance, and resource usage
4. API/contract compatibility and error handling
5. Test coverage gaps for changed behavior
6. Readability and maintainability (Clean Code)

## Clean Code Baseline (Always Apply)

Apply `.github/skills/oe-review/CLEAN_CODE.md` to every code file. It adds review-specific depth on top of the baseline rules already defined in the repository `AGENTS.md` ("Working Style") - do not restate those here.

## Expected Review Output

- List findings first, ordered by severity.
- For each finding include:
    - file path
    - line number(s)
    - concise risk explanation
    - concrete recommendation
- If no issues are found, explicitly state that and mention any residual risk/testing gaps.
- Every file reference must be a clickable Markdown link to the file path, e.g. `[io.openems.edge.some.bundle/src/.../SomeImpl.java](io.openems.edge.some.bundle/src/.../SomeImpl.java)`.

### Java Review Summary

- At the end, provide a summary of all files with issues, severity, file, line numbers and recommended solutions.

| Severity | File | Line(s) | Problem | Recommended Solution |
|-|-|-|-|-|
| Bug risk | [`path/to/SomeImpl.java`](path/to/SomeImpl.java) | 108 | Short problem description | Short recommended fix |
