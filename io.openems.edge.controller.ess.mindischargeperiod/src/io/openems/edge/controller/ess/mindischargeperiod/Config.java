package io.openems.edge.controller.ess.mindischargeperiod;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Minimum Discharge Period", //
		description = "Provides a minimum discharge power of the storage system for a configured time period, "
				+ "if a certain power peak reached has been reached.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlMinDischargePeriod0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Discharge power activate value", description = "If the discharge power of storage system "
			+ "is higher than this value, the controller guarantees the configured minimum discharge power for the configured time.")
	int activateDischargePower() default 10_000;

	@AttributeDefinition(name = "Minimum discharge power", description = "This power or more will be discharged for the configured time.")
	int minDischargePower() default 5_000;

	@AttributeDefinition(name = "Discharge time", description = "The discharge power will be guaranteed for this time.")
	int dischargeTime() default 120;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Minimum Discharge Period [{id}]";

}