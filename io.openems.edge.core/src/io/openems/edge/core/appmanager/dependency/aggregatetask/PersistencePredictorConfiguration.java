package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.Stream;

public record PersistencePredictorConfiguration(Set<String> channels) {

	public PersistencePredictorConfiguration(String... channels) {
		this(Stream.of(channels).collect(toSet()));
	}

}
