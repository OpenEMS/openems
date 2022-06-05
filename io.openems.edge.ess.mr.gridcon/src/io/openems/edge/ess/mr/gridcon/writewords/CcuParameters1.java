// CHECKSTYLE:OFF

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
		return this.qLimit;
	}

	public void setqLimit(float qLimit) {
		this.qLimit = qLimit;
	}

	public float getuByQDroopMainLower() {
		return this.uByQDroopMainLower;
	}

	public float getuByQDroopMainUpper() {
		return this.uByQDroopMainUpper;
	}

	public float getuByQDroopT1Main() {
		return this.uByQDroopT1Main;
	}

	public float getfByPDroopMainLower() {
		return this.fByPDroopMainLower;
	}

	public float getfByPDroopMainUpper() {
		return this.fByPDroopMainUpper;
	}

	public float getfByPDroopT1Main() {
		return this.fByPDroopT1Main;
	}

	public float getqByUDroopMainLower() {
		return this.qByUDroopMainLower;
	}

	public float getqByUDroopMainUpper() {
		return this.qByUDroopMainUpper;
	}

	public float getqByUDeadBandLower() {
		return this.qByUDeadBandLower;
	}

	public float getqByUDeadBandUpper() {
		return this.qByUDeadBandUpper;
	}

	@Override
	public String toString() {
		return "CcuParameters1 [uByQDroopMainLower=" + this.uByQDroopMainLower + ", uByQDroopMainUpper="
				+ this.uByQDroopMainUpper + ", uByQDroopT1Main=" + this.uByQDroopT1Main + ", fByPDroopMainLower="
				+ this.fByPDroopMainLower + ", fByPDroopMainUpper=" + this.fByPDroopMainUpper + ", fByPDroopT1Main="
				+ this.fByPDroopT1Main + ", qByUDroopMainLower=" + this.qByUDroopMainLower + ", qByUDroopMainUpper="
				+ this.qByUDroopMainUpper + ", qByUDeadBandLower=" + this.qByUDeadBandLower + ", qByUDeadBandUpper="
				+ this.qByUDeadBandUpper + ", qLimit=" + this.qLimit + "]" + "\n" + this.getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(CCU_PARAMETERS_1_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.format(this.uByQDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.uByQDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.uByQDroopT1Main, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.fByPDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.fByPDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.fByPDroopT1Main, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.qByUDroopMainLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.qByUDroopMainUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.qByUDeadBandLower, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.qByUDeadBandUpper, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.qLimit, true));

		return sb.toString();
	}
}
// CHECKSTYLE:ON