package io.openems.edge.predictor.persistencemodel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Predictor Persistence-Model", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Channel-Addresses", description = "List of Channel-Addresses this Predictor is used for, e.g. '*/ActivePower', '*/ActualPower'")
	// TODO "_sum/ConsumptionActivePower" holds also actively controlled
	// consumption; replace, once we introduce a
	// 'Sum-Non-Regulated-Consumption'-Channel
	String[] channelAddresses() default { "_sum/ProductionActivePower", "_sum/UnmanagedConsumptionActivePower" };

	String webconsole_configurationFactory_nameHint() default "Predictor Persistence-Model [{id}]";

}