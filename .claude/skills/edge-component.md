# OpenEMS Edge Component Creator

You are an expert in creating OpenEMS Edge components. This skill helps you create new Edge components following the established patterns and conventions used throughout the OpenEMS codebase.

## Your Task

When a user asks you to create an Edge component, guide them through the process and generate all necessary files following the patterns described below.

## Component Types

OpenEMS Edge components fall into several categories:
- **Controllers** (`io.openems.edge.controller.*`) - Control logic and algorithms
- **Meters** (`io.openems.edge.meter.*`) - Electricity metering components
- **Battery** (`io.openems.edge.battery.*`) - Battery management systems
- **ESS** (`io.openems.edge.ess.*`) - Energy Storage Systems
- **IO** (`io.openems.edge.io.*`) - Input/Output device interfaces
- **Bridge** (`io.openems.edge.bridge.*`) - Communication bridges
- **Other device types** as needed

## Standard Component Structure

Every Edge component follows this structure:

```
io.openems.edge.{type}.{vendor}.{model}/
├── src/io/openems/edge/{type}/{vendor}/{model}/
│   ├── {ComponentName}.java              # Interface defining component contract
│   ├── {ComponentName}Impl.java          # Implementation class
│   ├── Config.java                       # OSGi configuration
│   ├── State.java                        # Optional: State enum for state machines
│   ├── Mode.java                         # Optional: Mode enum for operation modes
│   └── {Helper}.java                     # Optional: Additional helper classes
├── test/io/openems/edge/{type}/{vendor}/{model}/
│   ├── {ComponentName}ImplTest.java      # Unit tests
│   └── MyConfig.java                     # Test configuration builder
├── bnd.bnd                               # OSGi bundle configuration
├── readme.adoc                           # Component documentation
├── .project                              # Eclipse project file
├── .classpath                            # Eclipse classpath
└── .gitignore                            # Git ignore file
```

## Step-by-Step Creation Process

### Step 1: Gather Requirements

Ask the user for:
1. **Component type** (controller, meter, battery, etc.)
2. **Vendor name** (e.g., fenecon, schneider, virtual, custom)
3. **Model/component name** (e.g., randompower, channelsinglethreshold)
4. **Description** of what the component does
5. **Configuration parameters** needed
6. **Channels** the component should expose (if any)
7. **Dependencies** on other components (ESS, IO devices, etc.)
8. **Interfaces to implement** (beyond the base ones)

### Step 2: Determine Naming

Based on the requirements, establish:
- **Module name**: `io.openems.edge.{type}.{vendor}.{model}`
- **Package name**: `io.openems.edge.{type}.{vendor}.{model}`
- **Component name**: `{Type}{Vendor}{Model}` (in PascalCase)
- **OSGi component name**: `{Type}.{Vendor}.{Model}` (dots separated)

Example:
- Type: controller, Vendor: ess, Model: randompower
- Module: `io.openems.edge.controller.ess.randompower`
- Component: `ControllerEssRandomPower`
- OSGi: `Controller.Ess.RandomPower`

### Step 3: Create Project Structure

Create the module directory and all subdirectories.

### Step 4: Create the Interface

**Template for `{ComponentName}.java`:**

```java
package io.openems.edge.{type}.{vendor}.{model};

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.{type}.api.{Type}; // e.g., Controller

/**
 * {Description of component}
 */
public interface {ComponentName} extends {Type}, OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        // Define custom channels here
        // Example:
        // STATE_MACHINE(Doc.of(State.values())
        //     .text("Current state of the component")),
        // CUMULATED_ACTIVE_TIME(Doc.of(OpenemsType.LONG)
        //     .unit(Unit.CUMULATED_SECONDS)
        //     .persistencePriority(PersistencePriority.HIGH));
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    // Add getter methods for channels
    // Example:
    // default Value<State> getStateMachine() {
    //     return this.channel(ChannelId.STATE_MACHINE).value();
    // }
}
```

**Key Points:**
- Always extend the appropriate base interface (Controller, OpenemsComponent, etc.)
- Define ChannelId enum for custom channels
- Use Doc.of() to document channels with appropriate types, units, and persistence
- Add convenience getter methods for channels

### Step 5: Create the Config Interface

**Template for `Config.java`:**

```java
package io.openems.edge.{type}.{vendor}.{model};

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "{Human Readable Name}",
    description = "{Description of what this component does}")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "{defaultId}";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    // Add component-specific configuration parameters
    // Examples:
    // @AttributeDefinition(name = "ESS-ID", description = "ID of the Energy Storage System")
    // String ess_id();
    //
    // @AttributeDefinition(name = "Min Power [W]", description = "Minimum power (negative for charge)")
    // int minPower() default 0;
    //
    // @AttributeDefinition(name = "Threshold", description = "Threshold value")
    // int threshold();

    String webconsole_configurationFactory_nameHint() default "{Human Readable Name} [{id}]";
}
```

**Key Points:**
- Always include id, alias, and enabled
- Use descriptive names and descriptions
- Use appropriate types (String, int, boolean, enums)
- Provide sensible defaults where possible
- webconsole_configurationFactory_nameHint customizes the UI display

