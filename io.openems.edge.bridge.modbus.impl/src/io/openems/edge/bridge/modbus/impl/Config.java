package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition()
@interface Config {
	String id();

	boolean enabled();

	String ip();
}