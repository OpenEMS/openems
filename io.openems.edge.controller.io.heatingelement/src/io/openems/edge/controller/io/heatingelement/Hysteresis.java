package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalDateTime;

import io.openems.edge.controller.io.heatingelement.enums.Level;

/**
 * Applies symmetric SURPLUS switching hysteresis between the current and target
 * {@link Level}.
 *
 * <p>
 * In SURPLUS mode, each change is made only after the configured minimum
 * switching time has elapsed since the previous change. Each change moves the
 * Level by one step toward the latest target and updates the switching
 * timestamp, regardless of direction. The target is reevaluated on every call,
 * so a change in direction during the waiting period is respected when the next
 * change is allowed. Callers can retain the legacy immediate downward behavior
 * outside SURPLUS mode; upward changes remain stepwise.
 */
public class Hysteresis {

	/**
	 * Result of {@link Hysteresis#apply}.
	 *
	 * @param level              the Level that should actually be applied
	 * @param awaitingHysteresis true if a target Level change is currently being
	 *                           delayed by the hysteresis
	 */
	public record Result(Level level, boolean awaitingHysteresis) {
	}

	private Level currentLevel = Level.LEVEL_0;
	private LocalDateTime lastLevelChange = LocalDateTime.MIN;

	/**
	 * Gets the Level that was last applied, be it via {@link #apply} or
	 * {@link #setCurrentLevel}.
	 *
	 * @return the current Level
	 */
	public Level getCurrentLevel() {
		return this.currentLevel;
	}

	/**
	 * Updates the Level that was actually applied, without going through the
	 * hysteresis. Must be called whenever a Level is applied that bypasses
	 * {@link #apply}, e.g. in Manual mode, during Calibration or while forced
	 * heating is active, so that the next {@link #apply} call compares the target
	 * Level against the actually applied Level.
	 *
	 * @param currentLevel the Level that was actually applied
	 */
	public void setCurrentLevel(Level currentLevel) {
		this.currentLevel = currentLevel;
	}

	/**
	 * Applies the hysteresis to the given target {@link Level}.
	 *
	 * @param targetLevel          the target Level
	 * @param now                  the current time
	 * @param minimumSwitchingTime the configured minimum switching time
	 * @param maxLevel             the highest possible Level, i.e. the number of
	 *                             configured Steps
	 * @param limitDownwardChanges true to limit downward changes as well; false to
	 *                             apply downward changes immediately
	 * @return the {@link Result}
	 */
	public Result apply(Level targetLevel, LocalDateTime now, Duration minimumSwitchingTime, int maxLevel,
			boolean limitDownwardChanges) {
		if (!limitDownwardChanges && targetLevel.getValue() < this.currentLevel.getValue()) {
			this.currentLevel = targetLevel;
			return new Result(targetLevel, false);
		}

		if (targetLevel.getValue() == this.currentLevel.getValue()) {
			return new Result(this.currentLevel, false);
		}

		if (this.lastLevelChange.plus(minimumSwitchingTime).isAfter(now)) {
			return new Result(this.currentLevel, true);
		}

		this.lastLevelChange = now;
		if (targetLevel.getValue() > this.currentLevel.getValue()) {
			this.currentLevel = getNextLevel(this.currentLevel, maxLevel);
		} else {
			this.currentLevel = Level.fromValue(this.currentLevel.getValue() - 1);
		}
		return new Result(this.currentLevel, false);
	}

	/**
	 * Returns the next higher {@link Level}, capped at {@code maxLevel}.
	 *
	 * @param level    the current Level
	 * @param maxLevel the highest possible Level, i.e. the number of configured
	 *                 Steps
	 * @return the next higher Level, or the same Level if already at the maximum
	 */
	private static Level getNextLevel(Level level, int maxLevel) {
		var nextValue = Math.min(level.getValue() + 1, maxLevel);
		return Level.fromValue(nextValue);
	}
}
