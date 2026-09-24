package io.openems.edge.simulator.csvexporter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator CSV Data Exporter", //
		description = "Real-time CSV exporter - collects channel data every cycle and writes to CSV at configurable intervals"//
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "csvDataExporter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Output Path", description = "Directory path where CSV files will be saved")
	String outputPath() default "/data/exports";

	@AttributeDefinition(name = "Write Interval (minutes)", description = "How often to write buffered data to CSV file")
	int writeIntervalMinutes() default 15;

	@AttributeDefinition(name = "ESS-ID", description = "ID of the ESS device to export (Soc + ActivePower)")
	String ess_id() default "";

	@AttributeDefinition(name = "Individual ESS IDs", description = "Array of individual ESS component IDs to export ActivePower for each")
	String[] ess_ids() default {};

	@AttributeDefinition(name = "Battery IDs", description = "Array of individual battery component IDs to export")
	String[] battery_ids() default {};

	@AttributeDefinition(name = "CSV Active Power Controller IDs", description = "Array of ControllerEssCsvActivePower component IDs to export CurrentPower for each")
	String[] controller_csv_active_power_ids() default {};

	@AttributeDefinition(name = "CSV Frequency Controller IDs", description = "Array of ControllerEssCsvFrequency component IDs to export Frequency, FcrPower, Ffr1Power, RechargePower, CalculatedPower, TotalPower for each")
	String[] controller_csv_frequency_ids() default {};

	String webconsole_configurationFactory_nameHint() default "CSV Data Exporter [{id}]";
}
