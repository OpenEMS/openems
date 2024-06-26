package io.openems.edge.controller.pvinverter.reversepowerrelay;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller PV-Inverter Reverse Power Relay (Rundsteuerempfänger)", //
		description = "Defines a reverse power relay to limit PV inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlPvInverterReversePowerRelay0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "PV-Inverter-ID", description = "ID of PV-Inverter device.")
	String pvInverter_id();
	
	@AttributeDefinition(name = "Input Channel 0%", description = "Address of the input channel. If this channel is active the inverter is limited to 0%. i.e. 'myRelay/Input1")
	String inputChannelAddress0Percent();	

	@AttributeDefinition(name = "Input Channel 30%", description = "Address of the input channel. If this channel is active the inverter is limited to 30%. i.e. 'myRelay/Input2")
	String inputChannelAddress30Percent();	
	
	@AttributeDefinition(name = "Input Channel 60%", description = "Address of the input channel. If this channel is active the inverter is limited to 60%. i.e. 'myRelay/Input3")
	String inputChannelAddress60Percent();			

	@AttributeDefinition(name = "Input Channel 100%", description = "Address of the input channel. If this channel is active the inverter is limited to 100%. i.e. 'myRelay/Input4")
	String inputChannelAddress100Percent();		
	
	@AttributeDefinition(name = "Power Limit 30% step [W]", description = "")
	int powerLimit30();
	
	@AttributeDefinition(name = "Power Limit 60% step [W]", description = "")
	int powerLimit60();	

	String webconsole_configurationFactory_nameHint() default "Controller PV-Inverter Reverse Power Relay Limitation [{id}]";
}