package io.openems.edge.controller.asymmetric.peakshaving;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
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

	@AttributeDefinition(name = "Peak-Shaving power", description = "Maximum grid purchase power on one Phase. The controller tries to shave to this value.")
	int peakShavingPower() default 7000;

	@AttributeDefinition(name = "Recharge power", description = "If grid purchase power is on each Phase below this value, the battery will recharge.")
	int rechargePower() default 6000;

	String webconsole_configurationFactory_nameHint() default "Controller Peak-Shaving Asymmetric [{id}]";
}