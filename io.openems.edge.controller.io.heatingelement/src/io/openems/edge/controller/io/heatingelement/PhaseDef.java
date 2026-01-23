package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.OptionalDouble;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.type.Phase.SinglePhase;

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
	private final SinglePhase phase;
	private final ArrayList<Integer> lastPowerValues = new ArrayList<Integer>();

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

	public PhaseDef(ControllerIoHeatingElementImpl parent, SinglePhase phase) {
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

	/**
	 * Calculates the average power of a phase for the last 20 times there will be
	 * a power on that phase.
	 *
	 * @param activePower the current active power of this phase
	 */
	public void calculateAvgPower(int activePower) {

		// Sets the avg power to 0, if there wasn't a calculation before and the
		// power is below 100 W.
		if (this.lastPowerValues.isEmpty() && activePower < 100) {
			this.setChannel(0);
			return;
		}

		if (activePower >= 100) {
			this.lastPowerValues.add(activePower);

			// Only the last 20 values are used
			if (this.lastPowerValues.size() > 20) {
				this.lastPowerValues.removeFirst();
			}

			OptionalDouble avgPower = this.lastPowerValues.stream().mapToInt(i -> i).average();

			if (avgPower.isPresent()) {
				this.setChannel((int) Math.round(avgPower.getAsDouble()));
			}
		}
	}

	/**
	 * Gets the average power of the phase from the Channel.
	 *
	 * @return avgPower
	 */
	public int getAvgPower() {
		return this.getAvgChannel().value().orElse(0);
	}

	private void setChannel(int power) {
		this.getAvgChannel().setNextValue(power);
	}

	private IntegerReadChannel getAvgChannel() {
		return switch (this.phase) {
		case SinglePhase.L1 -> this.parent.channel(ControllerIoHeatingElement.ChannelId.PHASE1_AVG_POWER);
		case SinglePhase.L2 -> this.parent.channel(ControllerIoHeatingElement.ChannelId.PHASE2_AVG_POWER);
		case SinglePhase.L3 -> this.parent.channel(ControllerIoHeatingElement.ChannelId.PHASE3_AVG_POWER);
		};
	}
}
