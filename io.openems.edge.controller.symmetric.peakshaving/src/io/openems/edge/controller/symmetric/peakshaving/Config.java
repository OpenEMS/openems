package io.openems.edge.controller.symmetric.peakshaving;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Peak-Shaving Symmetric", //
		description = "Cuts power peaks and recharges the battery in low consumption periods.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlPeakShaving0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Peak-Shaving power", description = "Grid purchase power above this value is considered a peak and shaved to this value.")
	int peakShavingPower();

	@AttributeDefinition(name = "Recharge power", description = "If grid purchase power is below this value battery is recharged.")
	int rechargePower();
	
	@AttributeDefinition(name = "Enable multiple ess constraints", description = "A fixed capacity configured by the minSocLimit is used for peakshaving. Additional capacity could be used by other controllers.")
	boolean enableMultipleEssConstraints() default false;

	@AttributeDefinition(name = "Minimum SoC required for Peak Shaving", description = "The controller force charges with the available surpluss till this SOC limit")
	int minSocLimit() default 70;

	@AttributeDefinition(name = "SoC hysterersis", description = "SoC hysteresis to avoid switching between force and soft limitation")
	int socHysteresis() default 2;

	String webconsole_configurationFactory_nameHint() default "Controller Peak-Shaving Symmetric [{id}]";

}