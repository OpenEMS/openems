package io.openems.edge.energy.optimizer;

import java.time.Instant;

/**
 * Represents the current state of an optimization run.
 */
public sealed interface OptimizationState {

	/**
	 * Gets the time when this state became active.
	 *
	 * @return the state start time
	 */
	Instant startedAt();

	/**
	 * Gets the reason why the optimization was started.
	 *
	 * @return the optimization reason
	 */
	String reason();

	/**
	 * The current schedule period is being reoptimized and may still change.
	 */
	record ReoptimizingCurrent(//
			Instant startedAt, //
			String reason) implements OptimizationState {
	}

	/**
	 * The current schedule period is fixed and only future periods are being
	 * optimized.
	 */
	record OptimizingFuture(//
			Instant startedAt, //
			String reason) implements OptimizationState {
	}

	/**
	 * The optimization run has finished.
	 */
	record Optimized(//
			Instant startedAt, //
			String reason) implements OptimizationState {
	}
}
