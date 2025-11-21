package io.openems.edge.evse.chargepoint.abl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(name = "EVSE Charge-Point ABL", //
		description = "The ABL EVCC2/3 electric vehicle charging station")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", //
			description = "Read-only mode: true = monitoring only (no control), false = full control capability. "
					+ "Set to true if you want to monitor the charging station without controlling it.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Debug Mode", //
			description = "Activates debug logging at INFO level. Logs current setpoints, state changes, and rate "
					+ "limiting events. Enable for troubleshooting, disable for production use.")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Hardware Wiring", //
			description = "Physical wiring configuration of the charging station. "
					+ "SINGLE_PHASE: 1-phase connection (L1 only). "
					+ "THREE_PHASE: 3-phase connection (L1, L2, L3). "
					+ "Must match actual hardware installation.")
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Phase Rotation", //
			description = "Phase rotation configuration for power calculation. "
					+ "L1_L2_L3: Standard rotation (default). "
					+ "L3_L2_L1: Rotated wiring. "
					+ "Use rotated if phases are swapped in installation.")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Modbus-ID", //
			description = "Component ID of the Modbus bridge to use for communication. "
					+ "Must reference an existing Modbus bridge component (e.g., 'modbus0').")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", //
			description = "Modbus unit/device ID of the ABL charging station on the Modbus bus. "
					+ "Valid range: 1-16. Must match the unit ID configured on the ABL device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Maximum Current", //
			description = "Maximum charging current supported by the installation in Ampere. "
					+ "Valid range: 6-80A. Must not exceed the cable/installation rating. "
					+ "Typical values: 16A (single-phase), 32A (three-phase residential).")
	int maxCurrent() default 32;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point ABL [{id}]";
}
