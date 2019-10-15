package io.openems.edge.controller.symmetric.selfconsumptionoptimization;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Self-Consumption Optmization", //
		description = "controller which optimizes the charging from PV")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlselfConsumptionOpt0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Allowed Feed-In", description = "The maximum amount of feed in possible according to energiewinde in Wh")
	int Maximum_Feed_In() default 5000;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Grid-Meter-Id", description = "ID of the Grid-Meter.")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "Start hour", description = "Defines when to start the controller in a day.")
	int Start_hour() default 6;

	@AttributeDefinition(name = "buffer hour", description = "number of hours before the final hour.")
	int Buffer_hours() default 4;

	String webconsole_configurationFactory_nameHint() default "Controller Self-Consumption Optmization [{id}]";
}