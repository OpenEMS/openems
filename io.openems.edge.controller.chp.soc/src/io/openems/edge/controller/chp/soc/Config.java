package io.openems.edge.controller.chp.soc;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller CHP SOC", //
		description = "This is a Controller for CHP (Combined Heat and Power Unit, German: BHKW - Blockheizkraftwerk). The Controller is used to signal CHP turn ON or turn OFF when the battery is empty or battery is full respectively, based on the SoC percentage")

@interface Config {
	String id() default "ctrlChpSoc0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	@AttributeDefinition(name = "Low threshold", description = "Low boundary of the threshold")
	int lowThreshold();

	@AttributeDefinition(name = "High threshold", description = "High boundary of the threshold")
	int highThreshold();

	String webconsole_configurationFactory_nameHint() default "Controller CHP SOC [{id}]";
}
