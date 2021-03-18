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

	@AttributeDefinition(name = "Reactive-Power-Limit [var]", min = "0", description = "Reactive power limit (cap./ind.) for peak shaving. Reactive power above this value is considered a peak and will shaved to this value.")
	int ReactivePowerLimit();
	
	@AttributeDefinition(name = "Kp (gain)", description = "PI-controller: gain")
	float piKp() default 0.45f;
	
	@AttributeDefinition(name = "Ti [s] (reset time / Nachstellzeit)", description = "PI-controller: reset time")
	float piTi_s() default 2.2f;
	
	@AttributeDefinition(name = "enable I-Delay", description = "PI-controller: if this option is enabled, the integrator of the controller is delayed by one calculation cycle")
	boolean piEnableIdelay() default true;
	
	@AttributeDefinition(name = "max. Reactive-Power [%]", min = "0.0", max = "100.0", description = "Maximum Reactive-Power in percent of maximum Apparent-Power")
	float piMaxReactivePower_pct() default 100.0f;
	
	String webconsole_configurationFactory_nameHint() default "Controller Reactive-Peak-Shaving Symmetric [{id}]";

}