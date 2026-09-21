package io.openems.edge.heat.mypv;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Heat my-PV", //
		description = "Implements a my-PV heating element")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "heat0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Read Only", description = "Defines that this my-PV heating element is read only.")
	boolean readOnly() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.OFF;

	@AttributeDefinition(name = "JSCalendar Schedule", description = "Takes a JSON-Array in JSCalendar format")
	String jsCalendar() default "[]";

	// TODO Add device-specific my-PV profiles; product models have different power
	// limits and Modbus mappings.
	@AttributeDefinition(name = "Max Heat Power", description = "Maximum power setpoint [W].", max = "9000")
	int maxHeatPower() default 3000;

	String webconsole_configurationFactory_nameHint() default "Heat my-PV [{id}]";

}
