package io.openems.edge.energy.optimizer;

public record OptimizationResult(//
		SimulationResult simulationResult, //
		Type type) {

	public enum Type {
		INTERMEDIATE, //
		FINAL
	}
}
