package io.openems.edge.ess.mr.gridcon;

import io.openems.edge.ess.mr.gridcon.enums.Mode;

public class GridconSettings {

	boolean switchOffIpus;
	float u0;
	float f0;
	Mode mode;

	private GridconSettings(boolean switchOffIpus, float u0, float f0, Mode mode) {
		this.switchOffIpus = switchOffIpus;
		this.u0 = u0;
		this.f0 = f0;
		this.mode = mode;
	}

	public static GridconSettings createRunningSettings(float u0, float f0, Mode mode) {
		return new GridconSettings(false, u0, f0, mode);
	}

	public static GridconSettings createStopSettings(Mode mode) {
		return new GridconSettings(true, 0, 0, mode);
	}

	public boolean isSwitchOffIpus() {
		return switchOffIpus;
	}

	public float getU0() {
		return u0;
	}

	public float getF0() {
		return f0;
	}

	public Mode getMode() {
		return mode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(f0);
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + (switchOffIpus ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(u0);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GridconSettings other = (GridconSettings) obj;
		if (Float.floatToIntBits(f0) != Float.floatToIntBits(other.f0))
			return false;
		if (mode != other.mode)
			return false;
		if (switchOffIpus != other.switchOffIpus)
			return false;
		if (Float.floatToIntBits(u0) != Float.floatToIntBits(other.u0))
			return false;
		return true;
	}

}
