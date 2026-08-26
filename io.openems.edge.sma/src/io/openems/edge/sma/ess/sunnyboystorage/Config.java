package io.openems.edge.sma.ess.sunnyboystorage;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ESS SMA Sunny Boy Storage", //
		description = "Implements the SMA Sunny Boy Storage 2.5 energy storage system via Modbus TCP.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read-Only mode", description = "Enables Read-Only mode; no setpoints are written to the device")
	boolean readOnlyMode() default false;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. SMA SBS default: 3")
	int modbusUnitId() default 3;

	@AttributeDefinition(name = "Capacity [Wh]", description = "Net usable capacity of the battery in Wh. SBS 2.5 = 2000 Wh usable")
	int capacity() default 2000;

	String webconsole_configurationFactory_nameHint() default "ESS SMA Sunny Boy Storage [{id}]";
}
