package io.openems.edge.controller.ess.acpredictivedelaycharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Predictive Delay Charge AC", //
		description = "controller delays the charging of the AC storage system based on the predicted PV generation values")
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

	@AttributeDefinition(name = "Buffer hour", description = "Number of hours that can act as a backup so that restrictions from the controller dosent apply.")
	int buffer_hour() default 2;

	String webconsole_configurationFactory_nameHint() default "Controller Predictive Delay Charge AC [{id}]";
}