package io.openems.edge.ess.mr.gridcon.writewords;

public class DcdcControlCommandWord {

	private float dcVoltageSetpoint = 0f;
	private float weightStringA = 0f; 
	private float weightStringB = 0f; 
	private float weightStringC = 0f; 
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
}
