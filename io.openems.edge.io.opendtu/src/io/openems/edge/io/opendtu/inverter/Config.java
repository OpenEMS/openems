package io.openems.edge.io.opendtu.inverter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

@ObjectClassDefinition(//
		name = "openDTU Hoymiles Inverter", //
		description = "Implements the openDTU for Hoymiles Inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Username", description = "Username for openDTU to make settings possible")
	String username() default "";

	@AttributeDefinition(name = "Password", description = "Password for oprnDTU to make settings possible", type = AttributeType.PASSWORD)
	String password() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is this Inverter connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the openDTU.")
	String ip();

	@AttributeDefinition(name = "Inverter Serial Number", description = "The serial number of the inverter connected to the DTU")
	String serialNumber() default "";

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this DTU?")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Initial Power Limit", description = "The initial power limit setting")
	int initialPowerLimit() default 100;

	String webconsole_configurationFactory_nameHint() default "IO openDTU [{id}]";
}