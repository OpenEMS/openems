package io.openems.edge.controller.HeatingElementController;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Heating Element", //
		description = "This controller will dynamically switches one of the three coils of the heating element(heizstab)")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHeatingElement0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel for the grid power")
	String inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress1();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddres2();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddres3();
	
	@AttributeDefinition(name = "End Time", description = "End time to check the minmum run time")
	String endTime() default "17:00:00";
	
	@AttributeDefinition(name = "Priority of running the heating element", description = "Decide the priority, time or the Kilo watt hour")
	Priority priority() default Priority.TIME;
	
	@AttributeDefinition(name = "Minimum time", description = "Minimum time for heating element to run in hours")
	int minTime()  default  1;

	@AttributeDefinition(name = "Min Kwh", description = "Minimun Kilo watt hour for heating element to run in kwh")
	int minkwh() default 4;
	

	@AttributeDefinition(name = "Power of Phase", description = "Power of the single phase")
	int powerOfPhase();

	String webconsole_configurationFactory_nameHint() default "Controller Heating Element [{id}]";
}