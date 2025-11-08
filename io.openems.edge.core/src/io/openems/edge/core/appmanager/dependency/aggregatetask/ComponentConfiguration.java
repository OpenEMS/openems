package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;
import java.util.stream.Stream;

import io.openems.common.types.EdgeConfig;

public record ComponentConfiguration(List<ComponentDef> components) {

	public ComponentConfiguration(EdgeConfig.Component... components) {
		this(Stream.of(components)//
				.map(t -> ComponentDef.from(t))//
				.toList());
	}

	public ComponentConfiguration(ComponentDef... components) {
		this(Stream.of(components).toList());
	}

}