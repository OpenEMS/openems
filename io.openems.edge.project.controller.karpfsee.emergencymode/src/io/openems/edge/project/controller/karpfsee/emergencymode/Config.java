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
	 * Ess
	 */
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess.")
	String ess_id();
	
	/*
	 * WAGO
	 */
	// Permission Signal Bock Heat Power Plant Relay Output 2/1
	@AttributeDefinition(name = "Block Heat Power Plant Permission Signal", description = "Permission Signal Bock Heat Power Plant")
	String blockHeatPowerPlantPermissionSignal() default "io0/DigitalOutputM2C1";

	// Off Grid Indication Controller Relay Output 2/2
	@AttributeDefinition(name = "MSR Heating System Controller", description = "MSR Heating System Controller")
	String msrHeatingSystemController() default "io0/DigitalOutputM2C2";

	// On Grid Indication Controller Relay Output 3/1
	@AttributeDefinition(name = "On Grid Indication Controller", description = "On Grid Indication Controller")
	String onGridIndicationController() default "io0/DigitalOutputM3C1";

	// Off Grid Indication Controller Relay Output 3/2
	@AttributeDefinition(name = "Off Grid Indication Controller", description = "Off Grid Indication Controller")
	String offGridIndicationController() default "io0/DigitalOutputM3C2";

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel. If the value of this channel is within a configured threshold, the output channel is switched ON.")
	String inputChannelAddress();

	@AttributeDefinition(name = "Low threshold", description = "Low boundary of the threshold")
	int lowThreshold();

	@AttributeDefinition(name = "High threshold", description = "High boundary of the threshold")
	int highThreshold();

	@AttributeDefinition(name = "Hysteresis", description = "The hysteresis is applied to low and high threshold to avoid continuous switching")
	int hysteresis();

	String webconsole_configurationFactory_nameHint() default "Controller Emergency Mode [{id}]";
}
