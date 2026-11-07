package io.openems.edge.firealarmsystem.hekatron;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Fire Alarm Hekatron B9-X2", //
		description = "Implements the Hekatron B9-X2 fire alarm system component")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "fireAlarmSystem0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Fire Alarm Config Version", description = "The config version of the Fire Alarm.")
	ConfigVersion configVersion() default ConfigVersion.INDUSTRIAL_XL_V1;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	String webconsole_configurationFactory_nameHint() default "Fire Alarm Hekatron B9-X2 [{id}]";

}