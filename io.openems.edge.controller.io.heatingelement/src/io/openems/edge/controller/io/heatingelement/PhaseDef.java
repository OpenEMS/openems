package io.openems.edge.controller.io.heatingelement;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

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
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

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
		if (!this.stopwatch.isRunning()) {
			this.stopwatch.start();
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
		if (this.stopwatch.isRunning()) {
			this.stopwatch.stop();
		}

		this.parent.setOutput(this.phase, false);
	}

	/**
	 * Gets the total switch-on time in seconds since last reset on midnight.
	 * 
	 * @return the total time in [s]
	 */
	public int getTotalTime() {
		return (int) this.stopwatch.elapsed(TimeUnit.SECONDS);
	}

	/**
	 * Gets the total energy in watthours, calculated from total switch-on time
	 * since last reset on midnight multiplied with the switched power per phase.
	 * 
	 * @return the total energy in [Wh]
	 */
	public int getTotalEnergy() {
		return (int) Math.round(
				(this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / MILLISECONDS_PER_HOUR) * parent.getPowerPerPhase());
	}

	/**
	 * Resets the Stopwatch - to be called at midnight.
	 */
	public void resetStopwatch() {
		boolean wasRunning = this.stopwatch.isRunning();
		this.stopwatch.reset();
		if (wasRunning) {
			this.stopwatch.start();
		}
	}
}
