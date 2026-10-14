package io.openems.edge.timeofusetariff.octopusdynamic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Time-of-Use Tariff Octopus Dynamic DE",
		description = "Account-specific Octopus Energy Germany dynamic electricity prices")
@interface Config {
	@AttributeDefinition(name = "Component-ID")
	String id() default "timeofusetariff0";

	@AttributeDefinition(name = "Alias")
	String alias() default "Octopus Dynamic DE";

	@AttributeDefinition(name = "Enabled")
	boolean enabled() default true;

	@AttributeDefinition(name = "Octopus account number",
			description = "Account number of the electricity contract, e.g. A-XXXXXXXX")
	String accountNumber();

	@AttributeDefinition(name = "Octopus login email")
	String email();

	@AttributeDefinition(name = "Octopus login password", type = AttributeType.PASSWORD)
	String password();

	@AttributeDefinition(name = "Refresh interval in minutes",
			description = "Octopus publishes the following day in the afternoon")
	int refreshMinutes() default 30;

	String webconsole_configurationFactory_nameHint() default "Octopus Dynamic DE [{id}]";
}
