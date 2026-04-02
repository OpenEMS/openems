package io.openems.edge.meter.opendtu;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SinglePhase;

@ObjectClassDefinition(name = "Meter OpenDTU", //
		description = "Implements the metering component for OpenDTU via HTTP API")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meterOpenDTU0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is the Inverter connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the OpenDTU.")
	String ipAddress();

	@AttributeDefinition(name = "Inverter Serial Number", description = "Serial Number of the Inverter")
	String serialNumber() default "";

	String webconsole_configurationFactory_nameHint() default "Meter OpenDTU[{id}]";

}