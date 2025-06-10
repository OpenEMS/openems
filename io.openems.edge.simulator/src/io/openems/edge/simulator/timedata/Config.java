package io.openems.edge.simulator.timedata;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.simulator.CsvFormat;

@ObjectClassDefinition(//
		name = "Simulator Timedata", //
		description = "This simulates a timeseries database")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timedata0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "CSV Format", description = "The format of the CSV file")
	CsvFormat format() default CsvFormat.GERMAN_EXCEL;

	@AttributeDefinition(name = "Filename", description = "The name of the file. The file is expected to be in the user home directory, e.g. 'C:\\Users\\username\\'.")
	String filename() default "timedata.csv";

	String webconsole_configurationFactory_nameHint() default "Simulator Timedata [{id}]";

}