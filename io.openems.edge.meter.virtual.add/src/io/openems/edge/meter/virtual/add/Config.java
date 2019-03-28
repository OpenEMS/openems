package io.openems.edge.meter.virtual.add;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(
	name = "Virtual Meter Add",
	description ="This is a virtaul meter which is used to sum up the values from the different components and calculate the avergage of those values"		
)

@interface Config {
	String id() default "meter0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Meter ids", description = "Ids of the meters to be configured")
	String[] meterIDs(); 

	String webconsole_configurationFactory_nameHint() default "Virtual Meter [{id}]";
}
