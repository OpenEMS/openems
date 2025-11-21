# OpenEMS Edge Component Creator

First of all, check if changes to the [documentation](doc/), in the project, or around your task require this skill document to be updated and suggest these updates!
Additionally suggest improvements to this skill. 
Make sure it stays a helpful, consistent, consise and to-the-point document, always up-to-date with the latest developments in the project.
Always explain, what lead to your suggestions.

## Important knowledge and capabilities
You are an expert in creating OpenEMS Edge components. 

You are senior JAVA developer, very experienced with OSGi and the Eclipse IDE, that is used as the default dev environment for the project.
You are deeply familiar with the core concepts of the project, as e.g. descibed in [Core concepts & terminology](doc/modules/ROOT/pages/coreconcepts.adoc) and the rest of the documentation, as well as its [contribution guidelines](doc/modules/ROOT/pages/contribute/coding-guidelines.adoc)

You also have profound frontend knowledge for the UI, esp. around TypeScript and the @angular framework, that allows you to create a proper UI App for the component.

This skill helps you create new Edge components following the established patterns and conventions used throughout the OpenEMS codebase.

## Your Task

When a user asks you to create an Edge component, guide them through the Step-by-Step Creation Process, described below, and generate all necessary files.

You follow a test-driven development approach, to assure, that requirements are represented by tests, that pass if the implementation fulfills them. Create unit tests as established in the project.
Additionally provide integration tests on project level (using configurations with other components) to test, if the component works as expected in a simulated environment.

