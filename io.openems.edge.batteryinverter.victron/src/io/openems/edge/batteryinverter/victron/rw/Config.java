package io.openems.edge.batteryinverter.victron.rw;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.ess.api.SinglePhase;

@ObjectClassDefinition(//
		name = "Battery-Inverter Victron RW", //
		description = "Implements the Victron battery inverter (read write).")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";
	
	@AttributeDefinition(name = "Phase", description = "The phase that this Victron ESS is connected to - if single phase.")
	SinglePhase phase() default SinglePhase.L1;
	
	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "Victron Multiplus 2GX ESS";
	
	@AttributeDefinition(name = "Modbus-ID 0", description = "ID of Modbus bridge to Victron Inverter.")
	String modbus_id0() default "modbus0";
	
	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. Defaults to '228' for Victron Multiplus 2GX.")
	int modbusUnitId() default 228;
	
	@AttributeDefinition(name = "Type of Installation", description = "The type of the Victron Installation. Used to determine any AC in/out limits.")
	VictronEss.Type victron_installation() default VictronEss.Type.Multiplus2GX3kVa;

	@AttributeDefinition(name = "Threshold for DC PV Feed-In in [W]", description = "DC PV Generation below this threshold will not be fed into grid.")
	int dcFeedInThreshold() default 100;
	
	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated 'Modbus-ID'.")
	String Modbus_target() default "(enabled=true)";
	
	@AttributeDefinition(name = "Victron Battery ID", description = "The Battery-ID of the Victron Battery")
	String battery_id() default "battery0";
	
	@AttributeDefinition(name = "Battery target filter", description = "This is auto-generated 'Modbus-ID'.")
	String Battery_target() default "(enabled=true)";
	
	String webconsole_configurationFactory_nameHint() default "Battery-Inverter Victron RW [{id}]";
}