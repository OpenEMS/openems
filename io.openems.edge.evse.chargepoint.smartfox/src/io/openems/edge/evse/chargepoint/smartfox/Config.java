package io.openems.edge.evse.chargepoint.smartfox;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(name = "EVSE Charge-Point SMARTFOX", //
		description = "A SMARTFOX Car Charger attached to a SMARTFOX Energy Manager (SMARTFOX Pro/Pro 2/EM)")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this Charge-Point is read only.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Charge-Point number", description = "Number of the Car Charger as configured on the SMARTFOX Energy Manager (1-5)")
	int chargePointNumber() default 1;

	@AttributeDefinition(name = "IP address", description = "The IP address of the SMARTFOX Energy Manager; used for control commands via the local HTTP API")
	String ip() default "192.168.0.50";

	@AttributeDefinition(name = "Max. charging power [W]", description = "Maximum charging power of the Car Charger in Watt; used to translate power set-points to percent")
	int maxChargingPower() default 11000;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge to the SMARTFOX Energy Manager")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the SMARTFOX Energy Manager.")
	int modbusUnitId() default 1;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point SMARTFOX [{id}]";
}
