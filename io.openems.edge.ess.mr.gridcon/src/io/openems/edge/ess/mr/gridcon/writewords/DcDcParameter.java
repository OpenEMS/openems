package io.openems.edge.ess.mr.gridcon.writewords;

public class DcDcParameter {

	public static int DC_DC_ADRESS = 32720;

	// 32720
	private DcDcParameter() {

	}

	private static DcDcParameter instance;

	public static DcDcParameter getDcdcParameter() {
		if (instance == null) {
			instance = new DcDcParameter();
		}
		return instance;
	}

	private float dcVoltageSetpoint = 0f;
	private float weightStringA = 0f;
	private float weightStringB = 0f;
	private float weightStringC = 0f;
	private float iRefStringA = 0f;
	private float iRefStringB = 0f;
	private float iRefStringC = 0f;
	private int stringControlMode = 0;

	public float getDcVoltageSetpoint() {
		return dcVoltageSetpoint;
	}

	public void setDcVoltageSetpoint(float dcVoltageSetpoint) {
		this.dcVoltageSetpoint = dcVoltageSetpoint;
	}

	public float getWeightStringA() {
		return weightStringA;
	}

	public void setWeightStringA(float weightStringA) {
		this.weightStringA = weightStringA;
	}

	public float getWeightStringB() {
		return weightStringB;
	}

	public void setWeightStringB(float weightStringB) {
		this.weightStringB = weightStringB;
	}

	public float getWeightStringC() {
		return weightStringC;
	}

	public void setWeightStringC(float weightStringC) {
		this.weightStringC = weightStringC;
	}

	public int getStringControlMode() {
		return stringControlMode;
	}

	public void setStringControlMode(int stringControlMode) {
		this.stringControlMode = stringControlMode;
	}

	public float getiRefStringA() {
		return iRefStringA;
	}

	public float getiRefStringB() {
		return iRefStringB;
	}

	public float getiRefStringC() {
		return iRefStringC;
	}

	public void setiRefStringA(float iRefStringA) {
		this.iRefStringA = iRefStringA;
	}

	public void setiRefStringB(float iRefStringB) {
		this.iRefStringB = iRefStringB;
	}

	public void setiRefStringC(float iRefStringC) {
		this.iRefStringC = iRefStringC;
	}

	@Override
	public String toString() {
		return "DcDcParameter [dcVoltageSetpoint=" + dcVoltageSetpoint + ", weightStringA=" + weightStringA
				+ ", weightStringB=" + weightStringB + ", weightStringC=" + weightStringC + ", iRefStringA="
				+ iRefStringA + ", iRefStringB=" + iRefStringB + ", iRefStringC=" + iRefStringC + ", stringControlMode="
				+ stringControlMode + "]\n" + getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(DC_DC_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.format(dcVoltageSetpoint, true));
		sb.append("  ");
		sb.append(HexFormatter.format(weightStringA, true));
		sb.append("  ");
		sb.append(HexFormatter.format(weightStringB, true));
		sb.append("  ");
		sb.append(HexFormatter.format(weightStringC, true));
		sb.append("  ");
		sb.append(HexFormatter.format(iRefStringA, true));
		sb.append("  ");
		sb.append(HexFormatter.format(iRefStringB, true));
		sb.append("  ");
		sb.append(HexFormatter.format(iRefStringC, true));
		sb.append("  ");
		sb.append(HexFormatter.format(stringControlMode, true));

		return sb.toString();
	}
}
