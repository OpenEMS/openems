package io.openems.edge.controller.ess.acisland;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller ESS AC-Island", //
		description = "This controller sets a digital output channel according to the given value")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEssAcIsland0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Max-SoC", description = "If Off-Grid, the PV is disconnected at this SoC")
	int maxSoc() default 85;

	@AttributeDefinition(name = "Min-SoC", description = "If Off-Grid, the PV is connected at this SoC")
	int minSoc() default 70;

	@AttributeDefinition(name = "Switch-Dealy", description = "Time to wait in [ms] before switching the output on")
	int switchDelay() default 10_000;

	@AttributeDefinition(name = "Invert On-Grid output", description = "True if the digital output for the On-Grid connector should be inverted.")
	boolean invertOnGridOutput() default false;

	@AttributeDefinition(name = "Invert Off-Grid output", description = "True if the digital output for the Off-Grid connector should be inverted.")
	boolean invertOffGridOutput() default false;

	@AttributeDefinition(name = "On-Grid Output Channel", description = "Channel address of the Digital Output for On-Grid connection")
	String onGridOutputChannelAddress();

	@AttributeDefinition(name = "Off-Grid Output Channel", description = "Channel address of the Digital Output for Off-Grid connection")
	String offGridOutputChannelAddress();

	String webconsole_configurationFactory_nameHint() default "Controller ESS AC-Island [{id}]";

}