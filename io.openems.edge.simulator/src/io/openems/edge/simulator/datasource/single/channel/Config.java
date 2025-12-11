package io.openems.edge.simulator.datasource.single.channel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator DataSource: Single Channel", //
		description = "This service provides direct input for one data channel via a value set in a channel.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "datasource0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Time-Delta", description = "Time-Delta in seconds. "
			+ "If set the output-value doesn't change, until the Time-Delta has passed in realtime.", required = false)
	int timeDelta() default -1;

	String webconsole_configurationFactory_nameHint() default "Simulator DataSource: Single Channel [{id}]";
}
