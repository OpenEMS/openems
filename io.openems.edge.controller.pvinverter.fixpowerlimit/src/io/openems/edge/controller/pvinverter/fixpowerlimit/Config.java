package io.openems.edge.controller.pvinverter.fixpowerlimit;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller PV-Inverter Fix Power Limit", //
		description = "Defines a fixed power limitation to PV inverter.")
@interface Config {
	String id() default "ctrlPvInverterFixPowerLimit0";

	boolean enabled() default true;

	@AttributeDefinition(name = "PV-Inverter-ID", description = "ID of PV-Inverter device.")
	String pvInverter_id();

	@AttributeDefinition(name = "Power Limit [W]", description = "")
	int powerLimit();

	String webconsole_configurationFactory_nameHint() default "Controller PV-Inverter Fix Power Limit [{id}]";
}