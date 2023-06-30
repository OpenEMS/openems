// CHECKSTYLE:OFF

package io.openems.edge.ess.mr.gridcon.writewords;

public class IpuParameter {

	// 32624, 32656, 32688
	public IpuParameter() {
	}

	private float dcVoltageSetpoint = 0f;
	private float dcCurrentSetpoint = 0f;
	private float u0OffsetToCcu = 0f;
	private float f0OffsetToCcu = 0f;
	private float qRefOffsetToCcu = 0f;
	private float pRefOffsetToCcu = 0f;
	private float pMaxDischarge = 0f;
	private float pMaxCharge = 0f;

	public float getpMaxDischarge() {
		return this.pMaxDischarge;
	}

	public void setpMaxDischarge(float pMaxDischarge) {
		this.pMaxDischarge = pMaxDischarge;
	}

	public float getpMaxCharge() {
		return this.pMaxCharge;
	}

	public void setpMaxCharge(float pMaxCharge) {
		this.pMaxCharge = pMaxCharge;
	}

	public float getDcVoltageSetpoint() {
		return this.dcVoltageSetpoint;
	}

	public float getDcCurrentSetpoint() {
		return this.dcCurrentSetpoint;
	}

	public float getU0OffsetToCcu() {
		return this.u0OffsetToCcu;
	}

	public float getF0OffsetToCcu() {
		return this.f0OffsetToCcu;
	}

	public float getqRefOffsetToCcu() {
		return this.qRefOffsetToCcu;
	}

	public float getpRefOffsetToCcu() {
		return this.pRefOffsetToCcu;
	}

	@Override
	public String toString() {
		return "IpuParameter [dcVoltageSetpoint=" + this.dcVoltageSetpoint + ", dcCurrentSetpoint="
				+ this.dcCurrentSetpoint + ", u0OffsetToCcu=" + this.u0OffsetToCcu + ", f0OffsetToCcu="
				+ this.f0OffsetToCcu + ", qRefOffsetToCcu=" + this.qRefOffsetToCcu + ", pRefOffsetToCcu="
				+ this.pRefOffsetToCcu + ", pMaxDischarge=" + this.pMaxDischarge + ", pMaxCharge=" + this.pMaxCharge
				+ "]\n" + this.getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(HexFormatter.format(this.dcVoltageSetpoint, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.dcCurrentSetpoint, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.u0OffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.f0OffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.qRefOffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pRefOffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pMaxDischarge, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.pMaxCharge, true));

		return sb.toString();
	}
}
// CHECKSTYLE:ON