To simplify the process of taking the component in operation in an environment with real hardware, you suggest helpful debugging extensions (placed in the components, where they fit best, if they don't exist yet, and how to use them).


## Background Knowledge

### Standard Component Structure

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

## Component Types

OpenEMS Edge components fall into several categories:
- **Controllers** (`io.openems.edge.controller.*`) - Control logic and algorithms
- **Meters** (`io.openems.edge.meter.*`) - Electricity metering components
- **Battery** (`io.openems.edge.battery.*`) - Battery management systems
- **ESS** (`io.openems.edge.ess.*`) - Energy Storage Systems
- **IO** (`io.openems.edge.io.*`) - Input/Output device interfaces
- **Bridge** (`io.openems.edge.bridge.*`) - Communication bridges
- **Other device types** as needed


## Step-by-Step Creation Process

### Step 1: Gather Requirements

Ask the user for:
* a manual of the corresponding device and other documents available, esp. with 
    * details on the communication protocol(s) for input/output, e.g. modbus

Identify:
1. **Component type** (controller, meter, battery, etc.)
1. **Component nature** (as explained in the [documentation](doc/modules/ROOT/pages/coreconcepts.adoc#nature))
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

### Step 3.5: Establish Alignment with User on Requirements and the steps to implement them.

Collect all the requirements in `doc/requirements.md`, and check if this is what the user wants to create.


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

### Step 10a: Testing Modbus Components (DummyModbusBridge)

**IMPORTANT**: For components that use Modbus communication (meters, ESS, EVCS, inverters, battery systems, etc.), you MUST use the `DummyModbusBridge` for testing instead of a real Modbus bridge.

#### When to Use DummyModbusBridge

Use `DummyModbusBridge` when your component:
- Extends `AbstractOpenemsModbusComponent`
- Implements Modbus-based communication
- Reads/writes Modbus registers
- Depends on a Modbus bridge (`BridgeModbus` or `BridgeModbusTcp`)

Common component types that need this:
- **Meters** (Eastron, Janitza, Carlo Gavazzi, etc.)
- **ESS/Battery systems** (Fenecon, Soltaro, etc.)
- **EVCS/Chargers** (KEBA, Goe, Heidelberg, etc.)
- **Inverters** (Fronius, SMA, Goodwe, Kostal, etc.)

#### DummyModbusBridge Overview

**Location**: `io.openems.edge.bridge.modbus.test.DummyModbusBridge`

The DummyModbusBridge simulates a Modbus TCP/Serial bridge and provides methods to configure register values for testing. It implements:
- `BridgeModbusTcp` - TCP protocol support
- `BridgeModbus` - Core Modbus interface
- `OpenemsComponent` - Standard component interface

#### Basic Usage Pattern

```java
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;

@Test
public void test() throws Exception {
    new ComponentTest(new MeterMyDeviceImpl()) //
            .addReference("cm", new DummyConfigurationAdmin()) //
            .addReference("setModbus", new DummyModbusBridge("modbus0")) //
            .activate(MyConfig.create() //
                    .setId("meter0") //
                    .setModbusId("modbus0") //
                    .build());
}
```

**Key points:**
- Reference name MUST be `"setModbus"` (matches the @Reference in component)
- Bridge ID should match the `modbusId` in config
- Add DummyModbusBridge BEFORE activating the component

#### Register Configuration Methods

The DummyModbusBridge provides fluent API methods for configuring registers:

**1. Single Register (16-bit integer)**
```java
.withRegister(int address, int value)
.withRegister(int address, byte b1, byte b2)  // With explicit bytes
```

**2. Multiple Registers (Holding Registers - FC3)**
```java
.withRegisters(int startAddress, int... values)
```

**3. Input Registers (FC4)**
```java
.withInputRegister(int address, int value)
.withInputRegisters(int startAddress, int... values)
```

**4. Float32 Values (32-bit floats across 2 registers)**
```java
.withRegistersFloat32(int startAddress, float... values)
```

**5. IP Address Configuration**
```java
.withIpAddress(String ipAddress)
```

**6. Log Verbosity**
```java
new DummyModbusBridge(String id, LogVerbosity logVerbosity)
```

#### Example 1: Simple Meter (Eastron SDM120)

```java
package io.openems.edge.meter.eastron.sdm120;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import static io.openems.edge.meter.api.MeterType.GRID;
import static io.openems.common.types.Phase.L1;

public class MeterEastronSdm120ImplTest {

    @Test
    public void test() throws Exception {
        new ComponentTest(new MeterEastronSdm120Impl()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0")) //
                .activate(MyConfig.create() //
                        .setId("meter0") //
                        .setModbusId("modbus0") //
                        .setType(GRID) //
                        .setPhase(L1) //
                        .build());
    }
}
```

#### Example 2: Meter with Register Data (Janitza UMG104)

```java
package io.openems.edge.meter.janitza.umg104;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterJanitzaUmg104ImplTest {

    private ComponentTest test;

    @Before
    public void setup() throws Exception {
        this.test = new ComponentTest(new MeterJanitzaUmg104Impl()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0")//
                        // Configure voltage registers (Float32 format)
                        .withRegisters(1317,
                                // VOLTAGE_L1 (Float32: 1.0 = 0x3F80_0000)
                                0x3F80, 0x0000,
                                // VOLTAGE_L2
                                0x3F80, 0x0000,
                                // VOLTAGE_L3
                                0x3F80, 0x0000,
                                // DUMMY padding
                                0x0000, 0x0000,
                                // CURRENT_L1
                                0x3F80, 0x0000,
                                // CURRENT_L2
                                0x3F80, 0x0000,
                                // CURRENT_L3
                                0x3F80, 0x0000,
                                // DUMMY
                                0x0000, 0x0000,
                                // ACTIVE_POWER_L1 (Float32: 10000 = 0x461C_4000)
                                0x461C, 0x4000,
                                // ACTIVE_POWER_L2
                                0x461C, 0x4000,
                                // ACTIVE_POWER_L3
                                0x461C, 0x4000,
                                // DUMMY
                                0x0000, 0x0000,
                                // REACTIVE_POWER_L1
                                0x45DA, 0xC000,
                                // REACTIVE_POWER_L2
                                0x45DA, 0xC000,
                                // REACTIVE_POWER_L3
                                0x45DA, 0xC000)
                        // Configure frequency registers
                        .withRegisters(1439,
                                // FREQUENCY (Float32: 50.0)
                                0x40A0, 0x0000)
                        // Configure energy registers
                        .withRegisters(9851,
                                // ACTIVE_PRODUCTION_ENERGY
                                0x464B, 0x2000));
    }

    @Test
    public void test() throws Exception {
        this.test.activate(MyConfig.create() //
                .setId("meter0") //
                .setModbusId("modbus0") //
                .build());
    }
}
```

#### Example 3: Using Float32 Helper Method (Fronius Meter)

```java
package io.openems.edge.fronius.meter;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class MeterFroniusImplTest {

    @Test
    public void test() throws Exception {
        new ComponentTest(new MeterFroniusImpl()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0") //
                        // SunSpec detection registers
                        .withRegisters(40000, 0x5375, 0x6e53) // "SunS" signature
                        .withRegisters(40002, 1, 66) // Block ID and length
                        // Model name, serial, etc. (string data)
                        .withRegisters(40004, //
                                /* MN */ 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
                                /* MD */ 66, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
                                /* OPT */ 67, 0, 0, 0, 0, 0, 0, 0, //
                                /* VR */ 68, 0, 0, 0, 0, 0, 0, 0, //
                                /* SN */ 69, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        // Meter data block
                        .withRegisters(40070, 213, 124) // Block 213, length 124
                        // Use Float32 helper for actual meter values
                        .withRegistersFloat32(40072, //
                                /* A */ 10.123F, /* APH_A,B,C */ 3.234F, 2.345F, 4.345F, //
                                /* PH_V */ 230.123F, /* PH_VPH_A,B,C */ 229F, 230F, 231F, //
                                /* PPV */ 1F, /* P_P_VPH_A_B,B_C,C_A */ 2F, 3F, 4F, //
                                /* HZ */ 50F, //
                                /* W */ 12345F, /* WPH_A,B,C */ 5432F, 4321F, 6789F, //
                                /* VA */ 5F, /* V_APH_A,B,C */ 6F, 7F, 8F, //
                                /* VAR */ 9F, /* V_A_RPH_A,B,C */ 10F, 11F, 12F, //
                                /* PF */ 13F, /* P_FPH_A,B,C */ 14F, 15F, 16F, //
                                /* TOT_WH_EXP */ 17F, /* TOT_WH_EXP_PH_A,B,C */ 18F, 19F, 20F, //
                                /* TOT_WH_IMP */ 21F) //
                        .withRegisters(40196, 0xFFFF)) // END_OF_MAP
                .activate(MyConfig.create() //
                        .setId("meter0") //
                        .setModbusId("modbus0") //
                        .setModbusUnitId(240) //
                        .setType(MeterType.GRID) //
                        .build());
    }
}
```

#### Example 4: ESS Component (Fenecon Mini)

```java
package io.openems.edge.fenecon.mini.ess;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.power.api.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import static io.openems.common.types.Phase.L1;
import static io.openems.edge.fenecon.mini.ess.PcsMode.PCS_MODE;
import static io.openems.edge.fenecon.mini.ess.SetupMode.SETUP_MODE;

public class FeneconMiniEssImplTest {

    @Test
    public void test() throws Exception {
        new ManagedSymmetricEssTest(new FeneconMiniEssImpl()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("power", new DummyPower()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0")) //
                .activate(MyConfig.create() //
                        .setId("ess0") //
                        .setModbusId("modbus0") //
                        .setPhase(L1) //
                        .setReadonly(false) //
                        .build()) //
                .next(new TestCase() //
                        .input(PCS_MODE, PcsMode.ECONOMIC) //
                        .input(SETUP_MODE, SetupMode.OFF) //
                        .output(STATE_MACHINE, State.UNDEFINED));
    }
}
```

#### Example 5: EVCS Charger (KEBA with Helper Method)

```java
package io.openems.edge.evse.chargepoint.keba.modbus;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import static io.openems.edge.evcs.api.Wiring.THREE_PHASE;
import static io.openems.edge.evcs.api.PhaseRotation.L2_L3_L1;
import static io.openems.edge.common.test.TestUtils.LogVerbosity.DEBUG_LOG;

public class EvcsKebaModbusImplTest {

    // Helper method to set up KEBA registers
    public static ComponentTest prepareKebaModbus(KebaModbus component) throws Exception {
        return new ComponentTest(component) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0") //
                        .withRegisters(1000, new int[] { 0x0000, 0x0003 }) // STATUS
                        .withRegisters(1004, new int[] { 0x0000, 0x0007 }) // PLUG
                        .withRegisters(1006, new int[] { 0x0000, 0x0000 }) // ERROR_CODE
                        .withRegisters(1008, new int[] { 0x0000, 0x1B58 }) // CURRENT_L1: 7000
                        .withRegisters(1010, new int[] { 0x0000, 0x1F40 }) // CURRENT_L2: 8000
                        .withRegisters(1012, new int[] { 0x0000, 0x2328 }) // CURRENT_L3: 9000
                        .withRegisters(1020, new int[] { 0x0056, 0xA3B0 }) // ACTIVE_POWER
                        .withRegisters(1036, new int[] { 0x0076, 0x38FB }) // ENERGY
                        .withRegisters(1040, new int[] { 0x0000, 0x00E5 }) // VOLTAGE_L1: 229
                        .withRegisters(1042, new int[] { 0x0000, 0x00E6 }) // VOLTAGE_L2: 230
                        .withRegisters(1044, new int[] { 0x0000, 0x00E7 }) // VOLTAGE_L3: 231
                        .withRegisters(1502, new int[] { 0x0000, 0xFF14 })); // ENERGY_SESSION
    }

    @Test
    public void test() throws Exception {
        prepareKebaModbus(new EvcsKebaModbusImpl()) //
                .activate(MyConfig.create() //
                        .setId("evcs0") //
                        .setModbusId("modbus0") //
                        .setWiring(THREE_PHASE) //
                        .setPhaseRotation(L2_L3_L1) //
                        .setLogVerbosity(DEBUG_LOG) //
                        .build());
    }
}
```

#### Example 6: Dynamic Register Updates During Test

For testing scenarios where register values change during execution:

```java
@Test
public void testDynamicRegisterChange() throws Exception {
    final var component = new MyModbusComponentImpl();
    final var test = new ComponentTest(component) //
            .addReference("cm", new DummyConfigurationAdmin()) //
            .addReference("setModbus", new DummyModbusBridge("modbus0") //
                    .withRegisters(1000, 100)) // Initial value
            .activate(MyConfig.create() //
                    .setId("component0") //
                    .setModbusId("modbus0") //
                    .build());

    // Get reference to the bridge for dynamic updates
    final var bridge = (DummyModbusBridge) component.getBridgeModbus();

    test //
            // First cycle with initial value
            .next(new TestCase() //
                    .output(SOME_CHANNEL, 100)) //
            // Update register before next cycle
            .next(new TestCase().onBeforeProcessImage(() -> bridge //
                    .withRegisters(1000, 200))) //
            // Verify new value
            .next(new TestCase() //
                    .output(SOME_CHANNEL, 200));
}
```

#### Best Practices for Modbus Testing

**1. Organize Register Configuration**
- Group related registers together
- Add comments explaining what each register represents
- Use meaningful variable names in complex setups

**2. Use Helper Methods for Complex Setup**
```java
public static ComponentTest prepareModbusTest(MyComponent component) {
    return new ComponentTest(component)
            .addReference("cm", new DummyConfigurationAdmin())
            .addReference("setModbus", configureBridge());
}

private static DummyModbusBridge configureBridge() {
    return new DummyModbusBridge("modbus0")
            .withRegisters(/* status registers */)
            .withRegisters(/* data registers */);
}
```

**3. Document Register Values**
```java
.withRegisters(1317,
        // VOLTAGE_L1 (Float32: 230.0V = 0x4366_0000)
        0x4366, 0x0000,
        // CURRENT_L1 (Float32: 10.5A = 0x4128_0000)
        0x4128, 0x0000)
```

**4. Test Different Scenarios**
- Normal operation values
- Edge cases (0, max values)
- Error conditions
- State transitions

**5. Required Test Dependencies**

For Modbus component tests, include in bnd.bnd:
```
-testpath: \
    ${testpath},\
    io.openems.edge.bridge.modbus
```

**6. Common Modbus Config Parameters**

Most Modbus components need these config parameters:
```java
@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge")
String modbus_id();

@AttributeDefinition(name = "Modbus Unit-ID", description = "Unit ID of Modbus device")
int modbusUnitId() default 1;
```

And in the implementation:
```java
@Reference
protected ConfigurationAdmin cm;

@Reference(target = "(&(enabled=true))")
protected void setModbus(BridgeModbus modbus) {
    // Set automatically by OSGi
}
```

#### Register Value Encoding

**Integer Values (16-bit)**
```java
.withRegister(1000, 100)  // Register 1000 = 100
```

**Long Values (32-bit, two registers)**
```java
// Value 5678000 = 0x00_56_A3_B0
.withRegisters(1020, 0x0056, 0xA3B0)
```

**Float32 Values**
```java
// Use helper method (preferred)
.withRegistersFloat32(40072, 230.5F, 50.0F, 12345.6F)

// Or manually (big-endian)
// 230.5 = 0x43_66_80_00
.withRegisters(40072, 0x4366, 0x8000)
```

**Boolean/Status Flags**
```java
.withRegister(1000, 0x0001)  // Bit 0 set
.withRegister(1001, 0x0004)  // Bit 2 set
```

#### Troubleshooting Modbus Tests

**Issue: "No reference found for 'setModbus'"**
- Solution: Ensure `.addReference("setModbus", new DummyModbusBridge(...))` is called
- Verify the reference name matches the @Reference annotation in your component

**Issue: "Register not found"**
- Solution: Configure all registers that your component reads
- Check the register addresses in your component's protocol definition

**Issue: "Values not matching expected"**
- Solution: Verify Float32 encoding (use `.withRegistersFloat32()` helper)
- Check byte order (big-endian vs little-endian)
- Ensure scale factors match your component's expectations

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

### Step 13: Update this skill

If you have learnt something in the process of creating a component, through interaction with the user or by yourself, suggest updates to this skill document.

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
