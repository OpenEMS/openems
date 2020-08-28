package io.openems.edge.controller.ess.predictivedelaycharge.ac;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Predictive Delay Charge AC", //
		description = "Delays the charging of the AC storage system based on predicted production and consumption")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlAcPredictiveDelayCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Grid-Meter-Id", description = "ID of the Grid-Meter.")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "Number of buffer hours", description = "The number of buffer hours to make sure the battery still "
			+ "charges full, even on prediction errors.")
	int noOfBufferHours() default 2;
	
	@AttributeDefinition(name = "Debug Prediction Values", description = "Displayus the production and consumption energy values")
	boolean showPreditionValues() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Predictive Delay Charge AC [{id}]";
}