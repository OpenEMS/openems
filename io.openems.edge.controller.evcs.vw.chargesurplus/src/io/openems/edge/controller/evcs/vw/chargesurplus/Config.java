package io.openems.edge.controller.evcs.vw.chargesurplus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller io.openems.edge.controller.evcs.vw.chargesurplus", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlio.openems.edge.controller.evcs.vw.chargesurplus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Evcs-ID", description = "ID of Evcs device (Has to be managed).", required = true)
	String evcs_id() default "evcs0";
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "EssDcCharger-ID", description = "ID of Ess DC charger device.")
	String essdccharger_id() default "essDcCharger0";
	
	@AttributeDefinition(name = "Meter-ID", description = "ID of the meter device.")
	String meter_id() default "meter0";
	
	@AttributeDefinition(name = "Monday min. SOC", description = "Minimal SoC for Mondays.")
	int min_soc_monday() default 25;
	
	@AttributeDefinition(name = "Tuesday min. SOC", description = "Minimal SoC for Tuesdays.")
	int min_soc_tuesday() default 25;
	
	@AttributeDefinition(name = "Wedtnesday min. SOC", description = "Minimal SoC for Wedtnesdays.")
	int min_soc_wedtnesday() default 25;
	
	@AttributeDefinition(name = "Thursday min. SOC", description = "Minimal SoC for Thursdays.")
	int min_soc_thursday() default 25;
	
	@AttributeDefinition(name = "Friday min. SOC", description = "Minimal SoC for Fridays.")
	int min_soc_friday() default 25;
	
	@AttributeDefinition(name = "Saturday min. SOC", description = "Minimal SoC for Saturdays.")
	int min_soc_saturday() default 25;
	
	@AttributeDefinition(name = "Sunday min. SOC", description = "Minimal SoC for Sundays.")
	int min_soc_sunday() default 25;

	String webconsole_configurationFactory_nameHint() default "Controller io.openems.edge.controller.evcs.vw.chargesurplus [{id}]";

}