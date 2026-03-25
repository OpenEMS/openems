# Bulk README Standardization Guide

This guide helps systematically complete standardization of all OpenEMS bundle readme.adoc files.

## Current Status (as of March 2026)

- **Completed**: ~40-45 bundles (18%)
- **Remaining**: ~155 bundles (82%)
- **Total**: 220 bundles

## Priority Processing Order

### Priority 1: Device/Hardware Bundles (80 bundles) - HIGHEST VALUE
Process these first as they directly help users and agents:

1. **Batteries** (6)
   - `io.openems.edge.battery.bmw` - No readme
   - `io.openems.edge.battery.fenecon.home` - Minimal readme
   - Others already standardized

2. **Meters** (25+)
   - Multiple vendor implementations
   - Each typically 1 component
   - Use meter.janitza as reference (6-component example)

3. **Electric Vehicle Charging (EVCS/EVSE)** (25+)
   - `io.openems.edge.evcs.abl` - API bundle
   - `io.openems.edge.evcs.cluster` - Cluster manager
   - Various charger implementations: GOE, Heidelberg, Mennekes, Spelsberg, Webasto, OpenWB

4. **PV Inverters** (15+)
   - `io.openems.edge.pvinverter.kaco.blueplanet` - Multi-family
   - `io.openems.edge.fronius` - Popular brand
   - `io.openems.edge.goodwe`, `io.openems.edge.sma`, `io.openems.edge.solaredge`

5. **Bridges & Communication** (8)
   - `io.openems.edge.bridge.http`
   - `io.openems.edge.bridge.mbus`
   - `io.openems.edge.bridge.modbus` - Important
   - `io.openems.edge.bridge.mqtt`
   - `io.openems.edge.bridge.onewire`

6. **Heat Management** (2)
   - `io.openems.edge.heat.mypv`
   - `io.openems.edge.heat.api` - API bundle

### Priority 2: Controllers (60 bundles)

1. **ESS Controllers** (15+)
   - Fixed power, balancing, charging, standby, etc.
   - Document what they control and how

2. **EVCS Controllers** (4)
   - `io.openems.edge.controller.evcs`
   - Related EVCS-specific controllers

3. **Asymmetric Controllers** (15+)
   - Various power and reactive power controllers

4. **Symmetric Controllers** (15+)
   - Similar to asymmetric but for three-phase systems

5. **IO Controllers** (10+)
   - Digital output, threshold, heating element, etc.

6. **Other Controllers** (10+)
   - Debug logging, API wrappers, energy, etc.

### Priority 3: Backend & Infrastructure (15 bundles)

- Backend application, core, edge manager
- Authentication (OAuth2, API)
- Metadata management (Dummy, File, Odoo)
- Timedata (InfluxDB, RRD4j, Aggregated)
- Metrics (Prometheus)
- Common utilities, loggers

## Processing Workflow

### For Each Bundle:

1. **Run extraction script**
   ```bash
   cd /home/stefan/git/openems
   bash doc/tools/standardize_bundle_readme.sh <bundle_name_or_pattern>
   ```
   Example:
   ```bash
   bash doc/tools/standardize_bundle_readme.sh battery.bmw
   bash doc/tools/standardize_bundle_readme.sh meter.fronius
   ```

2. **Review extracted information**
   - Component names and Factory-PIDs
   - Implemented interfaces
   - Configuration parameters
   - Default values

3. **Create standardized readme**
   Use appropriate template:

   **For Device Bundles:**
   ```asciidoc
   = Device Name

   One-line description.

   == Overview
   2-3 sentences about purpose and use cases.

   == Supported Devices
   .Device Model Names
   * Type: Category
   * Communication: Protocol
   * Typical Use: Applications
   * Documentation: Link

   == Components
   === <<_component_id,Component Display Name>>
   *Name*: From @ObjectClassDefinition
   *Factory-PID*: `Full.Factory.PID`

   .Implemented Natures/Interfaces
   * Interface1
   * Interface2

   *Description*: What it does

   *Configuration*:
   .Configuration Parameters
   * `param1` (Type): Description (default: value)
   * `param2` (Type): Description (default: value)

   .Example Configuration
   * `param1`: "value"
   * `param2`: value

   [[_component_id]]

   == Prerequisites / Dependencies
   List what must be set up first

   == Features
   * Feature 1
   * Feature 2

   == Known Limitations
   * Limitation 1
   * Limitation 2

   https://github.com/OpenEMS/openems/tree/develop/bundle-name[Source Code icon:github[]]
   ```

   **For API Bundles:**
   ```asciidoc
   = API Name

   Brief interface definition.

   == Overview
   Explain what interfaces are provided and their role.

   == Core Interfaces
   === InterfaceName
   Description and key methods.

   == Related Implementations
   * Impl1 bundle (device A)
   * Impl2 bundle (device B)

   == Use Cases
   When and how to use this API.

   https://github.com/OpenEMS/openems/tree/develop/bundle-name[Source Code icon:github[]]
   ```

   **For Controllers:**
   ```asciidoc
   = Controller Name

   What it does.

   == Overview
   What this controller manages and typical use cases.

   == Controlled Components
   Types and roles of controlled components.

   == Configuration
   .Configuration Parameters
   * `param1` (Type): Description (default: value)

   == Prerequisites
   What must be configured first.

   == Features & Behavior
   How the controller operates.

   https://github.com/OpenEMS/openems/tree/develop/bundle-name[Source Code icon:github[]]
   ```

