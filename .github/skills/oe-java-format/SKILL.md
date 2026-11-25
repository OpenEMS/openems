---
name: oe-java-format
description: Use when creating or modifying Java files in the OpenEMS workspace, to apply and verify Eclipse autoformatting and Organize Imports on the changed files.
---

# OpenEMS Java Format Skill

## Requirement

Every changed Java file must be equivalent to the output of Eclipse "Source > Format" plus "Source > Organize Imports", using the IDE's "Eclipse [built-in]" formatter profile.

## Trailing `//` Markers

A line ending in `//` is a hard line break for the JDT formatter — it will not join the next line. Add `//` only where you need to keep lines split (e.g. long builder chains); do not add it to every multi-line statement. Always preserve existing intentional `//` markers in files you touch. Applies to all Java files.

```java
MyConfig.create() //
		.setId("id0") //
		.setEnabled(true) //
		.build();
```

## Headless Application

Eclipse ships an official headless formatter application (`org.eclipse.jdt.core.formatterapp` bundle); it applies the same JDT formatter engine as the IDE:

```bash
eclipsec.exe -noSplash \
  -data <temporary-workspace-folder> \
  -application org.eclipse.jdt.core.JavaCodeFormatter \
  -config <repo-root>/cnf/formatter.properties \
  -quiet <java-files...>
```

- The workspace folder does not need to exist and can be deleted afterwards.
- The application rewrites files with the platform line delimiter (CRLF on Windows); restore line endings afterwards (e.g. `dos2unix <files>`).
- It formats code only, not imports. A file is conforming when formatting is a no-op on a copy and the import rules below hold.
- Without an Eclipse installation, the same engine can be driven via `org.eclipse.jdt.core.ToolFactory.createCodeFormatter` from the JDT jars.

## Import Rules (Organize Imports defaults)

JDT has no headless application for Organize Imports (it requires a live workspace with resolved classpath); apply these rules via the IDE action or manually.

- Static imports first, then regular imports.
- Order: `java`, `javax`, `org`, `com`, then all other packages alphabetically.
- No unused or wildcard imports.
