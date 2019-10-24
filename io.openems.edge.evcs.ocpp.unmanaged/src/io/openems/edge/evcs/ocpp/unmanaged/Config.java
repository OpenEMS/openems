package io.openems.edge.evcs.ocpp.unmanaged;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "EVCS OCPP Unmanaged", //
		description = "Implements an OCPP capable electric vehicle charging station whithout the smart charging function.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "OCPP chargepoint identifier", description = "The OCPP identifier of the charging station.", required = true)
	String ocpp_id() default "";
	
	@AttributeDefinition(name = "OCPP charger identifier", description = "The chargerid of the chargepoint (e.g. ABL with thwo chargers has two chargerid's 1 and 2).", required = true)
	String chargerId() default "0";

	String webconsole_configurationFactory_nameHint() default "EVCS OCPP Unmanaged [{id}]";
}
