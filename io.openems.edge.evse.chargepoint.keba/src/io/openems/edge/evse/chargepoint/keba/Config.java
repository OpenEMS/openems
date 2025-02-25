package io.openems.edge.evse.chargepoint.keba;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVSE Charge-Point KEBA", //
		description = "The KEBA KeContact P30 or P40 electric vehicle charging station")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this evcs is read only.", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Phase(s)", description = "", required = true)
	Phase phase() default Phase.THREE; // TODO drop

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge")
	String modbus_id() default "modbus0";

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point KEBA [{id}]";
}