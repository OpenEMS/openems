package io.openems.edge.ess.mr.gridcon.writewords;

public class CcuParameters1 {

	public static final int CCU_PARAMETERS_1_ADRESS = 32592;

	public CcuParameters1() {
	}

	private float uByQDroopMainLower = 0f;
	private float uByQDroopMainUpper = 0f;
	private float uByQDroopT1Main = 0f;
	private float fByPDroopMainLower = 0f;
	private float fByPDroopMainUpper = 0f;
	private float fByPDroopT1Main = 0f;
	private float qByUDroopMainLower = 0f;
	private float qByUDroopMainUpper = 0f;
	private float qByUDeadBandLower = 0f;
	private float qByUDeadBandUpper = 0f;
	private float qLimit = 0f;

	public float getqLimit() {
		return qLimit;
	}

	public void setqLimit(float qLimit) {
		this.qLimit = qLimit;
	}

	public float getuByQDroopMainLower() {
		return uByQDroopMainLower;
	}

	public float getuByQDroopMainUpper() {
		return uByQDroopMainUpper;
	}

	public float getuByQDroopT1Main() {
		return uByQDroopT1Main;
	}

	public float getfByPDroopMainLower() {
		return fByPDroopMainLower;
	}

	public float getfByPDroopMainUpper() {
		return fByPDroopMainUpper;
	}

	public float getfByPDroopT1Main() {
		return fByPDroopT1Main;
	}

	public float getqByUDroopMainLower() {
		return qByUDroopMainLower;
	}

	public float getqByUDroopMainUpper() {
		return qByUDroopMainUpper;
	}

	public float getqByUDeadBandLower() {
		return qByUDeadBandLower;
	}

	public float getqByUDeadBandUpper() {
		return qByUDeadBandUpper;
	}

	@Override
	public String toString() {
		return "CcuParameters1 [uByQDroopMainLower=" + uByQDroopMainLower + ", uByQDroopMainUpper=" + uByQDroopMainUpper
				+ ", uByQDroopT1Main=" + uByQDroopT1Main + ", fByPDroopMainLower=" + fByPDroopMainLower
				+ ", fByPDroopMainUpper=" + fByPDroopMainUpper + ", fByPDroopT1Main=" + fByPDroopT1Main
				+ ", qByUDroopMainLower=" + qByUDroopMainLower + ", qByUDroopMainUpper=" + qByUDroopMainUpper
				+ ", qByUDeadBandLower=" + qByUDeadBandLower + ", qByUDeadBandUpper=" + qByUDeadBandUpper + ", qLimit="
				+ qLimit + "]" + "\n" + getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(CCU_PARAMETERS_1_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.format(uByQDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(uByQDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(uByQDroopT1Main, true));
		sb.append("  ");
		sb.append(HexFormatter.format(fByPDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(fByPDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(fByPDroopT1Main, true));
		sb.append("  ");
		sb.append(HexFormatter.format(qByUDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(qByUDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(qByUDeadBandLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(qByUDeadBandUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(qLimit, true));

		return sb.toString();
	}
}
