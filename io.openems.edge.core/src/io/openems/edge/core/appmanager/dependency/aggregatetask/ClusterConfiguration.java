package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;

public record ClusterConfiguration(List<String> evseIds) {

	public ClusterConfiguration(String... evseIds) {
		this(List.of(evseIds));
	}
}
