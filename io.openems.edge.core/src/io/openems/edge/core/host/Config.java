package io.openems.edge.core.host;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Host", //
		description = "This component represents the Host computer and its Operating System.")
@interface Config {

	@AttributeDefinition(name = "Current Network Configuration", description = "This stores the current network configuration. " //
			+ "It is not possible to update the network configuration here - use the `SetNetworkConfig` JSONRPC-Request instead.")
	String networkConfiguration();

	@AttributeDefinition(name = "Current USB Configuration", description = "This stores the current USB configuration. " //
			+ "It is not possible to update the USB configuration here.")
	String usbConfiguration();

	String webconsole_configurationFactory_nameHint() default "Core Host";
}