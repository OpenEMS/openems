package io.openems.edge.controller.io.heatingelement;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tracks the daily (midnight-reset) total duration a Step has been switched on.
 *
 * <p>
 * This is the Step-based counterpart of {@link PhaseDef#getTotalDuration()},
 * used to derive the LEVEL1_TIME..LEVEL6_TIME Channels and TIME guarantees for
 * {@link io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements#TWO},
 * where switching is not routed through {@link PhaseDef}.
 */
public class StepActiveDuration {

	/**
	 * Keeps the total summed up Duration of the current day; it is updated on
	 * {@link #update(boolean, Clock)} and reset after midnight.
	 */
	private Duration dailyDuration = Duration.ZERO;

	/**
	 * Keeps the current day to detect changes in day.
	 */
	private LocalDate currentDay = LocalDate.MIN;

	/**
	 * Keeps the moment the Step was last switched on.
	 */
	private LocalTime lastOnAt = null;

	/**
	 * Updates the tracked on/off state. Call this once per cycle.
	 *
	 * @param isOn  true if the Step is currently switched on
	 * @param clock the {@link Clock}
	 */
	public void update(boolean isOn, Clock clock) {
		this.resetDailyDurationIfNeeded(clock);

		if (isOn) {
			if (this.lastOnAt == null) {
				this.lastOnAt = LocalTime.now(clock);
			}
		} else if (this.lastOnAt != null) {
			this.dailyDuration = this.getTotalDuration(clock);
			this.lastOnAt = null;
		}
	}

	/**
	 * Gets the total switch-on duration for the current day, resetting the tracked
	 * duration after midnight if needed.
	 *
	 * @param clock the {@link Clock}
	 * @return the total elapsed Duration
	 */
	public Duration getTotalDuration(Clock clock) {
		this.resetDailyDurationIfNeeded(clock);
		if (this.lastOnAt != null) {
			return this.dailyDuration.plus(Duration.between(this.lastOnAt, LocalTime.now(clock)));
		}
		return this.dailyDuration;
	}

	private void resetDailyDurationIfNeeded(Clock clock) {
		var today = LocalDate.now(clock);
		if (!this.currentDay.equals(today)) {
			this.currentDay = today;
			this.dailyDuration = Duration.ZERO;
			if (this.lastOnAt != null) {
				this.lastOnAt = LocalTime.MIN;
			}
		}
	}
}
