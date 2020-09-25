package io.openems.edge.simulator.datasource.csv.predefined;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.simulator.CsvFormat;

@ObjectClassDefinition(//
		name = "Simulator DataSource: CSV Predefined", //
		description = "This service provides CSV-Input data.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "datasource0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Factor", description = "Each value in the csv-file is multiplied by this factor.")
	float factor() default 10_000;

	@AttributeDefinition(name = "Time-Delta", description = "Time-Delta between two entries in the csv-file in seconds. "
			+ "If set the output-value doesn't change, until the Time-Delta has passed in realtime.")
	int timeDelta() default -1;

	@AttributeDefinition(name = "Source", description = "A CSV-Input containing an optional title line and a series of values.")
	Source source();

	@AttributeDefinition(name = "CSV Format", description = "The format of the CSV file")
	CsvFormat format() default CsvFormat.GERMAN_EXCEL;

	String webconsole_configurationFactory_nameHint() default "Simulator DataSource: CSV Predefined [{id}]";
}