package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;
import java.util.stream.Stream;

public record PredictorManagerByCentralOrderConfiguration(List<PredictorManagerComponent> components) {

	public record PredictorManagerComponent(//
			String componentId, //
			String factoryId //
	) {

	}

	public PredictorManagerByCentralOrderConfiguration(PredictorManagerComponent... components) {
		this(Stream.of(components).toList());
	}

}
