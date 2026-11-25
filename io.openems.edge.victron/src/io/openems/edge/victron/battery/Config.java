package io.openems.edge.victron.battery;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Victron Battery", //
		description = "Implements a Victron battery via Modbus TCP.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "battery0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "Victron Battery";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge to Victron Inverter / Same as for the Victron ESS.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "Use 225 for a CAN-bus BMS or 99 for the dbus-battery-aggregator service.")
	int modbusUnitId() default 225;

	@AttributeDefinition(name = "Battery source", description = "Select the battery service exposed by the Victron GX.")
	BatterySource batterySource() default BatterySource.CAN_BMS;

	@AttributeDefinition(name = "Nominal battery voltage", description = "Nominal battery voltage in V; decimals are supported and used to convert battery capacity from Ah to Wh.")
	double nominalBatteryVoltage() default 48;

	@AttributeDefinition(name = "Minimum discharge voltage", description = "Required for dbus-battery-aggregator, which does not provide this value; in V. Decimal values are rounded up to the next whole volt.")
	double dischargeMinVoltage() default 0;

	@AttributeDefinition(name = "Victron ESS-ID", description = "ESS-ID to which the batteryinverter is connected to")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Debug", description = "Enable debug mode?")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Victron Battery [{id}]";
}
