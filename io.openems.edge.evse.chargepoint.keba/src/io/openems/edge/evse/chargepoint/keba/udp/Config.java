package io.openems.edge.evse.chargepoint.keba.udp;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.common.enums.P30S10PhaseSwitching;

@ObjectClassDefinition(name = "EVSE Charge-Point KEBA (via UDP)", //
		description = "The KEBA KeContact P30 or P40 electric vehicle charging station")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this evcs is read only.", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "192.168.25.11";

	@AttributeDefinition(name = "Hardware Wiring", description = "", required = true)
	SingleThreePhase wiring() default SingleThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "For P30: Config for S10 phase switching", description = "Configuration for KEBA P30 with S10 phase switching device")
	P30S10PhaseSwitching p30S10PhaseSwitching() default P30S10PhaseSwitching.NOT_AVAILABLE;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point KEBA (via UDP) [{id}]";
}