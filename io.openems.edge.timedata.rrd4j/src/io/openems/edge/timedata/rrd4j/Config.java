package io.openems.edge.timedata.rrd4j;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

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

	@AttributeDefinition(name = "No. of Cycles", description = "How many Cycles till data is recorded.")
	int noOfCycles() default RecordWorker.DEFAULT_NO_OF_CYCLES;

	String webconsole_configurationFactory_nameHint() default "Timedata RRD4J [{id}]";
}