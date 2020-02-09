package io.openems.edge.ess.mr.gridcon.writewords;

public class DcdcControlCommandWord {

	private float dcVoltageSetpoint = 0f;
	private float weightStringA = 0f; // is set in applyPower()
	private float weightStringB = 0f; // is set in applyPower()
	private float weightStringC = 0f; // is set in applyPower()
	private float iRefStringA = 0f;
	private float iRefStringB = 0f;
	private float iRefStringC = 0f;
	private float stringControlMode = 0f;
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
	public float getStringControlMode() {
		return stringControlMode;
	}
	public void setStringControlMode(float stringControlMode) {
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
	
//	public DcdcControlCommand dcVoltageSetpoint(float value) {
//		this.dcVoltageSetpoint = value;
//		return this;
//	}
//
//	public DcdcControlCommand iRefStringA(float value) {
//		this.iRefStringA = value;
//		return this;
//	}
//
//	public DcdcControlCommand iRefStringB(float value) {
//		this.iRefStringB = value;
//		return this;
//	}
//
//	public DcdcControlCommand iRefStringC(float value) {
//		this.iRefStringC = value;
//		return this;
//	}
//
//	public DcdcControlCommand weightStringA(float value) {
//		this.weightStringA = value;
//		return this;
//	}
//
//	public DcdcControlCommand weightStringB(float value) {
//		this.weightStringB = value;
//		return this;
//	}
//
//	public DcdcControlCommand weightStringC(float value) {
//		this.weightStringC = value;
//		return this;
//	}
//
//	public DcdcControlCommand stringControlMode(int weightingMode) {
//		this.stringControlMode = weightingMode;
//		return this;
//	}
//
//	public void writeToChannels(GridconPCSImpl parent) throws IllegalArgumentException, OpenemsNamedException {
////		 weighting is never allowed to be '0'
//		if (this.stringControlMode == 0) {
//			throw new OpenemsException("Calculated weight of '0' -> not allowed!");
//		}
//
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT, this.dcVoltageSetpoint);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, this.weightStringA);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, this.weightStringB);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, this.weightStringC);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_I_REF_STRING_A, this.iRefStringA);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_I_REF_STRING_B, this.iRefStringB);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_I_REF_STRING_C, this.iRefStringC);
//		this.writeValueToChannel(parent, GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE, this.stringControlMode);
//	}
//
//	private <T> void writeValueToChannel(GridconPCSImpl parent, GridConChannelId channelId, T value)
//			throws IllegalArgumentException, OpenemsNamedException {
//		((WriteChannel<?>) parent.channel(channelId)).setNextWriteValueFromObject(value);
//	}
}