### Step 6: Create the Implementation Class

**Template for `{ComponentName}Impl.java`:**

```java
package io.openems.edge.{type}.{vendor}.{model};

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.{type}.api.{Type};

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "{Type}.{Vendor}.{Model}",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class {ComponentName}Impl extends AbstractOpenemsComponent
        implements {ComponentName}, {Type}, OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger({ComponentName}Impl.class);

    @Reference
    private ComponentManager componentManager;

    private Config config;

    public {ComponentName}Impl() {
        super(
            OpenemsComponent.ChannelId.values(),
            {Type}.ChannelId.values(),
            {ComponentName}.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;

        // Add initialization logic here
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsNamedException {
        // For Controllers: Implement control logic here
        // This method is called every cycle

        // Example pattern:
        // 1. Get referenced components
        // 2. Read current state/values
        // 3. Execute control logic
        // 4. Write outputs/commands
    }
}
```

**Key Points:**
- Use @Designate to link the Config
- Set appropriate OSGi component name
- Extend AbstractOpenemsComponent
- Inject ComponentManager via @Reference
- Initialize all ChannelId enums in constructor
- Implement activate/deactivate lifecycle methods
- For Controllers: implement run() method
- Use logger for debugging (not System.out.println)

### Step 7: Create Optional Enums

**State.java template** (for state machines):

```java
package io.openems.edge.{type}.{vendor}.{model};

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    IDLE(0, "Idle"),
    RUNNING(1, "Running"),
    ERROR(2, "Error");

    private final int value;
    private final String name;

    private State(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public OptionsEnum getUndefined() {
        return UNDEFINED;
    }
}
```

**Mode.java template** (for operation modes):

```java
package io.openems.edge.{type}.{vendor}.{model};

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
    AUTOMATIC(0, "Automatic"),
    MANUAL(1, "Manual"),
    OFF(2, "Off");

    private final int value;
    private final String name;

    private Mode(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public OptionsEnum getUndefined() {
        return AUTOMATIC;
    }
}
```

### Step 8: Create bnd.bnd

**Template:**

```
Bundle-Name: OpenEMS Edge {Type} {Vendor} {Model}
Bundle-Vendor: FENECON GmbH
Bundle-License: https://opensource.org/licenses/EPL-2.0
Bundle-Version: 1.0.0.${tstamp}

-buildpath: \
    ${buildpath},\
    io.openems.common,\
    io.openems.edge.common,\
    io.openems.edge.{type}.api,\
    # Add other dependencies as needed
    # io.openems.edge.ess.api,\
    # io.openems.edge.io.api,\

-testpath: \
    ${testpath}
```

**Key Points:**
- Update Bundle-Name appropriately
- Include all necessary dependencies in -buildpath
- Common dependencies: io.openems.common, io.openems.edge.common, type-specific API

### Step 9: Create Test Configuration Builder

**Template for `test/.../MyConfig.java`:**

```java
package io.openems.edge.{type}.{vendor}.{model};

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
        private String id;
        // Add fields for each config parameter
        // private String essId;
        // private int minPower;
        // private int maxPower;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        // Add setters for each config parameter
        // public Builder setEssId(String essId) {
        //     this.essId = essId;
        //     return this;
        // }

        public MyConfig build() {
            return new MyConfig(this);
        }
    }

    public static Builder create() {
        return new Builder();
    }

    private final Builder builder;

    private MyConfig(Builder builder) {
        super(Config.class, builder.id);
        this.builder = builder;
    }

    // Implement all Config interface methods
    // @Override
    // public String ess_id() {
    //     return this.builder.essId;
    // }
}
```

### Step 10: Create Unit Test

**Template for `test/.../{ComponentName}ImplTest.java`:**

```java
package io.openems.edge.{type}.{vendor}.{model};

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.{type}.test.{Type}Test;

public class {ComponentName}ImplTest {

    @Test
    public void test() throws Exception {
        new {Type}Test(new {ComponentName}Impl())
            .addReference("componentManager", new DummyComponentManager())
            // Add component dependencies
            // .addComponent(new DummyManagedSymmetricEss("ess0"))
            .activate(MyConfig.create()
                .setId("ctrl0")
                // Set config parameters
                // .setEssId("ess0")
                // .setMinPower(-1000)
                // .setMaxPower(1000)
                .build())
            // Add test cases if needed
            // .next(new TestCase()
            //     .input("ess0", SOC, 50)
            //     .output(SOME_CHANNEL, expectedValue))
            .deactivate();
    }
}
```

### Step 11: Create Eclipse Project Files

**.project template:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
    <name>io.openems.edge.{type}.{vendor}.{model}</name>
    <comment></comment>
    <projects>
    </projects>
    <buildSpec>
        <buildCommand>
            <name>org.eclipse.jdt.core.javabuilder</name>
            <arguments>
            </arguments>
        </buildCommand>
        <buildCommand>
            <name>bndtools.core.bndbuilder</name>
            <arguments>
            </arguments>
        </buildCommand>
    </buildSpec>
    <natures>
        <nature>org.eclipse.jdt.core.javanature</nature>
        <nature>bndtools.core.bndnature</nature>
    </natures>
