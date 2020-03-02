package io.openems.edge.ess.mr.gridcon.writewords;

public class IpuInverterControlWord {

	// 32592
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
	
}
