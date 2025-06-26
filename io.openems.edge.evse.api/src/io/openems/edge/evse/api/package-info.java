@org.osgi.annotation.versioning.Version("1.0.0")
@org.osgi.annotation.bundle.Export
package io.openems.edge.evse.api;

// Observations:
// - Hyundai KONA (2021)
//   - Pause charge does not work. In that case car has to be either unplugged/replugged or charging has to be started again via App
//   - PhaseSwitch from THREE to SINGLE without Paus works
//   - PhaseSwitch from SINGLE to THREE does not work; continues charging with one phase.
//      - Currently our implementation does not recognize that it keeps charging with one phase and so limits to 16 A (as suggested for three-phase)