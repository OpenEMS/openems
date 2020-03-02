package io.openems.edge.ess.mr.gridcon.writewords;

import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class CcuControlParametersWord {

	// 32592
	private float uByQDroopMain = 0f;
	private float uByQDroopT1Main = 0f;
	private float fByPDroopMain = 0f;
	private float fByPDroopT1Main = 0f;
	private float qByUDroopMain = 0f;
	private float qByUDeadBand = 0f;
	private float qLimit = 0f;
	private float pByFDroopMain = 0f;
	private float pByFDeadBand = 0f;
	private float pByUDroop = 0f;
	private float pByUDeadBand = 0f;
	private float pByUMaxCharge = 0f;
	private float pByUMaxDischarge = 0f;
	private PControlMode pControlMode = PControlMode.ACTIVE_POWER_CONTROL;
	private float pControlLimTwo = 0f;
	private float pControlLimOne = 0f;
	public float getqLimit() {
		return qLimit;
	}
	public void setqLimit(float qLimit) {
		this.qLimit = qLimit;
	}
	public PControlMode getpControlMode() {
		return pControlMode;
	}
	public void setpControlMode(PControlMode pControlMode) {
		this.pControlMode = pControlMode;
	}
	public float getuByQDroopMain() {
		return uByQDroopMain;
	}
	public float getuByQDroopT1Main() {
		return uByQDroopT1Main;
	}
	public float getfByPDroopMain() {
		return fByPDroopMain;
	}
	public float getfByPDroopT1Main() {
		return fByPDroopT1Main;
	}
	public float getqByUDroopMain() {
		return qByUDroopMain;
	}
	public float getqByUDeadBand() {
		return qByUDeadBand;
	}
	public float getpByFDroopMain() {
		return pByFDroopMain;
	}
	public float getpByFDeadBand() {
		return pByFDeadBand;
	}
	public float getpByUDroop() {
		return pByUDroop;
	}
	public float getpByUDeadBand() {
		return pByUDeadBand;
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
}
