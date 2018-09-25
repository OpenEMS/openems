package io.openems.edge.ess.core.power;

import io.openems.edge.ess.power.api.Pwr;

class PowerTuple {
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
		return activePower;
	}

	public int getReactivePower() {
		return reactivePower;
	}

	@Override
	public String toString() {
		return "[P=" + activePower + ", Q=" + reactivePower + "]";
	}

}