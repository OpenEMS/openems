# OpenEMS README Standardization Guide

## Overview

This guide documents the standard structure and process for creating and updating `readme.adoc` files for OpenEMS bundles. The standardization ensures:

- **Human discoverability**: Consistent structure makes it easier to navigate and learn about features
- **Agent context**: Coding agents have rich context for understanding bundle capabilities
- **Documentation quality**: Professional, comprehensive documentation for the official Antora documentation site

## Standard Structure

All bundle readmes must follow this AsciiDoc structure:

```
= Bundle Title

One-line description.

== Overview

2-3 sentences describing what the bundle provides, its purpose, and typical use cases.

== Supported Devices

List of specific devices/models supported with key attributes:
- Device names
- Communication protocols
- Typical meter types (GRID, PRODUCTION, CONSUMPTION)
- Links to manufacturer documentation

== Components

Section listing all OSGi components in the bundle.

For EACH component, create a subsection:

=== <<_component_id,Component Display Name>>

*Name*: Display name from @ObjectClassDefinition

*Factory-PID*: `Factory.PID.From.Component`

.Implemented Natures/Interfaces
* Nature1
* Nature2
* Nature3

*Description*: 2-3 sentence explanation of what this component does.

*Use Cases*: (optional) Bullet list of typical use cases

.*Configuration*:
* `configParam1` (Type): Description (default: value)
* `configParam2` (Type): Description (default: value)

[[_component_id]]

https://github.com/OpenEMS/openems/tree/develop/io.openems.edge.BUNDLE[Source Code icon:github[]]
```

## Key AsciiDoc Syntax Rules

**CRITICAL**: Incorrect syntax breaks the build. Follow these exactly:

### List Titles
```asciidoc
✓ CORRECT:
.Implemented Natures/Interfaces
* Item1
* Item2

✗ WRONG:
*Implemented Natures/Interfaces*:
* Item1
- Item2
```

### Bullet Lists
```asciidoc
✓ CORRECT:
* Bullet item (use asterisk)

✗ WRONG:
- Bullet item (hyphen for unordered is wrong)
```

### External Links
```asciidoc
✓ CORRECT:
https://example.com[Link text icon:external-link[]]
https://github.com/path[Source Code icon:github[]]

✗ WRONG:
link:https://example.com[Link text]
https://example.com[Link text]
```

### Cross-References (Anchors)
```asciidoc
✓ CORRECT:
[[_component_name]]        # Anchor definition

<<_component_name,Link text>>  # Reference to anchor

Anchors are auto-generated from headings:
"=== Component Foo Bar" → [[_component_foo_bar]]

✗ WRONG:
<<Component Name>>         # Don't include spaces in anchors
```

### Heading Hierarchy
```asciidoc
= Main Title (Bundle name)
== Primary Sections (Overview, Components, etc.)
=== Subsections (Individual components)
[[_anchor]]  # Place anchors before headings
```

## Extraction Workflow

### Step 1: Locate Component Files

```bash
find /path/to/bundle -name "*.java" -type f | grep -E "(Impl|Config)"
```

Expected files for a component:
- `*Impl.java` - Component implementation
- `Config.java` - Configuration interface

### Step 2: Extract Component Metadata

From `*Impl.java`:

```bash
grep -A 8 "@Component" $impl_file
```

Look for:
- `name = "Factory.PID.Name"` - This is the Factory-PID
- `public class ... implements` - The implemented interfaces

From `Config.java`:

```bash
grep -A 3 "@ObjectClassDefinition" $config_file
# Shows: name = "Display Name", description = "..."

grep "@AttributeDefinition" $config_file -A 1
# Shows all configuration parameters with defaults
```

### Step 3: Create/Update readme.adoc

Use the standard template above, filling in:

1. **Bundle Title** - From bundle name (e.g., `io.openems.edge.meter.schneider` → "Schneider Meter")
2. **Overview** - What does this do? Why would someone use it?
3. **Supported Devices** - Model names, protocols, typical applications
4. **Components** - One subsection per OSGi component with all metadata
5. *(Optional)* **Prerequisites** - What must be configured first?
6. *(Optional)* **Features** - Key capabilities
7. *(Optional)* **Known Limitations** - Constraints or gotchas

### Step 4: Build and Verify

