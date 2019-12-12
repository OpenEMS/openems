package io.openems.edge.controller.channelsocthreshold;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import io.openems.edge.controller.singlethreshold.Mode;
import io.openems.edge.controller.channelsocthreshold.InputChannelAddress;

@ObjectClassDefinition( //
		name = "Controller Channel SoC Threshold", //
		description = "This controller switches a Digital Output channel ON, if the value of the SoC is above a configured threshold. This behaviour can be inverted using the 'invert' config option.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlChannelSocThreshold0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;
	
	@AttributeDefinition(name = "Input Address", description = "Set the Input channel.")
	InputChannelAddress input_channel() default InputChannelAddress.SOC;

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String output_channel_address();

	@AttributeDefinition(name = "Threshold", description = "Threshold boundary value")
	int threshold();

	@AttributeDefinition(name = "Hysteresis", description = "The hysteresis is applied to threshold to avoid continuous switching")
	int hysteresis() default 30;

	@AttributeDefinition(name = "Invert behaviour", description = "If this option is activated the behaviour is inverted, i.e. the Digital Output channel is switched OFF if the value of the input channel is within a configured threshold")
	boolean invert() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Channel SoC Threshold [{id}]";
}