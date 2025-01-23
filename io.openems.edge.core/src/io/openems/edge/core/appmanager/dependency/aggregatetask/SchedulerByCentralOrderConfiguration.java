package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;
import java.util.stream.Stream;

public record SchedulerByCentralOrderConfiguration(List<SchedulerComponent> componentOrder) {

	public static record SchedulerComponent(//
			String id, //
			String factoryId, //
			String createdByAppId // nullable
	) {

	}

	public SchedulerByCentralOrderConfiguration(SchedulerComponent... components) {
		this(Stream.of(components).toList());
	}

}
