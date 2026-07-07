package io.openems.edge.energy.api.simulation.periods;

import java.time.Duration;

public enum PeriodDuration {
	QUARTER(Duration.ofMinutes(15)), //
	HOUR(Duration.ofHours(1)), //
	;

	public final Duration duration;

	/**
	 * Converts power [W] to energy [Wh], considering the duration of the Period.
	 *
	 * @param power the power value
	 * @return the energy value
	 */
	public int convertPowerToEnergy(int power) {
		return switch (this) {
		case QUARTER -> power / 4;
		case HOUR -> power;
		};
	}

	/**
	 * Converts energy [Wh] to power [W], considering the duration of the Period.
	 *
	 * @param energy the energy value
	 * @return the power value
	 */
	public int convertEnergyToPower(int energy) {
		return switch (this) {
		case QUARTER -> energy * 4;
		case HOUR -> energy;
		};
	}

	PeriodDuration(Duration duration) {
		this.duration = duration;
	}
}