4. **AsciiDoc Syntax Rules** (CRITICAL)
   ```asciidoc
   ✓ CORRECT:
   .List Title
   * Bullet item

   ✓ CORRECT:
   https://example.com[Link text icon:external-link[]]

   ✓ CORRECT:
   [[_anchor_name]]
   === Section Name

   ✓ CORRECT:
   <<_anchor_name,Link text>>

   ✗ WRONG (will break build):
   *List Title*:
   - Bullet item
   [[AnchorName]]
   <<AnchorName>>
   ```

5. **Build and verify**
   ```bash
   cd /home/stefan/git/openems
   ./gradlew buildAntoraDocs --quiet
   ```
   Should complete without xref errors.

6. **Commit changes**
   ```bash
   git add <bundle>/readme.adoc
   git commit -m "Standardize <bundle> readme.adoc

   Added comprehensive documentation following template:
   - Overview and purpose
   - Supported devices/implementations
   - Components with Factory-PIDs and config
   - Prerequisites and features
   - Known limitations

   Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
   ```

## Batch Processing Tips

### Group Similar Bundles
Process 5-10 similar bundles before committing (same category or vendor family)

### Use References
- Device bundle with multiple components: `io.openems.edge.kostal` (reference 7 components)
- Multi-family bundle: `io.openems.edge.kostal` (PIKO vs PLENTICORE)
- Large component set: `io.openems.edge.meter.janitza` (reference 6 components)
- Simple single-component: `io.openems.edge.meter.weidmueller`

### Common Patterns

**Single Component, Modbus:**
- Device connects via Modbus
- 1 component = 1 bundle
- Standard config: id, alias, enabled, modbus_id, modbusUnitId, etc.
- ~80-100 lines typical

**Multiple Components (same type):**
- Different models/versions in one family
- Example: KEBA (4 components), Soltaro (5 components)
- Each needs subsection with own Factory-PID
- ~200-300 lines typical

**API Bundle:**
- Only interfaces, no components
- Link to implementations
- Brief, ~30-50 lines

**Controller:**
- What it controls
- Configuration for control parameters
- Prerequisites (what components must exist)
- ~60-100 lines typical

## Common Issues & Solutions

### Issue: Build fails with "Unresolved xref"
**Solution**: Check anchor format
```
Wrong: [[SectionName]]  →  Right: [[_section_name]]
Wrong: <<SectionName>>  →  Right: <<_section_name>>
```

### Issue: List formatting wrong
**Solution**: Use correct syntax
```
Wrong:
*Title*:
- Item1

Right:
.Title
* Item1
```

### Issue: Component info incomplete
**Solution**: Use extraction script to find all @Component annotations
```bash
grep -r "@Component" <bundle>/src --include="*.java"
```

### Issue: Can't find Factory-PID
**Solution**: Check @Component name attribute
```bash
grep -A 2 "@Component" <bundle>/src/**/\*Impl.java | grep "name ="
```

## Estimating Effort

- **Simple API bundle**: 15-20 minutes
- **Single-component device**: 30-45 minutes
- **Multi-component device**: 1-2 hours
- **Complex family bundle**: 2-3 hours
- **Controller**: 30-45 minutes

**Total estimated effort for all 155 remaining bundles**: 80-120 hours
**Suggested approach**: 10 bundles/day → 15-20 days

## Quality Checklist

Before committing each readme:

- [ ] Title and one-liner present
- [ ] Overview section explains purpose
- [ ] All components documented (if device bundle)
- [ ] Each component has: Name, Factory-PID, Interfaces, Description
- [ ] Configuration parameters documented with types and defaults
- [ ] Prerequisites/dependencies listed
- [ ] Features section present
- [ ] Known limitations documented
- [ ] GitHub source link at end with icon
- [ ] All external links have `icon:external-link[]`
- [ ] No hyphenated bullet lists (use `*`)
- [ ] All list titles use `.Title` format
- [ ] All anchors use `[[_name]]` format
- [ ] Build passes: `./gradlew buildAntoraDocs --quiet`
- [ ] Line count reasonable for complexity (50-400 lines)

## Progress Tracking

Keep track in this file or a separate spreadsheet:

```
| Bundle | Category | Status | Lines | Date |
|--------|----------|--------|-------|------|
| battery.bmw | Battery | TODO | - | - |
| evcs.abl | EVCS API | TODO | - | - |
| bridge.modbus | Bridge | TODO | - | - |
```

## Resources

- **Template & Guide**: `doc/tools/OPENEMS_README_STANDARDIZATION.md`
- **Script**: `doc/tools/standardize_bundle_readme.sh`
- **Quick Syntax Ref**: `doc/tools/QUICK-REFERENCE.md`
- **Examples**: Any of the 40+ completed bundles
- **Specification**: Session reference files or SPECIFICATION.md

## Getting Help

If stuck on a bundle:
1. Check an example bundle in same category
2. Run extraction script to see component details
3. Review OPENEMS_README_STANDARDIZATION.md for that bundle type
4. Check for similar patterns in completed bundles

Good luck standardizing! The goal is comprehensive, consistent documentation across all OpenEMS bundles.
