// CHECKSTYLE:OFF

package io.openems.edge.ess.mr.gridcon.writewords;

import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class CcuParameters2 {

	public static final int CCU_PARAMETERS_2_ADRESS = 32784;

	public CcuParameters2() {
	}

	private float pByFDroopMainLower = 0f;
	private float pByFDroopMainUpper = 0f;
	private float pByFDeadBandLower = 0f;
	private float pByFDeadBandUpper = 0f;
	private float pByUDroopLower = 0f;
	private float pByUDroopUpper = 0f;
	private float pByUDeadBandLower = 0f;
	private float pByUDeadBandUpper = 0f;
	private float pByUMaxCharge = 0f;
	private float pByUMaxDischarge = 0f;
	private PControlMode pControlMode = PControlMode.ACTIVE_POWER_CONTROL;
	private float pControlLimTwo = 0f;
	private float pControlLimOne = 0f;

	public PControlMode getpControlMode() {
		return this.pControlMode;
	}

	public void setpControlMode(PControlMode pControlMode) {
		this.pControlMode = pControlMode;
	}

	public float getpByFDroopMainLower() {
		return this.pByFDroopMainLower;
	}

	public float getpByFDroopMainUpper() {
		return this.pByFDroopMainUpper;
	}

	public float getpByFDeadBandLower() {
		return this.pByFDeadBandLower;
	}

	public float getpByFDeadBandUpper() {
		return this.pByFDeadBandUpper;
	}

	public float getpByUDroopLower() {
		return this.pByUDroopLower;
	}

	public float getpByUDroopUpper() {
		return this.pByUDroopUpper;
	}

	public float getpByUDeadBandLower() {
		return this.pByUDeadBandLower;
	}

	public float getpByUDeadBandUpper() {
		return this.pByUDeadBandUpper;
	}

	public float getpByUMaxCharge() {
		return this.pByUMaxCharge;
	}

	public float getpByUMaxDischarge() {
		return this.pByUMaxDischarge;
	}

	public float getpControlLimTwo() {
		return this.pControlLimTwo;
	}

	public float getpControlLimOne() {
		return this.pControlLimOne;
	}

	@Override
	public String toString() {
		return "CcuParameters2 [pByFDroopMainLower=" + this.pByFDroopMainLower + ", pByFDroopMainUpper="
				+ this.pByFDroopMainUpper + ", pByFDeadBandLower=" + this.pByFDeadBandLower + ", pByFDeadBandUpper="
				+ this.pByFDeadBandUpper + ", pByUDroopLower=" + this.pByUDroopLower + ", pByUDroopUpper="
				+ this.pByUDroopUpper + ", pByUDeadBandLower=" + this.pByUDeadBandLower + ", pByUDeadBandUpper="
				+ this.pByUDeadBandUpper + ", pByUMaxCharge=" + this.pByUMaxCharge + ", pByUMaxDischarge="
				+ this.pByUMaxDischarge + ", pControlMode=" + this.pControlMode + ", pControlLimTwo="
				+ this.pControlLimTwo + ", pControlLimOne=" + this.pControlLimOne + "]" + "\n"
				+ this.getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(CCU_PARAMETERS_2_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.format(this.pByFDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByFDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByFDeadBandLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByFDeadBandUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByUDroopLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByUDroopUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByUDeadBandLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByUDeadBandUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByUMaxCharge, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pByUMaxDischarge, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pControlMode.getValue(), true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pControlLimTwo, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pControlLimOne, true));
		sb.append("  ");
		return sb.toString();
	}
}
// CHECKSTYLE:ON