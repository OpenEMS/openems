package io.openems.edge.controller.pvinverter.fixpowerlimit;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller PV-Inverter Fix Power Limit", //
		description = "Defines a fixed power limitation to PV inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlPvInverterFixPowerLimit0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "PV-Inverter-ID", description = "ID of PV-Inverter device.")
	String pvInverter_id();

	@AttributeDefinition(name = "Power Limit [W]", description = "")
	int powerLimit();

	String webconsole_configurationFactory_nameHint() default "Controller PV-Inverter Fix Power Limit [{id}]";
}