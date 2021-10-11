package io.openems.edge.controller.io.channelsinglethreshold;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller IO Channel Single Threshold", //
		description = "This controller switches a Digital Output channel ON, if the value of the input channel is above a configured threshold. This behaviour can be inverted using the 'invert' config option.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoChannelSingleThreshold0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel. If the value of this channel is above the configured threshold, the output is switched ON.")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channels", description = "Channel addresses of the Digital Outputs that should be switched")
	String[] outputChannelAddress();

	@AttributeDefinition(name = "Threshold", description = "Threshold boundary value")
	int threshold();

	@AttributeDefinition(name = "Switched Load Power", description = "load power value of the device that needs to be switched on/off (Applies only if input channel is Grid Active power, otherwise set it to 0)")
	int switchedLoadPower() default 0;

	@AttributeDefinition(name = "Minimum switching time between two states", description = "Minimum time (Seconds) is applied to avoid continuous switching between on and off based on threshold")
	int minimumSwitchingTime() default 60;

	@AttributeDefinition(name = "Invert behaviour", description = "If this option is activated the behaviour is inverted, i.e. the output is switched OFF if the value of the input channel is below the configured threshold")
	boolean invert() default false;

	String webconsole_configurationFactory_nameHint() default "Controller IO Channel Single Threshold [{id}]";
}