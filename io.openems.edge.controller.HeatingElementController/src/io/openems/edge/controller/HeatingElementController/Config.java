package io.openems.edge.controller.HeatingElementController;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Heizstab", //
		description = "This controller will dynamically switches one of the three coils of the heating element(heizstab)")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHeizstab0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress1();
	
	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddres2();
	
	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddres3();
	
	@AttributeDefinition(name = "Power of Phase", description = "Power of the single phase")
	int powerOfPhase();

	String webconsole_configurationFactory_nameHint() default "Controller Heizstab [{id}]";
}