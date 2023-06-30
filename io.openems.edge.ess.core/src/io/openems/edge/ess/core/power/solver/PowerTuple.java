package io.openems.edge.ess.core.power.solver;

import io.openems.edge.ess.power.api.Pwr;

public class PowerTuple {

	private int activePower = 0;
	private int reactivePower = 0;

	/**
	 * Set a value.
	 *
	 * @param pwr   the {@link Pwr}
	 * @param value the value
	 */
	public void setValue(Pwr pwr, int value) {
		switch (pwr) {
		case ACTIVE:
			this.activePower = value;
			break;
		case REACTIVE:
			this.reactivePower = value;
			break;
		}
	}

	/**
	 * Get the Active-Power value.
	 *
	 * @return value for {@link Pwr#ACTIVE}.
	 */
	public int getActivePower() {
		return this.activePower;
	}

	/**
	 * Get the Reactive-Power value.
	 *
	 * @return value for {@link Pwr#REACTIVE}.
	 */
	public int getReactivePower() {
		return this.reactivePower;
	}

	@Override
	public String toString() {
		return "[P=" + this.activePower + ", Q=" + this.reactivePower + "]";
	}

}