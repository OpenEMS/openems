package io.openems.edge.evse.api;

public record Limit(SingleThreePhase phase, int minCurrent, int maxCurrent) {
}
