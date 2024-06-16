package io.openems.edge.timeofusetariff.groupe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff GroupeE", //
		description = "Time-Of-Use Tariff implementation for GroupeE.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Exchangerate.host API Access Key", description = "Access key for Exchangerate.host: Please register at https://exchangerate.host/ to get your personal access key", type = AttributeType.PASSWORD)
	String exchangerateAccesskey() default "";

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff GroupeE [{id}]";
}
