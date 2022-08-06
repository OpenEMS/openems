// CHECKSTYLE:OFF

package io.openems.edge.ess.mr.gridcon.writewords;

public class DcDcParameter {

	public static final int DC_DC_ADRESS = 32720;

	// 32720
	public DcDcParameter() {

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
		return this.dcVoltageSetpoint;
	}

	public void setDcVoltageSetpoint(float dcVoltageSetpoint) {
		this.dcVoltageSetpoint = dcVoltageSetpoint;
	}

	public float getWeightStringA() {
		return this.weightStringA;
	}

	public void setWeightStringA(float weightStringA) {
		this.weightStringA = weightStringA;
	}

	public float getWeightStringB() {
		return this.weightStringB;
	}

	public void setWeightStringB(float weightStringB) {
		this.weightStringB = weightStringB;
	}

	public float getWeightStringC() {
		return this.weightStringC;
	}

	public void setWeightStringC(float weightStringC) {
		this.weightStringC = weightStringC;
	}

	public int getStringControlMode() {
		return this.stringControlMode;
	}

	public void setStringControlMode(int stringControlMode) {
		this.stringControlMode = stringControlMode;
	}

	public float getiRefStringA() {
		return this.iRefStringA;
	}

	public float getiRefStringB() {
		return this.iRefStringB;
	}

	public float getiRefStringC() {
		return this.iRefStringC;
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
		return "DcDcParameter [dcVoltageSetpoint=" + this.dcVoltageSetpoint + ", weightStringA=" + this.weightStringA
				+ ", weightStringB=" + this.weightStringB + ", weightStringC=" + this.weightStringC + ", iRefStringA="
				+ this.iRefStringA + ", iRefStringB=" + this.iRefStringB + ", iRefStringC=" + this.iRefStringC
				+ ", stringControlMode=" + this.stringControlMode + "]\n" + this.getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(DC_DC_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.format(this.dcVoltageSetpoint, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.weightStringA, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.weightStringB, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.weightStringC, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.iRefStringA, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.iRefStringB, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.iRefStringC, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.stringControlMode, true));

		return sb.toString();
	}
}
// CHECKSTYLE:ON
