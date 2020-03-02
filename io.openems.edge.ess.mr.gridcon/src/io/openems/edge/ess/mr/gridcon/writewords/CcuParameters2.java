package io.openems.edge.ess.mr.gridcon.writewords;

import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class CcuParameters2 {
	
	public static final int CCU_PARAMETERS_2_ADRESS = 32784;
	
		private CcuParameters2() {

		}
		
		private static CcuParameters2 instance;
		
		public static CcuParameters2 getCcuParameters2()  {
			if (instance == null) {
				instance = new CcuParameters2();
			}
			return instance;
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
		return pControlMode;
	}
 	public void setpControlMode(PControlMode pControlMode) {
		this.pControlMode = pControlMode;
	}

	public static CcuParameters2 getInstance() {
		return instance;
	}
	public float getpByFDroopMainLower() {
		return pByFDroopMainLower;
	}

	public float getpByFDroopMainUpper() {
		return pByFDroopMainUpper;
	}

	public float getpByFDeadBandLower() {
		return pByFDeadBandLower;
	}

	public float getpByFDeadBandUpper() {
		return pByFDeadBandUpper;
	}

	public float getpByUDroopLower() {
		return pByUDroopLower;
	}

	public float getpByUDroopUpper() {
		return pByUDroopUpper;
	}

	public float getpByUDeadBandLower() {
		return pByUDeadBandLower;
	}

	public float getpByUDeadBandUpper() {
		return pByUDeadBandUpper;
	}

	public float getpByUMaxCharge() {
		return pByUMaxCharge;
	}

	public float getpByUMaxDischarge() {
		return pByUMaxDischarge;
	}

	public float getpControlLimTwo() {
		return pControlLimTwo;
	}

	public float getpControlLimOne() {
		return pControlLimOne;
	}
		
	@Override
	public String toString() {
		return "CcuParameters2 [pByFDroopMainLower=" + pByFDroopMainLower + ", pByFDroopMainUpper=" + pByFDroopMainUpper
				+ ", pByFDeadBandLower=" + pByFDeadBandLower + ", pByFDeadBandUpper=" + pByFDeadBandUpper
				+ ", pByUDroopLower=" + pByUDroopLower + ", pByUDroopUpper=" + pByUDroopUpper + ", pByUDeadBandLower="
				+ pByUDeadBandLower + ", pByUDeadBandUpper=" + pByUDeadBandUpper + ", pByUMaxCharge=" + pByUMaxCharge
				+ ", pByUMaxDischarge=" + pByUMaxDischarge + ", pControlMode=" + pControlMode + ", pControlLimTwo="
				+ pControlLimTwo + ", pControlLimOne=" + pControlLimOne + "]" + "\n" + getHexRepresentation();
	}
	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(CCU_PARAMETERS_2_ADRESS);
		sb.append(": ");		
		sb.append(HexFormatter.format(pByFDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByFDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByFDeadBandLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByFDeadBandUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByUDroopLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByUDroopUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByUDeadBandLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByUDeadBandUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByUMaxCharge, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pByUMaxDischarge, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pControlMode.getValue(), true));
		sb.append("  ");
		sb.append(HexFormatter.format(pControlLimTwo, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pControlLimOne, true));
		sb.append("  ");
		return sb.toString();
	}
}