```bash
cd /home/stefan/git/openems
./gradlew buildAntoraDocs --quiet
```

Common errors and fixes:

| Error | Cause | Fix |
|-------|-------|-----|
| Unresolved xref | Anchor doesn't exist or format wrong | Verify anchor format: `[[_section_name]]` before heading. Spaces → underscores, lowercase |
| List formatting | Mixed `-` and `*` or wrong title syntax | Use `*` for bullets, `.Title` (period) for list titles |
| Block formatting | Wrong indentation or missing dots | Use `.Title` for section titles within lists |

## Component Types and Patterns

### Device/Meter Bundles
Single component reading from one device type.

Example: `io.openems.edge.meter.weidmueller`
- 1 component (Weidmueller 525)
- Common config: `modbus_id`, `modbusUnitId`, `type`, `invert`

### Multi-Component Bundles
Multiple components for different device models.

Example: `io.openems.edge.meter.janitza`
- 6 components (UMG96RME, UMG104, UMG511, UMG604, UMG801, UMG806)
- Each has separate Factory-PID and configuration

### Multi-Family Bundles
Different product lines in one bundle.

Example: `io.openems.edge.kostal`
- 2 families: PIKO (3 components), PLENTICORE (4 components)
- Create device section grouping by family, then components

### API Bundles
Define interfaces, no components implemented.

Example: `io.openems.edge.meter.api`
- No components section needed
- Document interfaces and their role
- Keep brief (30-50 lines)

### Library/Protocol Bundles
Provide utility code, not end-user components.

Example: `io.openems.edge.katek.edcom`
- No components section needed
- Explain what protocol/functionality they provide
- Document role in the system (e.g., "Provides Modbus protocol implementation")

## Configuration Parameter Patterns

### Standard Parameters (All Components)

```
id (String): Component ID, unique in system [default: "meter0", "temp0", etc.]
alias (String): Human-readable name [default: ""]
enabled (Boolean): Enable/disable component [default: true]
```

### Meter-Specific Parameters

```
type (MeterType): GRID, PRODUCTION, CONSUMPTION, CONSUMPTION_METERED
modbus_id (String): ID of Modbus bridge [default: "modbus0"]
modbusUnitId (Integer): Modbus Unit-ID (1-247) [default: 1]
invert (Boolean): Multiply power by -1, swap energy directions [default: false]
```

### Phase Parameters

```
phase (PhaseSelection): L1, L2, L3 (for single-phase meters)
```

### Energy Storage Parameters

```
capacity (Integer): Battery capacity in Wh
readOnly (Boolean): Prevent charging/discharging
```

### Bridge Parameters

```
bridge_id (String): ID of communication bridge
bridge_target (String): Auto-generated filter (do not document)
```

## Example Real-World Cases

### Case 1: Simple Single-Component Meter

Bundle: `io.openems.edge.meter.weidmueller`

```
= Weidmueller 525 Meter

Three-phase energy meter by Weidmueller with Modbus communication.

== Overview
Provides integration with the Weidmueller 525 meter. Reads power, energy, voltage, 
current and other electrical parameters over Modbus.

== Supported Devices
.Weidmueller 525
* Type: Three-phase electricity meter
* Communication: Modbus TCP/RTU
* Typical Use: Grid, production, or consumption monitoring

== Components

=== Weidmueller 525
*Name*: Meter Weidmueller 525
*Factory-PID*: `Meter.Weidmueller.525`
.Implemented Natures/Interfaces
* MeterWeidmueller525
* ElectricityMeter
* OpenemsComponent
* ModbusComponent
* ModbusSlave

[... rest of structure ...]
```

### Case 2: Multi-Component Bundle

Bundle: `io.openems.edge.meter.janitza` (6 components)

```
== Supported Devices
.Janitza UMG Series
* UMG96RME - Universal meter
* UMG104 - Compact meter
* UMG511 - Modular system
* UMG604 - Three-phase
* UMG801 - High-end monitoring
* UMG806 - Extended monitoring

== Components

=== <<_umg96rme,Janitza UMG96RME>>
*Factory-PID*: `Meter.Janitza.UMG96RME`
[...]

=== <<_umg104,Janitza UMG104>>
*Factory-PID*: `Meter.Janitza.UMG104`
[...]

[... one subsection per component ...]
```

### Case 3: Multi-Family Bundle

