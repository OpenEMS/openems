package io.openems.edge.controller.evse.cluster.powerdistribute;

public sealed interface PowerDistributionKey {
	record Evse(String ctrlSingleId) implements PowerDistributionKey {
	}
}
