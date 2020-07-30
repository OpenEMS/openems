package io.openems.edge.controller.lowfrequencypowersmoothing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Low-Frequency Power-Smoothing", //
		description = "Smoothes the power curve during uneven PV porudction by Chraging/Discharging the battery")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrLowFrequencyPowerSmoothing0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Grid Meter-ID", description = "ID of Grid Meter.")
	String gridMeter_id() default "meter0";

	@AttributeDefinition(name = "Pv Inverter Meter-ID", description = "ID of Pv Inverter Meter.")
	String pvInverterMeter_id() default "meter1";

	@AttributeDefinition(name = "Threshold", description = "Threshold of the difference between two consecutive Pv values.")
	int threshold() default 2000;

	@AttributeDefinition(name = "Hysteresis", description = "This is minimum seconds to avoid immediate switching")
	int Hysteresis() default 5;

	String webconsole_configurationFactory_nameHint() default "Controller low-frequency power-smoothing [{id}]";

}