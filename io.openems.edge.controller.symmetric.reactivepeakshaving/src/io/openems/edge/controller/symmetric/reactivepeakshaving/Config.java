package io.openems.edge.controller.symmetric.reactivepeakshaving;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Reactive-Peak-Shaving Symmetric", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlReactivePeakShaving0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Peak-Shaving reactive power [var]", min = "0", description = "Reactive power (cap./ind.) above this value is considered a peak and shaved to this value.")
	int ReactivePowerLimit();
	
	@AttributeDefinition(name = "K_p (debugging)", description = "debugging")
	float pidKp() default 0.45f;
	
	@AttributeDefinition(name = "T_i [s] (debugging)", description = "debugging")
	float pidTi() default 0.24f;
	
	@AttributeDefinition(name = "deltaT [s] (debugging)", description = "debugging")
	float pidDeltaT() default 1.0f;
	
	String webconsole_configurationFactory_nameHint() default "Controller Reactive-Peak-Shaving Symmetric [{id}]";

}