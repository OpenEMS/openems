package io.openems.edge.controller.dynamicdischarge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Dynamic Discharge", //
		description = "controller which supports dynamic prices in germany market base")
@interface Config {
	
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicDischarge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "url", description = "URL to connect API", type = AttributeType.STRING)
	String url() default "https://api.awattar.com/v1/marketdata";
	
	@AttributeDefinition(name = "Apikey", description = "Apikey for authenticating Awattar API.", type = AttributeType.PASSWORD)
	String apikey() default "ak_7YTR42jBwtnk5kXuMZRYEju8hvj918H0";

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-Id", description = "ID of the Grid-Meter.")
	String meter_id();
	
	@AttributeDefinition(name = "Start hour", description = "")
	int startHour() default 9;

	@AttributeDefinition(name = "Morning-Hour", description = "Calculation stop at this hour")
	int Max_Morning_hour() default 7;

	@AttributeDefinition(name = "Evening-Hour", description = "Calculation starts at this hour")
	int Max_Evening_hour() default 16;

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Discharge [{id}]";

}
