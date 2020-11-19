package io.openems.edge.evcs.ocpp.abl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS OCPP ABL", //
		description = "Implements an OCPP capable ABL electric vehicle charging station without the smart charging function.")
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

	@AttributeDefinition(name = "Maximum current", description = "Maximum current of the charger in mA.", required = true)
	int maxHwCurrent() default 32000;

	@AttributeDefinition(name = "Minimum current", description = "Minimum current of the Charger in mA.", required = true)
	int minHwCurrent() default 6000;

	String webconsole_configurationFactory_nameHint() default "EVCS OCPP ABL [{id}]";
}
