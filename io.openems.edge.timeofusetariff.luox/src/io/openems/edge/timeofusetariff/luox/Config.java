package io.openems.edge.timeofusetariff.luox;

import io.openems.common.types.DebugMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff LUOX", //
		description = "Time-Of-Use Tariff implementation for LUOX Energy.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Backend OAuth Client Identifier")
	String backendOAuthClientIdentifier() default "luox_prod";

	@AttributeDefinition(name = "Access Token", type = AttributeType.PASSWORD)
	String accessToken();

	@AttributeDefinition(name = "Refresh Token", type = AttributeType.PASSWORD)
	String refreshToken();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff LUOX [{id}]";

}