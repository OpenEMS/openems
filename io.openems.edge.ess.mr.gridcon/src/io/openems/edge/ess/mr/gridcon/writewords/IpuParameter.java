package io.openems.edge.ess.mr.gridcon.writewords;

public class IpuParameter {

	private IpuParameter() {
		
	}
	
	private static IpuParameter instanceIpu1Parameter;
	private static IpuParameter instanceIpu2Parameter;
	private static IpuParameter instanceIpu3Parameter;
	
	public static IpuParameter getIpu1Parameter() {
		if (instanceIpu1Parameter == null) {
			instanceIpu1Parameter = new IpuParameter();
		}
		return instanceIpu1Parameter;
	}
	
	public static IpuParameter getIpu2Parameter() {
		if (instanceIpu2Parameter == null) {
			instanceIpu2Parameter = new IpuParameter();
		}
		return instanceIpu2Parameter;
	}
	
	public static IpuParameter getIpu3Parameter() {
		if (instanceIpu3Parameter == null) {
			instanceIpu3Parameter = new IpuParameter();
		}
		return instanceIpu3Parameter;
	}
	
	// 32624, 32656, 32688
	private float dcVoltageSetpoint = 0f;
	private float dcCurrentSetpoint = 0f;
	private float u0OffsetToCcu = 0f;
	private float f0OffsetToCcu = 0f;
	private float qRefOffsetToCcu = 0f;
	private float pRefOffsetToCcu = 0f;
	private float pMaxDischarge = 0f;
	private float pMaxCharge = 0f;
	
	public float getpMaxDischarge() {
		return pMaxDischarge;
	}

	public void setpMaxDischarge(float pMaxDischarge) {
		this.pMaxDischarge = pMaxDischarge;
	}

	public float getpMaxCharge() {
		return pMaxCharge;
	}

	public void setpMaxCharge(float pMaxCharge) {
		this.pMaxCharge = pMaxCharge;
	}

	public float getDcVoltageSetpoint() {
		return dcVoltageSetpoint;
	}

	public float getDcCurrentSetpoint() {
		return dcCurrentSetpoint;
	}

	public float getU0OffsetToCcu() {
		return u0OffsetToCcu;
	}

	public float getF0OffsetToCcu() {
		return f0OffsetToCcu;
	}

	public float getqRefOffsetToCcu() {
		return qRefOffsetToCcu;
	}

	public float getpRefOffsetToCcu() {
		return pRefOffsetToCcu;
	}

	@Override
	public String toString() {
		return "IpuParameter [dcVoltageSetpoint=" + dcVoltageSetpoint + ", dcCurrentSetpoint="
				+ dcCurrentSetpoint + ", u0OffsetToCcu=" + u0OffsetToCcu + ", f0OffsetToCcu=" + f0OffsetToCcu
				+ ", qRefOffsetToCcu=" + qRefOffsetToCcu + ", pRefOffsetToCcu=" + pRefOffsetToCcu + ", pMaxDischarge="
				+ pMaxDischarge + ", pMaxCharge=" + pMaxCharge + "]\n" + getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(HexFormatter.format(dcVoltageSetpoint, true));
		sb.append("  ");
		sb.append(HexFormatter.format(dcCurrentSetpoint, true));
		sb.append("  ");
		sb.append(HexFormatter.format(u0OffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(f0OffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(qRefOffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pRefOffsetToCcu, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pMaxDischarge, true));
		sb.append("  ");
		sb.append(HexFormatter.format(pMaxCharge, true));
		
		return sb.toString();
	}
}
