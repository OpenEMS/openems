package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.io.heatingelement.enums.Phase;

/**
 * PhaseDef represents one Phase of the Heating Element.
 *
 * <ul>
 * <li>Keeps info about whether the phase is switched ON or OFF
 * <li>Calculates totalTime and totalEnergy
 * </ul>
 */
public class PhaseDef {

	private final ControllerIoHeatingElementImpl parent;
	private final Phase phase;

	/**
	 * keeps the total summed up Duration of the current day; it is updated on
	 * switchOff() and reset after midnight by getTotalDuration().
	 */
	private Duration dailyDuration = Duration.ZERO;

	/**
	 * Keeps the current day to detect changes in day.
	 */
	private LocalDate currentDay = LocalDate.MIN;

	/**
	 * Keeps the moment of the last switchOn().
	 */
	private LocalTime lastSwitchOn = null;

	public PhaseDef(ControllerIoHeatingElementImpl parent, Phase phase) {
		this.parent = parent;
		this.phase = phase;
	}

	/**
	 * Switch the output ON.
	 *
	 * @throws OpenemsNamedException    on error.
	 * @throws IllegalArgumentException on error.
	 */
	protected void switchOn() throws IllegalArgumentException, OpenemsNamedException {
		if (this.lastSwitchOn == null) {
			this.lastSwitchOn = LocalTime.now(this.parent.componentManager.getClock());
		}

		this.parent.setOutput(this.phase, true);
	}

	/**
	 * Switch the output OFF.
	 *
	 * @throws OpenemsNamedException    on error.
	 * @throws IllegalArgumentException on error.
	 */
	protected void switchOff() throws IllegalArgumentException, OpenemsNamedException {
		if (this.lastSwitchOn != null) {
			this.dailyDuration = this.getTotalDuration();
			this.lastSwitchOn = null;
		}

		this.parent.setOutput(this.phase, false);
	}

	/**
	 * Gets the total switch-on time in seconds since last reset on midnight.
	 *
	 * @return the total elapsed time
	 */
	public Duration getTotalDuration() {

		// Did we pass midnight?
		var today = LocalDate.now(this.parent.componentManager.getClock());
		if (!this.currentDay.equals(today)) {
			// Always reset Duration
			this.currentDay = today;
			this.dailyDuration = Duration.ZERO;
			if (this.lastSwitchOn != null) {
				this.lastSwitchOn = LocalTime.MIN;
			}
		}

		// Calculate and return the Duration
		if (this.lastSwitchOn != null) {
			var now = LocalTime.now(this.parent.componentManager.getClock());
			return this.dailyDuration.plus(Duration.between(this.lastSwitchOn, now));
		}
		return this.dailyDuration;
	}
}
