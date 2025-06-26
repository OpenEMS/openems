package io.openems.edge.controller.channelthreshold;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Channel Threshold", //
		description = "This controller switches a Digital Output channel ON, if the value of the input channel is within a configured threshold. This behaviour can be inverted using the 'invert' config option.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlChannelThreshold0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel. If the value of this channel is within a configured threshold, the output channel is switched ON.")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	@AttributeDefinition(name = "Low threshold", description = "Low boundary of the threshold")
	int lowThreshold();

	@AttributeDefinition(name = "High threshold", description = "High boundary of the threshold")
	int highThreshold();

	@AttributeDefinition(name = "Hysteresis", description = "The hysteresis is applied to low and high threshold to avoid continuous switching")
	int hysteresis();

	@AttributeDefinition(name = "Invert behaviour", description = "If this option is activated the behaviour is inverted, i.e. the Digital Output channel is switched OFF if the value of the input channel is within a configured threshold")
	boolean invert() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Channel Threshold [{id}]";
}