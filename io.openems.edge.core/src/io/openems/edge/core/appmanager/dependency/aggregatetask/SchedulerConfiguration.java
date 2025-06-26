package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;
import java.util.stream.Stream;

public record SchedulerConfiguration(List<String> componentOrder) {

	public SchedulerConfiguration(String... componentIds) {
		this(Stream.of(componentIds).toList());
	}

}
