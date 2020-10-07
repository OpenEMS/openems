package io.openems.edge.controller.io.mainswitchcontrol;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller IO Main Switch Control", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlMainSwitch0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess();
	
	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Peak-Shaving power", description = "Grid purchase power above this value is considered a peak and shaved to this value.")
	int peakShavingPower();

	@AttributeDefinition(name = "Recharge power", description = "If grid purchase power is below this value battery is recharged.")
	int rechargePower();
	
	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	String webconsole_configurationFactory_nameHint() default "Controller IO MainSwitchControl [{id}]";

}