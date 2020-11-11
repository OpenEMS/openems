package io.openems.edge.core.host;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Host", //
		description = "This component represents the Host computer and its Operating System.")
@interface Config {

	@AttributeDefinition(name = "Current Network Configuration", description = "This stores the current network configuration. " //
			+ "It is not possible to update the network configuration here - use the `SetNetworkConfig` JSONRPC-Request instead.")
	String networkConfiguration();

	String webconsole_configurationFactory_nameHint() default "Host";
}