package io.openems.core.logger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Logger", //
		description = "Configures the logger for OpenEMS." //
)
@interface Config {

	@AttributeDefinition(name = "Path", description = "Path to the log4j2 config file. If empty, the default configuration is used.")
	String path();

}