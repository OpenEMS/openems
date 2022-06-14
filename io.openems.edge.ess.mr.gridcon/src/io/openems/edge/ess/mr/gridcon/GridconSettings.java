// CHECKSTYLE:OFF
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
		return this.switchOffIpus;
	}

	public float getU0() {
		return this.u0;
	}

	public float getF0() {
		return this.f0;
	}

	public Mode getMode() {
		return this.mode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(this.f0);
		result = prime * result + ((this.mode == null) ? 0 : this.mode.hashCode());
		result = prime * result + (this.switchOffIpus ? 1231 : 1237);
		result = prime * result + Float.floatToIntBits(this.u0);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() != obj.getClass()) {
			return false;
		}
		GridconSettings other = (GridconSettings) obj;
		if (Float.floatToIntBits(this.f0) != Float.floatToIntBits(other.f0)) {
			return false;
		} else if (this.mode != other.mode) {
			return false;
		} else if (this.switchOffIpus != other.switchOffIpus) {
			return false;
		} else if (Float.floatToIntBits(this.u0) != Float.floatToIntBits(other.u0)) {
			return false;
		} else {
			return true;
		}
	}

}
// CHECKSTYLE:ON
