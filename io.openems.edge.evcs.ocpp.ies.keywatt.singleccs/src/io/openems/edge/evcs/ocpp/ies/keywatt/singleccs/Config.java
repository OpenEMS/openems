package io.openems.edge.evcs.ocpp.ies.keywatt.singleccs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS OCPP Ies KeyWatt", //
		description = "Implements an OCPP capable Ies KeyWatt electric vehicle charging station without the smart charging function.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "OCPP chargepoint identifier", description = "The OCPP identifier of the charging station.", required = true)
	String ocpp_id() default "IES1";

	@AttributeDefinition(name = "OCPP connector identifier", description = "The connector id of the chargepoint (e.g. if there are two connectors, then the evcs has two id's 1 and 2).", required = true)
	int connectorId() default 1;

	@AttributeDefinition(name = "Maximum power", description = "Maximum power of the charger in Watt.", required = true)
	int maxHwPower() default 25920; // 24000 + 8% (IesKeywattSingleCcs.DEFAULT_EFFECTIVE_POWER_LOSS)

	@AttributeDefinition(name = "Minimum power", description = "Minimum power of the charger in Watt.", required = true)
	int minHwPower() default 2000;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "EVCS OCPP Ies KeyWatt [{id}]";
}
