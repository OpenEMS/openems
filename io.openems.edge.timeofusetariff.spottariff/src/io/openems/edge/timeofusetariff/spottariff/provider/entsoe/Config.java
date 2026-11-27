package io.openems.edge.timeofusetariff.spottariff.provider.entsoe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Spot Tariff ENTSO-E", //
		description = "Epex Spot Tariff implementation that uses the ENTSO-E transparency platform.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffSpot0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD, required = false)
	String securityToken() default "";

	@AttributeDefinition(name = "Service Ranking", description = "Service ranking; higher values are preferred.")
	int service_ranking() default 80;

	String webconsole_configurationFactory_nameHint() default "Spot Tariff ENTSO-E [{id}]";
}