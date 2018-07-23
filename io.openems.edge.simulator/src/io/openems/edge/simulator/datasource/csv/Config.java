package io.openems.edge.simulator.datasource.csv;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Simulator DataSource: CSV Reader", //
		description = "This service provides CSV-Input data.")
@interface Config {
	String service_pid();

	String id() default "datasource0";

	boolean enabled() default true;
	
	@AttributeDefinition(name = "Multiplier", description = "Each value in the csv-file is multiplied by this factor.")
	float multiplier() default 1000;

	@AttributeDefinition(name = "Source", description = "A CSV-Input file containing a series of values.")
	Source source();

	String webconsole_configurationFactory_nameHint() default "Simulator DataSource: CSV Reader [{id}]";
}