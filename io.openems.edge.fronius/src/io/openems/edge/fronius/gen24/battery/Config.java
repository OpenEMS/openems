package io.openems.edge.fronius.gen24.battery;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.fronius.enums.BatteryPreset;

@ObjectClassDefinition(//
		name = "ESS Fronius Gen24 Battery", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "battery0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus-Unit-ID", description = "Modbus Unit-ID. Must match the BatteryInverter's Unit-ID, since both talk to the same physical device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Number of Battery Modules", description = "Number of battery modules installed.")
	int numberOfModules() default 4;

	@AttributeDefinition(name = "Battery Preset", description = "Predefined battery model. Automatically sets voltage limits based on module count. Set to CUSTOM to enter values manually.")
	BatteryPreset batteryPreset() default BatteryPreset.CUSTOM;

	@AttributeDefinition(name = "Charge Max Voltage", description = "Maximum charge voltage in V. Only used when Battery Preset is set to CUSTOM.")
	int chargeMaxVoltage() default 480;

	@AttributeDefinition(name = "Discharge Min Voltage", description = "Minimum discharge voltage in V. Only used when Battery Preset is set to CUSTOM.")
	int dischargeMinVoltage() default 320;

	String webconsole_configurationFactory_nameHint() default "ESS Fronius Gen24 Battery [{id}]";

}
