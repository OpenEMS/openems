package io.openems.edge.evse.chargepoint.hardybarth;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evse.api.chargepoint.PhaseRotation;

@ObjectClassDefinition(name = "EVSE Charge-Point Hardy Barth", //
		description = "The Hardy Barth P electric vehicle charging station")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "192.168.25.30";

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;
	
	@AttributeDefinition(name = "Read only", description = "Defines that this evcs is read only.", required = true)
	boolean readOnly() default false;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point Hardy Barth [{id}]";
}