package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Octopus Dynamic Grid-Buy", //
		description = "Time-Of-Use Tariff GridBuy implementation for the dynamic tariff of Octopus Energy via Kraken API.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Country", description = "Octopus Energy market; currently only Germany is supported")
	Country country() default Country.GERMANY;

	@AttributeDefinition(name = "Email", description = "E-mail address used for Octopus Energy account login")
	String email() default "";

	@AttributeDefinition(name = "Password", description = "Password for Octopus Energy account login", type = AttributeType.PASSWORD)
	String password() default "";

	@AttributeDefinition(name = "Account Number", description = "Octopus Energy account number, e.g. A-1234ABCD")
	String accountNumber() default "";

	@AttributeDefinition(name = "Refresh interval [min]", description = "Refresh interval after a successful update (minimum 15)")
	int refreshMinutes() default 30;

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Octopus Dynamic Grid-Buy [{id}]";
}
