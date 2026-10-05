package io.openems.edge.evse.chargepoint.ambibox;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evse.chargepoint.ambibox.enums.Connector;

@ObjectClassDefinition(//
		name = "EVSE Charge-Point Ambibox", //
		description = "Implements the Ambibox ambiCHARGE Home bidirectional EV charger for charge management")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this charge point is read only.", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Connector", description = "Physical charging connector of the Ambibox charger")
	Connector connector() default Connector.CONNECTOR_1;

	@AttributeDefinition(name = "Maximum hardware power", description = "Maximum charging power of the Charger in W.", required = true)
	int maxHwPower() default 11000;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point Ambibox [{id}]";

}
