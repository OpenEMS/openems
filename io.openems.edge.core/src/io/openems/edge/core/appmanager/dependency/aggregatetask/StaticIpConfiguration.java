package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.core.appmanager.InterfaceConfiguration;

public record StaticIpConfiguration(List<InterfaceConfiguration> interfaceConfiguration) {

	public StaticIpConfiguration(InterfaceConfiguration... interfaceConfiguration) {
		this(Stream.of(interfaceConfiguration).toList());
	}

}