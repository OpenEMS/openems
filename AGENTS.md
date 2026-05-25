# AGENTS.md

## Project Overview

OpenEMS (FEMS) is a modular energy management system:

- **Edge** (`io.openems.edge.*`) runs on site, controls devices, executes energy logic and runs mostly on low-power computers (1gb ram, 2 cores, 4gb disk).
- **Backend** (`io.openems.backend.*`) aggregates, monitors, and supports remote control.
- **UI** (`ui/`) is the Angular/Ionic frontend for real-time monitoring.

Main stacks: Java 21, Gradle, OSGi/bnd workspace, JUnit, Angular/Ionic.

## Working Style

- Prefer small, focused diffs and avoid unrelated reformatting.
- Prefer self-explanatory code with clear names and simple structure. Add comments only when they explain non-obvious intent, constraints, or tradeoffs. Comments should be only in English.
- Preserve existing behavior unless a change is explicitly requested.
- Follow nearby code and existing FEMS/OpenEMS utilities, modules, helper APIs, and configuration patterns before adding abstractions.
- Run the narrowest relevant validation for the changed area.
- Do not bump dependencies, wrappers, plugins, or introduce new frameworks unless explicitly requested.

## Java / Edge / Backend

- Bundles are top-level directories. Components use OSGi annotations such as `@Component`, `@Activate`, `@Reference`, `@Designate`, and config via `@ObjectClassDefinition`.
- Device capabilities are expressed through natures like `ManagedSymmetricEss`, `ElectricityMeter`, and `EssDcCharger`.
- Implementation classes must explicitly list all parent interfaces of child natures; otherwise OpenEMS nature detection does not work.
- Channels are the main runtime data exchange mechanism; prefer existing typed `ChannelId` patterns and static imports for channel IDs/constants.
- Edge logic is cycle/event driven via `EdgeEventConstants` topics. For controller changes, consider scheduler order, channel interactions, ESS/grid/PV/load energy flow, and production debugging.
- Modbus devices should use the existing `io.openems.edge.bridge.modbus` protocol/register mapping patterns.
- Checkstyle is enforced via `cnf/checkstyle.xml` with zero warnings.
- Add new bundles/components only when explicitly required; follow the closest existing bundle structure.
- Useful Java entry point bundles: `io.openems.edge.controller.ess.balancing`, `io.openems.edge.evse.chargepoint.keba`, and `io.openems.edge.evse.chargepoint.mennekes`; inspect matching `Config.java`, `*Impl.java`, and `*Test.java` files before copying patterns.

## UI

- For Angular/Ionic work under `ui/`, also follow `ui/AGENTS.md`.

## Tests

Use dedicated skills for detailed test-writing patterns:

- `.github/skills/oe-junit/SKILL.md` for Java Edge/Backend tests.
- `.github/skills/oe-ui-test/SKILL.md` for Angular/Ionic UI tests.

Java tests must match the framework and style of nearby tests. Many existing tests still use JUnit 4; use Jupiter for new test classes only when it fits. Do not mix JUnit 4 and Jupiter in the same file.

Preserve intentional trailing `//` markers in fluent `ComponentTest` chains.

## Validation

- Java: use the Gradle wrapper and prefer single-bundle tasks, for example `.\gradlew.bat :bundle.name:test` and `.\gradlew.bat :bundle.name:checkstyleMain` on Windows PowerShell.
- Java pre-PR checks: `.\gradlew.bat checkstyleAll`; use `.\gradlew.bat buildEdge` or `.\gradlew.bat buildBackend` when app assembly/resolve behavior is affected.
- Avoid blocking long-running/watch commands unless explicitly requested.
