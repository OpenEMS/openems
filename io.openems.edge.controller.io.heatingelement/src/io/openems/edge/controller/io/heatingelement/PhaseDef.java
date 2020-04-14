package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * PhaseDef represents one Phase of the Heating Element.
 * 
 * <ul>
 * <li>Keeps info about whether the phase is switched ON or OFF
 * <li>Calculates totalTime and totalEnergy
 * </ul>
 */
public class PhaseDef {

	private static final double MILLISECONDS_PER_HOUR = 60 /* minutes */ * 60 /* seconds */ * 1000 /* milliseconds */;

	private final ControllerHeatingElement parent;
	private final Phase phase;

	/**
	 * keeps the total summed up Duration of the current day; it is updated on
	 * switchOff() and reset after midnight by getTotalDuration().
	 */
	private Duration duration = Duration.ZERO;

	/**
	 * Keeps the moment of the last switchOn().
	 */
	private LocalTime lastSwitchOn = null;

	public PhaseDef(ControllerHeatingElement parent, Phase phase) {
		this.parent = parent;
		this.phase = phase;
	}

	/**
	 * Switch the output ON.
	 * 
	 * @param outputChannelAddress address of the channel which must set to ON
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
	 * @param outputChannelAddress address of the channel which must set to OFF.
	 * 
	 * @throws OpenemsNamedException    on error.
	 * @throws IllegalArgumentException on error.
	 */
	protected void switchOff() throws IllegalArgumentException, OpenemsNamedException {
		if (this.lastSwitchOn != null) {
			this.duration = this.getTotalDuration();
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
		LocalTime now = LocalTime.now(this.parent.componentManager.getClock());

		// Did we pass midnight?
		if (this.lastSwitchOn != null && now.isBefore(this.lastSwitchOn)) {
			// Always reset Duration
			this.duration = Duration.ZERO;
			this.lastSwitchOn = LocalTime.MIN;
		}

		// Calculate and return the Duration
		if (this.lastSwitchOn != null) {
			return this.duration.plus(Duration.between(this.lastSwitchOn, now));
		} else {
			return this.duration;
		}
	}

	/**
	 * Gets the total energy in watthours, calculated from total switch-on time
	 * since last reset on midnight multiplied with the switched power per phase.
	 * 
	 * @return the total energy in [Wh]
	 */
	public int getTotalEnergy() {
		return (int) Math
				.round((this.getTotalDuration().toMillis() / MILLISECONDS_PER_HOUR) * parent.getPowerPerPhase());
	}
}
