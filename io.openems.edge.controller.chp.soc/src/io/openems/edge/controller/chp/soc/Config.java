package io.openems.edge.controller.chp.soc;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller CHP SOC", //
		description = "This is a Controller for CHP (Combined Heat and Power Unit, German: BHKW - Blockheizkraftwerk). The Controller is used to signal CHP turn ON or turn OFF when the battery is empty or battery is full respectively, based on the SoC percentage")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlChpSoc0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	@AttributeDefinition(name = "Low threshold", description = "Low boundary of the threshold")
	int lowThreshold();

	@AttributeDefinition(name = "High threshold", description = "High boundary of the threshold")
	int highThreshold();

	@AttributeDefinition(name = "Invert behaviour", description = "If this option is activated the behaviour of switching ON and OFF is inverted")
	boolean invert() default false;

	String webconsole_configurationFactory_nameHint() default "Controller CHP SOC [{id}]";
}
