# OpenEMS - AI Assistant Guide

This document provides comprehensive guidance for AI assistants working with the OpenEMS codebase. OpenEMS is a modular, open-source Energy Management System built on OSGi/Java for Edge and Backend components, with an Angular/Ionic-based UI.

## Table of Contents

- [Project Overview](#project-overview)
- [Repository Structure](#repository-structure)
- [Technology Stack](#technology-stack)
- [Build System](#build-system)
- [Development Workflows](#development-workflows)
- [Code Organization Patterns](#code-organization-patterns)
- [Testing Guidelines](#testing-guidelines)
- [Key Conventions](#key-conventions)
- [Common Tasks](#common-tasks)
- [Important Files](#important-files)

---

## Project Overview

**OpenEMS** - Open Source Energy Management System

### Components

1. **OpenEMS Edge** (189 modules)
   - Runs on-site at customer premises
   - Communicates with devices (batteries, inverters, meters, EV chargers)
   - Executes control algorithms
   - Fast, PLC-like control cycles (default: 1000ms)

2. **OpenEMS Backend** (18 modules)
   - Cloud/server-based aggregation service
   - Connects decentralized Edge systems
   - Provides monitoring and control via internet
   - Integrates with external systems (Odoo ERP, databases)

3. **OpenEMS UI**
   - Real-time web and mobile interface
   - Angular 20 + Ionic 8
   - Multi-platform: Web (PWA), Android, iOS

### License

- **Edge & Backend**: Eclipse Public License version 2.0 (EPL-2.0)
- **UI**: GNU Affero General Public License version 3 (AGPL-3.0)

### Community

- **Forum**: https://community.openems.io/
- **Documentation**: https://openems.github.io/openems.io/openems/latest/
- **Javadoc**: https://openems.github.io/openems.io/javadoc/
- **Organization**: OpenEMS Association e.V.

---

## Repository Structure

### Root Directory Layout

```
/home/user/openems/
├── io.openems.edge.*           # 189 Edge modules (device control, controllers, etc.)
├── io.openems.backend.*        # 18 Backend modules (cloud services)
├── io.openems.common           # Shared code between Edge & Backend
├── io.openems.shared.*         # Additional shared utilities (InfluxDB, etc.)
├── io.openems.wrapper.*        # Third-party library wrappers
├── io.openems.oem.*           # OEM customizations
├── ui/                        # Angular/Ionic UI application
├── doc/                       # Antora-based documentation
├── tools/                     # Build/deployment scripts
├── cnf/                       # Bnd/OSGi workspace configuration
├── .github/                   # CI/CD workflows
├── build.gradle               # Root Gradle build file
└── settings.gradle            # Gradle workspace settings
```

### Module Naming Convention

**Pattern**: `io.openems.{layer}.{category}.{implementation}`

**Examples**:
- `io.openems.edge.meter.carlo.gavazzi` - Carlo Gavazzi meter implementation
- `io.openems.backend.metadata.odoo` - Odoo metadata service
- `io.openems.edge.controller.ess.balancing` - ESS balancing controller
- `io.openems.common` - Shared across all layers

**Special Suffixes**:
- `*.api` - Nature definitions and interfaces
- `*.common` - Shared code within a category
- `*.core` - Central singleton services

### Edge Module Categories

**Core Infrastructure**:
- `io.openems.edge.application` - Main Edge application
- `io.openems.edge.common` - Shared Edge utilities
- `io.openems.edge.core` - Core services (ComponentManager, Cycle, Sum)

**Device Categories** (~80 modules):
- `io.openems.edge.battery.*` - Battery implementations (7 manufacturers)
- `io.openems.edge.batteryinverter.*` - Battery inverters (4 types)
- `io.openems.edge.ess.*` - Energy Storage Systems (7 variants)
- `io.openems.edge.evcs.*` - EV Charging Stations (13 vendors)
- `io.openems.edge.evse.*` - EV Supply Equipment (5 vendors)
- `io.openems.edge.meter.*` - Power meters (17 manufacturers)
- `io.openems.edge.pvinverter.*` - PV inverters (5 brands)
- `io.openems.edge.io.*` - IO devices (9 types)
- `io.openems.edge.heat.*` - Heat pump systems

**Controllers** (~60 modules) - Business logic:
- `io.openems.edge.controller.ess.*` - ESS control (balancing, peak shaving, grid optimization)
- `io.openems.edge.controller.io.*` - IO control (heating, digital/analog outputs)
- `io.openems.edge.controller.api.*` - API endpoints (Websocket, REST, MQTT, Modbus)
- `io.openems.edge.controller.symmetric.*` - Symmetric power control
- `io.openems.edge.controller.asymmetric.*` - Asymmetric phase control

**Support Services**:
- `io.openems.edge.bridge.*` - Protocol bridges (Modbus, HTTP, M-Bus, OneWire)
- `io.openems.edge.scheduler.*` - Controller execution order (4 types)
- `io.openems.edge.timedata.*` - Data persistence (InfluxDB, RRD4j)
- `io.openems.edge.timeofusetariff.*` - Dynamic pricing (11 providers)
- `io.openems.edge.predictor.*` - Forecasting (LSTM, persistence models)

### Backend Module Organization

- `io.openems.backend.application` - Main Backend application
- `io.openems.backend.common` - Shared Backend code
- `io.openems.backend.core` - Core Backend services
- **Communication**: `edgewebsocket`, `uiwebsocket`, `b2brest`, `b2bwebsocket`
- **Timedata**: `timedata.influx`, `timedata.timescaledb`, `timedata.aggregatedinflux`
- **Metadata**: `metadata.odoo`, `metadata.file`, `metadata.dummy`
- **Services**: `alerting`, `metrics.prometheus`

### UI Structure

```
ui/
├── src/
│   ├── app/
│   │   ├── edge/          # Edge device views (history, live, settings)
│   │   ├── user/          # User management
│   │   └── shared/        # Shared components, services, guards
│   ├── themes/            # UI theming
│   └── environments/      # Environment configurations
├── android/               # Android Capacitor app
├── ios/                   # iOS Capacitor app
└── package.json           # NPM dependencies
```

---

## Technology Stack

### Backend (Java)

- **Language**: Java 21 (source & target)
- **Framework**: OSGi R8 (Felix implementation)
- **Build**: Gradle 8.x + Bnd 7.1.0
- **Dependencies**: Maven Central via Bnd POM repository

**Core Libraries**:
- OSGi Core 8.0.0, Component Annotations 1.5.0
- Google Guava 33.5.0, Gson 2.13.2
- Pax Logging 2.2.1 (SLF4J)
- Apache Felix Jetty 12 (HTTP/WebSocket)
- Custom Modbus implementation
- Eclipse Paho (MQTT)

**Testing**:
- JUnit Jupiter 5.x
- Mockito, ByteBuddy
- JaCoCo (code coverage)

**Databases**:
- InfluxDB (primary time-series)
- TimescaleDB (alternative)
- RRD4j (embedded)

### Frontend (TypeScript)

- **Framework**: Angular 20.3.10 (standalone components)
- **UI**: Ionic 8.7.9
- **Mobile**: Capacitor 7.4.4
- **Language**: TypeScript 5.8.3

**Key Libraries**:
- Chart.js 4.5.1, ng2-charts, D3.js 7.9.0 (visualization)
- @ngx-formly 7.0.0 (forms)
- @ngx-translate 17.0.0 (i18n)
- date-fns 4.1.0 (date handling)

**Testing**:
- Jasmine 5.12.1, Karma 6.4.4

---

## Build System

### Gradle + Bnd Workspace

OpenEMS uses Gradle with the Bnd Workspace Plugin for OSGi bundle management.

**Key Configuration Files**:
1. `/build.gradle` - Root build configuration
2. `/settings.gradle` - Workspace settings
3. `/gradle.properties` - Build properties
4. `/cnf/build.bnd` - OSGi bundle defaults & dependencies
5. `/io.openems.{bundle}/bnd.bnd` - Per-bundle configuration

### Common Gradle Tasks

**Edge Operations**:
```bash
./gradlew buildEdge          # Build Edge Fat-JAR → build/openems-edge.jar
./gradlew assembleEdge       # Assemble Edge bundles
./gradlew testEdge           # Test Edge bundles only
./gradlew cleanEdge          # Clean Edge artifacts
```

**Backend Operations**:
```bash
./gradlew buildBackend       # Build Backend Fat-JAR → build/openems-backend.jar
./gradlew assembleBackend    # Assemble Backend bundles
./gradlew testBackend        # Test Backend bundles only
./gradlew cleanBackend       # Clean Backend artifacts
```

**Quality & Testing**:
```bash
./gradlew test               # Run all tests (Edge + Backend)
./gradlew checkstyleAll      # Run Checkstyle on all bundles
./gradlew jacocoTestReport   # Generate code coverage report
```

**Documentation**:
```bash
./gradlew buildAntoraDocs           # Build documentation site
./gradlew buildAggregatedJavadocs   # Build Javadoc
```

**UI Tasks**:
```bash
cd ui
npm install                  # Install dependencies
npm start                    # Development server (port 4200)
npm test                     # Run tests
npm run build                # Production build
npm run lint                 # Run ESLint
```

### Standard Bundle Structure

```
io.openems.edge.{category}.{implementation}/
├── bnd.bnd              # OSGi manifest & build config
├── .project             # Eclipse project file
├── .classpath           # Eclipse classpath
├── src/                 # Source code
│   └── io/openems/edge/{category}/{implementation}/
│       ├── {Component}Impl.java      # Main implementation
│       ├── Config.java               # OSGi configuration
│       └── package-info.java         # Package annotations
├── test/                # Unit tests
│   └── io/openems/edge/{category}/{implementation}/
│       └── {Component}Test.java
├── doc/                 # Documentation (optional)
│   └── readme.adoc
└── readme.adoc          # Bundle description
```

### Sample bnd.bnd

```properties
Bundle-Name: OpenEMS Edge Component Name
Bundle-Vendor: FENECON GmbH
Bundle-License: https://opensource.org/licenses/EPL-2.0
Bundle-Version: 1.0.0.${tstamp}

-buildpath: \
    ${buildpath},\
    io.openems.common,\
    io.openems.edge.common,\
    # Additional dependencies...

-testpath: \
    ${testpath}
```

---

## Development Workflows

### Setting Up Development Environment

**Java/OSGi Development**:
1. Install JDK 21 (Temurin recommended)
2. Install Eclipse IDE 4.30+ for Java
3. Install BndTools plugin from Eclipse Marketplace
4. Import workspace: File → Import → Bndtools → Existing Bnd Workspace → `/home/user/openems`
5. Install Checkstyle plugin (Eclipse-cs)

**UI Development**:
1. Install Node.js 22+
2. `cd ui && npm install`
3. Use VS Code (recommended) or any TypeScript IDE

### Before Committing Code

**CRITICAL**: Always run the preparation script:

```bash
./tools/prepare-commit.sh
```

This script:
- Adds `.gitignore` to empty test directories
- Resets unnecessary `.classpath` file changes
- Resolves EdgeApp and BackendApp bndrun files
- Validates project structure

**Additional Pre-Commit Checks**:
1. Format code using Eclipse Auto-format (Ctrl+Shift+F)
2. Run Checkstyle: `./gradlew checkstyleAll`
3. Ensure all tests pass: `./gradlew test`
4. Remove any debugging code or console output
5. Organize imports

### Pull Request Process

1. **Discuss First**: For features/major changes, discuss in [OpenEMS Community forum](https://community.openems.io/)
2. **Create Branch**: From `develop` branch (not `main`)
3. **One Bundle Per PR**: Preferred approach for easier review
4. **Follow Coding Guidelines**: See https://openems.github.io/openems.io/openems/latest/contribute/coding-guidelines.html
5. **Run Prepare Script**: `./tools/prepare-commit.sh`
6. **Write Tests**: Add JUnit tests for new functionality
7. **Update Documentation**: If adding features, update relevant `.adoc` files
8. **Self-Review**: Review your own changes on GitHub first
9. **Request Review**: Tag relevant maintainers
10. **Address Feedback**: Iterate based on review comments

**DO NOT**:
- Open empty PRs for discussion (use forum instead)
- Submit duplicate feature requests (search first)
- Push directly to `main` branch
- Commit without running prepare script

### Git Workflow

**Branch Strategy**:
- `main` - Stable production releases (protected)
- `develop` - Development branch
- Feature branches from `develop`

**Commit Messages**:
- Use clear, descriptive messages
- Reference issue/discussion if applicable
- Follow conventional commit style when possible

---

## Code Organization Patterns

### Java Component Pattern

Every OpenEMS component follows this structure:

```java
package io.openems.edge.category.implementation;

import org.osgi.service.component.annotations.*;
import io.openems.edge.common.component.*;

@Component(
    name = "Category.Implementation",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ComponentImpl
    extends AbstractOpenemsComponent
    implements Component, OpenemsComponent, EventHandler {

    @Reference
    protected ComponentManager componentManager;

    public ComponentImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            Component.ChannelId.values()
        );
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        // Component-specific activation
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        // Handle configuration changes
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        // Cleanup
    }
}
```

### Configuration Interface Pattern

```java
@interface Config {
    @AttributeDefinition(name = "Component-ID", description = "Unique ID")
    String id() default "component0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?")
    boolean enabled() default true;

    // Component-specific configuration
    @AttributeDefinition(name = "Setting", description = "Description")
    int setting() default 100;

    String webconsole_configurationFactory_nameHint()
        default "Component [{id}] {enabled}";
}
```

### Nature (Interface) Pattern

Natures define device capabilities:

```java
package io.openems.edge.ess.api;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface SymmetricEss extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SOC(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.PERCENT)
            .text("State of Charge")),
        ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)
            .unit(Unit.WATT)
            .text("Current active power"));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    // Typed channel accessors
    default IntegerReadChannel getSocChannel() {
        return this.channel(ChannelId.SOC);
    }

    default IntegerReadChannel getActivePowerChannel() {
        return this.channel(ChannelId.ACTIVE_POWER);
    }
}
```

### Controller Pattern

Controllers implement business logic executed every cycle:

```java
@Component(
    name = "Controller.Ess.Example",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ControllerImpl
    extends AbstractOpenemsComponent
    implements Controller, OpenemsComponent {

    @Reference
    protected ComponentManager componentManager;

    @Reference
    protected ConfigurationAdmin cm;

    private String essId;

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.essId = config.ess_id();
    }

    @Override
    public void run() throws OpenemsException {
        // Get ESS component
        SymmetricEss ess = this.componentManager.getComponent(this.essId);

        // Read current state
        int soc = ess.getSocChannel().getNextValue().orElse(0);

        // Calculate control value
        int targetPower = calculateTargetPower(soc);

        // Write control command
        ess.setActivePowerEquals(targetPower);
    }
}
```

### Channel Access Pattern

Channels provide type-safe, thread-safe data access:

```java
// Writing to a channel (staged for next cycle)
component.channel(ChannelId.TARGET_POWER)
    .setNextValue(1000);

// Reading from a channel with explicit null handling
Integer power = component.channel(ChannelId.ACTIVE_POWER)
    .getNextValue()
    .orElse(0);

// Reading with throwing on null
int power = component.channel(ChannelId.ACTIVE_POWER)
    .getNextValue()
    .orElseThrow(() -> new OpenemsException("Power value not available"));
```

### Process Image Pattern

**Two-Stage Values**:
- `nextValue` - Latest received/written data (updated asynchronously)
- `value` - Active in current cycle (synchronized at cycle start)

This prevents race conditions and ensures stable data during control algorithm execution.

---

## Testing Guidelines

### Java Unit Testing

**Framework**: JUnit Jupiter 5.x

**Test Location**: `/test` directory parallel to `/src`, same package structure

**Naming**: `{Class}Test.java`

**Example**:

```java
package io.openems.edge.controller.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class ControllerImplTest {

    @Test
    public void test() throws Exception {
        var controller = new ControllerImpl();
        var componentManager = new DummyComponentManager();

        new AbstractComponentTest(controller)
            .addReference("componentManager", componentManager)
            .activate(MyConfig.create()
                .setId("ctrl0")
                .setEnabled(true)
                .build());

        // Test logic
        controller.run();

        // Assertions
        assertEquals(100, controller.getSomeValue());
    }
}
```

**Test Utilities**:
- `io.openems.edge.common.test.*` - Test helpers
- `DummyComponentManager` - Mock component registry
- `AbstractComponentTest` - Component testing framework
- Dummy implementations for devices (e.g., `DummyElectricityMeter`)

### Running Tests

```bash
# All tests
./gradlew test

# Edge tests only
./gradlew testEdge

# Backend tests only
./gradlew testBackend

# Specific bundle
./gradlew :io.openems.edge.controller.example:test

# With coverage
./gradlew test jacocoTestReport
```

### Code Coverage

- **Tool**: JaCoCo
- **CI Integration**: Codecov.io (automatic on PRs)
- **Reports**: `build/reports/jacoco/`

### UI Testing

**Framework**: Jasmine + Karma

```bash
cd ui
npm test                     # Run tests
npm test -- --code-coverage  # With coverage
```

---

## Key Conventions

### Code Style - Java

**From Coding Guidelines** (https://openems.github.io/openems.io/openems/latest/contribute/coding-guidelines.html):

1. **Modern Java 21 Features**:
   - Use `var` keyword where type is obvious
   - Lambda expressions and method references
   - Streams API for collections
   - Record types for DTOs
   - Pattern matching

2. **Naming**:
   - Precise, descriptive method names
   - Narrow variable scopes
   - No class-level variables unless necessary
   - Use enums for constants

3. **Structure**:
   - Split into Interface, Implementation (.Impl), Config
   - One logical feature per bundle
   - Add `readme.adoc` to new bundles

4. **Code Quality**:
   - **NO** `System.out.println()` - Use SLF4J logging
   - **NO** unnecessary console logs
   - Fix all errors & warnings
   - Remove empty lines at end of files
   - Add JUnit tests for new functionality
   - Prefer functional programming style

5. **Formatting**:
   - Use Eclipse Auto-format (Ctrl+Shift+F)
   - Eclipse [built-in] formatter profile
   - Organize imports (Ctrl+Shift+O)
   - Checkstyle 11.1.0 compliance required

### Code Style - TypeScript

**Configuration**: `/ui/eslint.config.mjs`

1. Use Angular standalone components (Angular 20+)
2. Follow Ionic design patterns
3. Strict TypeScript enabled
4. Use translation keys for all user-facing text
5. Prettier for formatting

### Logging

**Use SLF4J** (via Pax Logging):

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyComponent {
    private final Logger log = LoggerFactory.getLogger(MyComponent.class);

    public void doSomething() {
        this.log.info("Doing something with value: {}", value);
        this.log.warn("Warning condition detected");
        this.log.error("Error occurred: {}", e.getMessage());
    }
}
```

**Log Levels**:
- `error` - Errors requiring attention
- `warn` - Warnings (system continues)
- `info` - Important state changes
- `debug` - Detailed debugging (not in production)
- `trace` - Very detailed (rarely used)

### Documentation

**Bundle Documentation**:
- Create `readme.adoc` in bundle root (AsciiDoc format)
- Include usage examples
- Document configuration parameters
- Automatically included in Antora docs

**JavaDoc**:
- Required for public APIs
- Use `@param`, `@return`, `@throws` tags
- Include examples for complex methods

**Inline Comments**:
- Explain **why**, not **what**
- Complex algorithms require explanation
- Self-documenting code preferred

---

## Common Tasks

### Creating a New Device Implementation

1. **Choose Category**: Determine device type (meter, battery, ess, etc.)

2. **Create Bundle**:
   ```bash
   # Bundle name: io.openems.edge.{category}.{manufacturer}.{model}
   mkdir io.openems.edge.meter.example.meter123
   ```

3. **Create Structure**:
   ```
   io.openems.edge.meter.example.meter123/
   ├── bnd.bnd
   ├── src/io/openems/edge/meter/example/meter123/
   │   ├── Meter123Impl.java
   │   ├── Config.java
   │   └── package-info.java
   ├── test/io/openems/edge/meter/example/meter123/
   │   └── Meter123Test.java
   └── readme.adoc
   ```

4. **Implement Component**: Extend appropriate base classes, implement natures

5. **Add to Gradle**: Run `./gradlew tasks` to detect new bundle

6. **Test**: Write unit tests

7. **Document**: Create `readme.adoc` with usage instructions

### Creating a New Controller

1. **Create Bundle**: `io.openems.edge.controller.{category}.{name}`

2. **Implement Controller Interface**:
   ```java
   public class MyController extends AbstractOpenemsComponent implements Controller {
       @Override
       public void run() throws OpenemsException {
           // Control logic executed every cycle
       }
   }
   ```

3. **Add Configuration**: Create `Config` interface with required parameters

4. **Test Controller**: Use `DummyComponentManager` for testing

5. **Add to Scheduler**: Controllers must be added to a Scheduler to execute

### Debugging Components

**Eclipse Debug**:
1. Open EdgeApp.bndrun or BackendApp.bndrun
2. Click "Debug OSGi"
3. Set breakpoints in code
4. Access Felix Web Console: http://localhost:8080/system/console

**Logging**:
```java
this.log.debug("Component state: {}", this.getState());
```

**Felix Web Console** (admin/admin):
- Components: Check component status
- Configuration: Modify component configs
- Bundles: Check bundle states
- Services: Inspect OSGi services

### Running UI Against Local Edge

```bash
cd ui
npm start  # Defaults to http://localhost:8085 (Edge WebSocket)
```

**Configuration**: Edit `/ui/src/environments/environment.ts`

---

## Important Files

### Configuration Files

| File | Purpose |
|------|---------|
| `/build.gradle` | Root Gradle build configuration |
| `/settings.gradle` | Gradle workspace settings |
| `/gradle.properties` | Build properties (Java version, etc.) |
| `/cnf/build.bnd` | OSGi bundle defaults, dependencies, Eclipse workingsets |
| `/cnf/pom.xml` | Maven dependencies for Bnd repository |
| `/{bundle}/bnd.bnd` | Per-bundle OSGi configuration |

### Entry Points

| File | Purpose |
|------|---------|
| `/io.openems.edge.application/EdgeApp.bndrun` | Edge application runtime config |
| `/io.openems.backend.application/BackendApp.bndrun` | Backend application runtime config |
| `/ui/src/main.ts` | UI application bootstrap |
| `/ui/src/app/app.component.ts` | UI root component |

### Build Outputs

| File | Purpose |
|------|---------|
| `/build/openems-edge.jar` | Standalone Edge Fat-JAR |
| `/build/openems-backend.jar` | Standalone Backend Fat-JAR |
| `/ui/www/` | UI production build output |

### Development Tools

| File | Purpose |
|------|---------|
| `/tools/prepare-commit.sh` | Pre-commit preparation script (**ALWAYS RUN**) |
| `/tools/prepare-release.sh` | Release preparation |
| `/tools/build-debian-package.sh` | Debian package creation |
| `/.github/workflows/build.yml` | CI/CD pipeline |

### Documentation

| File | Purpose |
|------|---------|
| `/doc/modules/ROOT/pages/` | Antora documentation source |
| `/README.md` | Project overview |
| `/.github/CONTRIBUTING.md` | Contribution guidelines |
| `/{bundle}/readme.adoc` | Per-bundle documentation |

---

## Architecture Principles

### Input-Process-Output (IPO) Model

OpenEMS Edge follows a strict IPO cycle:

1. **Input Phase**: Collect data from devices into process image
2. **Process Phase**: Execute controllers sequentially
3. **Output Phase**: Apply control commands to devices

This guarantees stable data during the control cycle, preventing race conditions.

### Controller Prioritization

- Controllers execute in **order defined by Scheduler**
- Later controllers **cannot override** earlier controllers
- **Interval of feasible solutions** narrows progressively
- Example: LimitTotalDischarge controller restricts what Balancing controller can do

### Nature Abstraction

- Device-independent interfaces (Natures) enable code reuse
- Controllers work with **any implementation** of a nature
- Example: Balancing controller works with any `SymmetricEss` implementation
- Promotes modularity and maintainability

### Asynchronous Communication

- Non-blocking device communication
- Cycle synchronization via OSGi events
- Modbus read/write synchronized with control cycle
- Thread-safe by design (process image pattern)

---

## CI/CD Pipeline

**GitHub Actions** (`.github/workflows/build.yml`):

**On Every Push/PR**:
1. Checkstyle validation
2. Build all bundles
3. Resolve dependencies
4. Validate `.bndrun` files
5. Run all tests (parallel execution)
6. Generate JaCoCo coverage
7. Upload coverage to Codecov.io
8. Build UI (lint, test, build)
9. Build documentation

**On Release**:
1. Build Fat-JARs (Edge & Backend)
2. Build Docker images
3. Create Debian packages
4. Create GitHub release
5. Upload artifacts

---

## Quick Reference

### Most Common Commands

```bash
# Build everything
./gradlew build

# Build Edge Fat-JAR
./gradlew buildEdge

# Run all tests
./gradlew test

# Check code style
./gradlew checkstyleAll

# Prepare for commit
./tools/prepare-commit.sh

# Build documentation
./gradlew buildAntoraDocs

# UI development
cd ui && npm start

# UI tests
cd ui && npm test
```

### Most Common Patterns

```java
// Reading a channel
int value = component.channel(ChannelId.SOME_VALUE)
    .getNextValue()
    .orElse(0);

// Writing a channel
component.channel(ChannelId.TARGET_VALUE)
    .setNextValue(100);

// Getting a component
SymmetricEss ess = this.componentManager.getComponent("ess0");

// Logging
this.log.info("Message with value: {}", value);
```

### Getting Help

1. **Documentation**: https://openems.github.io/openems.io/openems/latest/
2. **Forum**: https://community.openems.io/
3. **Javadoc**: https://openems.github.io/openems.io/javadoc/
4. **Code Examples**: Search existing implementations in `io.openems.edge.*`

---

## Key Takeaways for AI Assistants

1. **ALWAYS run** `./tools/prepare-commit.sh` before committing
2. **Follow OSGi patterns**: Component, Config, Nature implementations
3. **Use Process Image**: Respect `nextValue` vs `value` semantics
4. **No `System.out`**: Use SLF4J logging only
5. **Test Required**: Add JUnit tests for new functionality
6. **One Bundle Per PR**: Easier to review and maintain
7. **Discuss First**: Use community forum for feature discussions
8. **Format Code**: Eclipse auto-format, Checkstyle compliance
9. **Document**: Add `readme.adoc` to new bundles
10. **Respect Licenses**: EPL-2.0 (Edge/Backend), AGPL-3.0 (UI)

---

## Additional Resources

- **Getting Started Guide**: https://openems.github.io/openems.io/openems/latest/gettingstarted.html
- **Implementation Guide**: https://openems.github.io/openems.io/openems/latest/edge/implement.html
- **Coding Guidelines**: https://openems.github.io/openems.io/openems/latest/contribute/coding-guidelines.html
- **Architecture Documentation**: https://openems.github.io/openems.io/openems/latest/edge/architecture.html
- **Live Demo**: https://gitpod.io/#https://github.com/OpenEMS/openems

---

**Last Updated**: 2025-11-17
**OpenEMS Version**: Based on latest `main` branch
