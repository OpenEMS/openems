package io.openems.edge.evse.chargepoint.hardybarth.ecb1;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVSE Charge-Point Hardy Barth cPH1", //
		description = "Implements the Hardy Barth eCharge cPH1 electric vehicle charging station (ECB1 REST API).")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "192.168.2.8";

	@AttributeDefinition(name = "Charge-Control-ID", description = "The ID of the charge control unit (default: 1).", required = true)
	int chargeControlId() default 1;

	@AttributeDefinition(name = "Meter-ID", description = "The ID of the energy meter (default: 1).", required = true)
	int meterId() default 1;

	@AttributeDefinition(name = "Minimum hardware current", description = "Minimum current of the Charger in mA.", required = true)
	int minHwCurrent() default 6000;

	@AttributeDefinition(name = "Maximum hardware current", description = "Maximum current of the Charger in mA.", required = true)
	int maxHwCurrent() default 32000;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Read only", description = "Defines that this charger is read only.", required = true)
	boolean readOnly() default false;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point Hardy Barth cPH1 [{id}]";
}
