package io.openems.edge.simulator.evse.chargepoint;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(name = "Simulator EVSE Charge-Point", //
		description = "This simulates a electrical vehicle charging station")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this evse is read only. If readonly is enabled, minCurrent field is used to display power consumption", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "Vehicle Connected", description = "True if a vehicle is connected.")
	boolean vehicleConnected() default true;

	@AttributeDefinition(name = "Max Current", description = "Maximum allowed current for charging [mA]")
	int maxCurrent() default 16000;

	@AttributeDefinition(name = "Min Current", description = "Minimum allowed current for charging [mA]")
	int minCurrent() default 6000;

	@AttributeDefinition(name = "Voltage", description = "Mocked voltage")
	int voltage() default 230;

	@AttributeDefinition(name = "Hardware Wiring", description = "Number of phases the wallbox is physically connected to the grid (single-phase or three-phase)", required = true)
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Has phase switching ability", description = "Enables phase switching ability")
	boolean supportsPhaseSwitching() default false;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	String webconsole_configurationFactory_nameHint() default "Simulator EVSE Charge-Point [{id}]";
}