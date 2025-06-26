package io.openems.edge.pvinverter.cluster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "PV-Inverter Cluster", //
		description = "Combines several PV-Inverters to one.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pvInverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "PV-Inverter-IDs", description = "IDs of PvInverter devices.")
	String[] pvInverter_ids();

	String webconsole_configurationFactory_nameHint() default "PV-Inverter Cluster [{id}]";
}