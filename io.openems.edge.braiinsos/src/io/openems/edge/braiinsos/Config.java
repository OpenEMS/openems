package io.openems.edge.braiinsos;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;

@ObjectClassDefinition(//
		name = "Braiins OS Controller Single", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlBraiinsSingle0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read Only", description = "Defines if the Braiins miner is read-only.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Mode", description = "Set the mode.")
	Mode mode() default Mode.OFF;

	@AttributeDefinition(name = "Default Consumption", description = "Fallback consumption in Watt.")
	int defaultConsumptionW() default 3000;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the device.")
	String ip();

	@AttributeDefinition(name = "Username", description = "The username. Defaults to 'root'")
	String username() default "root";

	@AttributeDefinition(name = "Password", description = "The password. Default is empty")
	String password() default "";

	@AttributeDefinition(name = "Phase", description = "Which Phase is this Miner connected to?")
	SingleOrAllPhase phase() default SingleOrAllPhase.L1;

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "JSCalendar Schedule", description = "Takes a JSON-Array in JSCalendar format")
	String jsCalendar() default "[]";

	String webconsole_configurationFactory_nameHint() default "Braiins OS Controller Single [{id}]";
}
