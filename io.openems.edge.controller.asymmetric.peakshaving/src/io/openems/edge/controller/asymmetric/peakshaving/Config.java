package io.openems.edge.controller.asymmetric.peakshaving;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Peak-Shaving Asymmetric", //
		description = "Cuts power peaks and recharges the battery in low consumption periods, depending on the individual phase.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlPeakShaving0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "Peak-Shaving power", description = "Grid purchase power above this value is considered a peak and shaved to this value.")
	int peakShavingPower() default 22080;

	@AttributeDefinition(name = "Recharge power", description = "If grid purchase power is below this value battery is recharged.")
	int rechargePower() default 15000;

	String webconsole_configurationFactory_nameHint() default "Controller Peak-Shaving Asymmetric [{id}]";
}