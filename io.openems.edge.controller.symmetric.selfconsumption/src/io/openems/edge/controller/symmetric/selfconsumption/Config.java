package io.openems.edge.controller.symmetric.selfconsumption;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Symmetric Self-Consumption Optmization", //
		description = "controller which optimizes the charging from PV")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlSelfConsumptionOpt0";

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

	String webconsole_configurationFactory_nameHint() default "Controller Symmetric Self-Consumption Optmization [{id}]";
}