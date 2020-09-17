package io.openems.edge.ess.core.power.solver;

import io.openems.edge.ess.power.api.Pwr;

public class PowerTuple {

	private int activePower = 0;
	private int reactivePower = 0;

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

	public int getActivePower() {
		return this.activePower;
	}

	public int getReactivePower() {
		return this.reactivePower;
	}

	@Override
	public String toString() {
		return "[P=" + activePower + ", Q=" + reactivePower + "]";
	}

}