package io.openems.edge.timedata.rrd4j;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.channel.PersistencePriority;

@ObjectClassDefinition(//
		name = "Timedata RRD4J", //
		description = "This component persists data to RRD4J files.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "rrd4j0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Persistence Priority", description = "Store only Channels with a Persistence Priority above this. Be aware that too many writes can wear-out your flash storage.")
	PersistencePriority persistencePriority() default PersistencePriority.MEDIUM;

	String webconsole_configurationFactory_nameHint() default "Timedata RRD4J [{id}]";
}