Bundle: `io.openems.edge.kostal`

```
== Supported Devices

.PIKO Family
* PIKO 3.0 - 3kW
* PIKO 4.6 - 4.6kW
* PIKO 5.5 - 5.5kW

.PLENTICORE Family
* PLENTICORE plus 3.0 - 3kW
* PLENTICORE plus 4.6 - 4.6kW
* PLENTICORE plus 5.5 - 5.5kW
* PLENTICORE plus 6.0 - 6.0kW

== Components

=== PIKO

==== <<_piko_3,PIKO 3>>
*Factory-PID*: `Pv.Inverter.Kostal.Piko.3`
[...]

==== <<_piko_4,PIKO 4>>
*Factory-PID*: `Pv.Inverter.Kostal.Piko.4`
[...]

=== PLENTICORE

==== <<_plenticore_3,PLENTICORE plus 3>>
*Factory-PID*: `Pv.Inverter.Kostal.Plenticore.3`
[...]

[... etc ...]
```

## Checklist for Standardization

- [ ] Located all `*Impl.java` and `Config.java` files
- [ ] Extracted @Component name (Factory-PID)
- [ ] Extracted all implemented interfaces
- [ ] Extracted @ObjectClassDefinition name (display name)
- [ ] Listed all @AttributeDefinition parameters with defaults
- [ ] Created overview explaining bundle purpose
- [ ] Listed all supported devices/models
- [ ] Created one subsection per component with all metadata
- [ ] (Optional) Added prerequisites/dependencies section
- [ ] (Optional) Added features section
- [ ] (Optional) Added known limitations section
- [ ] Verified all xref anchors use correct format
- [ ] Verified all lists use `*` bullets and `.Title` format
- [ ] Verified all external links include `icon:external-link[]` or `icon:github[]`
- [ ] Built with `./gradlew buildAntoraDocs --quiet`
- [ ] No AsciiDoc errors in build output
- [ ] Line count reasonable for complexity (simple: 50-80 lines, complex: 150-300 lines)

## Troubleshooting

### Build fails with xref errors
```
✗ Wrong: <<Component Foo>>
✓ Correct: <<_component_foo>>

Anchors must:
- Start with underscore
- Use lowercase
- Replace spaces with underscores
- Be defined with [[_anchor]] before the heading
```

### Lists not rendering correctly
```
✗ Wrong:
*Implemented Natures*:
- Item1
- Item2

✓ Correct:
.Implemented Natures
* Item1
* Item2
```

### Documentation doesn't appear in Antora
- Check that readme.adoc is in bundle root directory (not in src/ or doc/)
- Verify bundle is included in Antora configuration
- Check no AsciiDoc syntax errors in build output

## Quick Command Reference

```bash
# Find components in a bundle
find /path/to/bundle -name "*Impl.java" | head -5

# Extract Factory-PID
grep "@Component(name = " /path/to/bundle/src/**/\*Impl.java | grep -o '"[^"]*"' | head -1

# Extract Display Name
grep -A 1 "@ObjectClassDefinition" /path/to/bundle/src/**/Config.java | grep "name ="

# Extract all config parameters
grep "@AttributeDefinition" /path/to/bundle/src/**/Config.java -A 1

# Build and check
cd /home/stefan/git/openems && ./gradlew buildAntoraDocs --quiet

# Count lines (goal: 50-300 depending on complexity)
wc -l /path/to/bundle/readme.adoc
```

## Related Resources

- **AsciiDoc Specification**: https://docs.asciidoctor.org/asciidoc/latest/
- **Antora Documentation**: https://docs.antora.org/antora/latest/
- **OpenEMS Source**: https://github.com/OpenEMS/openems
- **Session Reference Files**:
  - SPECIFICATION.md - Detailed technical specification
  - QUICK-REFERENCE.md - AsciiDoc syntax quick reference

## Contributing

When standardizing a new bundle:

1. Follow this guide exactly
2. Test build with `./gradlew buildAntoraDocs --quiet`
3. Commit with message: "Standardize [bundle] readme.adoc"
4. Include co-author: `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`

## Statistics

As of March 2026:
- **Standardized**: 31+ bundles (30% of total)
- **Target**: 202 bundles
- **Average lines per bundle**: 80-150 lines depending on component count
- **Build time**: ~2-3 minutes for full build
