package io.openems.edge.core.serialnumber;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Serial Numbers", //
		description = "Global storage for serial numbers" //
)
@interface Config {

	@AttributeDefinition(name = "Serial Numbers", description = "json data of all stored serial numbers", required = false)
	String data() default "{}";

}