</projectDescription>
```

**.classpath template:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
    <classpathentry kind="src" output="target/classes" path="src"/>
    <classpathentry kind="src" output="target/test-classes" path="test"/>
    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
    <classpathentry kind="con" path="aQute.bnd.classpath.container"/>
    <classpathentry kind="output" path="target/classes"/>
</classpath>
```

**.gitignore template:**

```
/target/
/bin/
/bin_test/
/generated/
/.settings/
```

### Step 12: Create Documentation

**readme.adoc template:**

```asciidoc
= {Component Name}

{Detailed description of what the component does}

== Configuration

[options="header"]
|===
| Parameter | Type | Default | Description
| id | String | {defaultId} | Unique component ID
| alias | String | | Human-readable name
| enabled | Boolean | true | Enable/disable component
// Add rows for each config parameter
|===

== Channels

[options="header"]
|===
| Channel | Type | Description
// Add rows for each channel
|===

== Usage

{Explain how to use and configure the component}

== Example Configuration

[source,json]
----
{
  "id": "{defaultId}",
  "alias": "My {Component}",
  "enabled": true
  // Add example config values
}
----
```

## Common Patterns and Best Practices

### Channel Types and Documentation

Use appropriate channel types:
- **StateChannel**: Boolean state channels (Level.INFO, Level.WARNING, Level.FAULT)
- **IntegerChannel**: Integer values
- **LongChannel**: Long values
- **StringChannel**: Text values
- **EnumChannel**: Enum values (State, Mode, etc.)

Channel documentation attributes:
```java
Doc.of(OpenemsType.LONG)                    // Type
    .unit(Unit.WATT)                        // Unit of measurement
    .persistencePriority(PersistencePriority.HIGH)  // Persistence level
    .text("Description of channel")         // Human-readable description
```

### Getting Referenced Components

```java
// Get a component by ID
ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

// Get channel by address
Channel<?> channel = this.componentManager.getChannel(
    ChannelAddress.fromString(this.config.inputChannelAddress())
);
```

### Setting Channel Values

```java
// Set next value (will be applied next cycle)
this.channel(ChannelId.STATE_MACHINE).setNextValue(State.RUNNING);

// Using convenience setter (define in interface)
this._setStateMachine(State.RUNNING);
```

### Error Handling

```java
try {
    // Code that might throw
} catch (OpenemsNamedException e) {
    this.logError(this.log, "Error message: " + e.getMessage());
}
```

### Controller Run Method Pattern

```java
@Override
public void run() throws OpenemsNamedException {
    // 1. Get components
    ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

    // 2. Read current state
    int currentPower = ess.getActivePower().orElse(0);

    // 3. Execute logic
    int targetPower = calculateTargetPower(currentPower);

    // 4. Apply constraints/commands
    ess.addPowerConstraintAndValidate("MyController", Phase.ALL, Pwr.ACTIVE,
        Relationship.EQUALS, targetPower);

    // 5. Update internal channels
    this.channel(ChannelId.TARGET_POWER).setNextValue(targetPower);
}
```

## Component Examples for Reference

**Simple Controller:**
- Path: `io.openems.edge.controller.symmetric.randompower`
- Use for: Basic controller structure
- Files: Interface, Config, Impl, Test

**Controller with State Machine:**
- Path: `io.openems.edge.controller.io.channelsinglethreshold`
- Use for: State machines, modes, complex logic
- Files: Interface, Config, Impl, State, Mode, Test

**Meter Component:**
- Path: `io.openems.edge.meter.virtual`
- Use for: Meter patterns
- Files: Multiple implementations

## Workflow Summary

When user requests a new component:

1. **Ask for requirements** (type, vendor, model, description, config params, channels, dependencies)
2. **Confirm naming** (module, package, component name, OSGi name)
3. **Create directory structure**
4. **Generate all files** using templates above
5. **Customize** based on specific requirements:
   - Add custom channels to interface
   - Add config parameters to Config.java
   - Implement business logic in Impl.java
   - Add state/mode enums if needed
   - Create appropriate tests
6. **Verify dependencies** in bnd.bnd
7. **Create documentation** in readme.adoc

## Important Notes

- Always extend `AbstractOpenemsComponent`
- Always implement appropriate interfaces (Controller, OpenemsComponent, etc.)
- Use `@Reference` for dependency injection (ComponentManager, other services)
- Initialize all ChannelId enums in constructor
- Use logger (not System.out)
- Follow OSGi lifecycle (activate, deactivate)
- Use builder pattern for test configuration
- Include proper error handling
- Document channels thoroughly
- Use descriptive variable names
- Follow Java naming conventions

## After Component Creation

Remind the user to:
1. Add the new module to parent `build.gradle` or build configuration
2. Build the project: `./gradlew build` or equivalent
3. Test the component in OpenEMS Edge environment
4. Consider adding integration tests
5. Update parent documentation if needed

Now you're ready to create Edge components! Ask the user what component they want to create.
