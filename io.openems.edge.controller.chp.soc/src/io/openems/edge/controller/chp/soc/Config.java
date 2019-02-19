package io.openems.edge.controller.chp.soc;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller CHP SOC", //
		description = "This is a controller to signal CHP turn ON or turn OFF when the battery is empty or battery is full respectively, based on the SoC percentage")

@interface Config {
	String id() default "ctrlChannelThreshold0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel. If the value of this channel is within a configured threshold, the output channel is switched ON.")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	@AttributeDefinition(name = "Low threshold", description = "Low boundary of the threshold")
	int lowThreshold();

	@AttributeDefinition(name = "High threshold", description = "High boundary of the threshold")
	int highThreshold();

	/*
	 * @AttributeDefinition(name = "Hysteresis", description =
	 * "The hysteresis is applied to low and high threshold to avoid continuous switching"
	 * ) int hysteresis();
	 * 
	 * @AttributeDefinition(name = "Invert behaviour", description =
	 * "If this option is activated the behaviour is inverted, i.e. the Digital Output channel is switched OFF if the value of the input channel is within a configured threshold"
	 * ) boolean invert() default false;
	 */

	String webconsole_configurationFactory_nameHint() default "Controller Channel Threshold [{id}]";
}
