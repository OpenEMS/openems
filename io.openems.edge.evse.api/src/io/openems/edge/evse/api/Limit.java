package io.openems.edge.evse.api;

public record Limit(Phase phase, int minCurrent, int maxCurrent) {
}
