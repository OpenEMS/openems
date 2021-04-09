package io.openems.edge.controller.symmetric.fixactivegirdpower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Fix Active-Gird-Power Symmetric", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlio.openems.edge.controller.symmetric.fixactivegirdpower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();
	
	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();
	
	@AttributeDefinition(name = "Set-Point Active-Power [W]", min = "0", description = "Grid-Power-Set-Point")
	int setPointActivePower();
	
	@AttributeDefinition(name = "Kp (gain)", description = "PI-controller: gain")
	float piKp() default 0.45f;
	
	@AttributeDefinition(name = "Ti [s] (reset time / Nachstellzeit)", description = "PI-controller: reset time")
	float piTi_s() default 2.2f;
	
	@AttributeDefinition(name = "enable I-Delay", description = "PI-controller: if this option is enabled, the integrator of the controller is delayed by one calculation cycle")
	boolean piEnableIdelay() default true;
	
	@AttributeDefinition(name = "max. Active-Power [%]", min = "0.0", max = "100.0", description = "Maximum Active-Power in percent of maximum Apparent-Power")
	float piMaxActivePower_pct() default 100.0f;

	String webconsole_configurationFactory_nameHint() default "Controller io.openems.edge.controller.symmetric.fixactivegirdpower [{id}]";

}