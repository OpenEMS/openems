package io.openems.edge.meter.openwb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(name = "Meter OpenWB", //
		description = "Implements the metering component for OpenWB Series2 with internal chargepoint via HTTP API")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meterOpenWB0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production, Consumption (=default)")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the OpenWB.")
	String ipAddress();

	@AttributeDefinition(name = "Port", description = "Port of the OpenWB")
	int port() default 8443;

	String webconsole_configurationFactory_nameHint() default "Meter OpenWB[{id}]";

}