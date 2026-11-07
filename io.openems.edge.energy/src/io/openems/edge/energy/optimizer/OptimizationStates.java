package io.openems.edge.energy.optimizer;

import java.time.Instant;

/**
 * Provides state transitions and state-related helper methods for
 * {@link OptimizationState}.
 */
final class OptimizationStates {

	private OptimizationStates() {
	}

	/**
	 * Creates the initial state for a new optimization run.
	 *
	 * @param now                   the time when the state becomes active
	 * @param reason                the reason why the optimization was started
	 * @param optimizeCurrentPeriod whether the current period may be reoptimized
	 * @return the initial optimization state
	 */
	static OptimizationState start(//
			Instant now, //
			String reason, //
			boolean optimizeCurrentPeriod) {

		if (optimizeCurrentPeriod) {
			return new OptimizationState.ReoptimizingCurrent(now, reason);
		}

		return new OptimizationState.OptimizingFuture(now, reason);
	}

	/**
	 * Fixes the current schedule period and continues optimizing future periods.
	 *
	 * <p>
	 * This transition is only valid while the current period is being reoptimized.
	 *
	 * @param current the current optimization state
	 * @param now     the time when the new state becomes active
	 * @return the new optimization state
	 * @throws IllegalStateException if the current period is already fixed
	 */
	static OptimizationState commitCurrentPeriod(//
			OptimizationState current, //
			Instant now) {

		if (!(current instanceof OptimizationState.ReoptimizingCurrent state)) {
			throw new IllegalStateException("Cannot commit current period in state [" + current + "]");
		}

		return new OptimizationState.OptimizingFuture(now, state.reason());
	}

	/**
	 * Marks the current optimization run as finished.
	 *
	 * @param current the current optimization state
	 * @param now     the time when the finished state becomes active
	 * @return the finished optimization state
	 */
	static OptimizationState finish(//
			OptimizationState current, //
			Instant now) {

		return new OptimizationState.Optimized(now, current.reason());
	}

	/**
	 * Checks whether the current schedule period is fixed and must no longer be
	 * changed by the optimizer. A null state, i.e. no active optimization, is
	 * treated as not fixed.
	 *
	 * @param state the current optimization state; or null
	 * @return {@code true} if the current period is fixed
	 */
	static boolean isCurrentPeriodFixed(OptimizationState state) {
		return switch (state) {
		case null -> false; // no optimization is active -> nothing is fixed
		case OptimizationState.ReoptimizingCurrent ignored -> false;
		case OptimizationState.OptimizingFuture ignored -> true;
		case OptimizationState.Optimized ignored -> true;
		};
	}
}
