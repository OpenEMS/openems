package io.openems.edge.evcs.ocpp.abl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS OCPP ABL", //
		description = "Implements an OCPP capable ABL electric vehicle charging station whithout the smart charging function.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "OCPP chargepoint identifier", description = "The OCPP identifier of the charging station.", required = true)
	String ocpp_id() default "";

	@AttributeDefinition(name = "OCPP connector identifier", description = "The connector id of the chargepoint (e.g. if there are two connectors, then the evcs has two id's 1 and 2).", required = true)
	int connectorId() default 0;
	
	@AttributeDefinition(name = "ABL logical identifier", description = "The logical id defined in the web administration interface of the ABL chargepoint.", required = true)
	String logicalId() default "evse100";
	
	@AttributeDefinition(name = "ABL limit identifier", description = "The limit id defined in the web administration interface of the ABL chargepoint.", required = true)
	String limitId() default "limit100";

	@AttributeDefinition(name = "Maximum power", description = "Maximum power of the charger in Watt.", required = true)
	int maxHwPower() default 22000;

	@AttributeDefinition(name = "Minimum power", description = "Minimum power of the charger in Watt.", required = true)
	int minHwPower() default 0;

	@AttributeDefinition(name = "Ocpp Server", description = "ID of the Ocpp Server.", required = true)
	String ocppServerId() default "ocppServer0";

	String webconsole_configurationFactory_nameHint() default "EVCS OCPP ABL [{id}]";
}
