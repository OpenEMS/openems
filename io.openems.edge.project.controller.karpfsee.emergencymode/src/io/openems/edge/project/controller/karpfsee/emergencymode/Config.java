package io.openems.edge.project.controller.karpfsee.emergencymode;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Emergency Mode", //
		description = "TODO.")
@interface Config {
	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";
	
	String id() default "ctrlEmergencyMode0";

	boolean enabled() default true;

	/*
	 * WAGO
	 */
	// Permission Signal Bock Heat Power Plant Relay Output 1/1
	@AttributeDefinition(name = "Block Heat Power Plant Permission Signal", description = "Permission Signal Bock Heat Power Plant")
	String blockHeatPowerPlantPermissionSignal() default "io0/DigitalOutputM1C1";

	// On Grid Indication Controller Relay Output 1/2
	@AttributeDefinition(name = "On Grid Indication Controller", description = "On Grid Indication Controller")
	String onGridIndicationController() default "io0/DigitalOutputM1C2";

	// Off Grid Indication Controller Relay Output 2/1
	@AttributeDefinition(name = "Off Grid Indication Controller", description = "Off Grid Indication Controller")
	String offGridIndicationController() default "io0/DigitalOutputM2C1";
	
	/*
	 * Meters
	 */
	@AttributeDefinition(name = "Meter ID", description = "Id of Meter")
	String meter_id();

	/*
	 * Ess
	 */
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess.")
	String ess_id();

	String webconsole_configurationFactory_nameHint() default "Controller Emergency Mode [{id}]";
}
