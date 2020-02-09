package io.openems.edge.ess.mr.gridcon.writewords;

import io.openems.edge.ess.mr.gridcon.enums.Mode;

public class CommandControlWord {

	// 32560
	private boolean enableIpu1 = false;
	private boolean enableIpu2 = false;
	private boolean enableIpu3 = false;
	private boolean enableIpu4 = false;

	// 32561
	private boolean play = false;
	private boolean ready = false;
	private boolean acknowledge = false;
	private boolean stop = false;
	private boolean blackstartApproval = false;
	private boolean syncApproval = false;
	private boolean shortCircuitHandling = false;
	private Mode modeSelection = Mode.CURRENT_CONTROL;
	private boolean triggerSia = false;
	private boolean harmonicCompensation = false;
	private boolean parameterSet1 = false;
	private boolean parameterSet2 = false;
	private boolean parameterSet3 = false;
	private boolean parameterSet4 = false;

	// 32562
	private int errorCodeFeedback = 0;
	private float parameterU0 = 0f;
	private float parameterF0 = 0f;
	private float parameterQref = 0f; // is set in applyPower()
	private float parameterPref = 0f; // is set in applyPower()
	private int syncDate = 0;
	private int syncTime = 0;
	
	public boolean isEnableIpu1() {
		return enableIpu1;
	}
	public void setEnableIpu1(boolean enable) {
		this.enableIpu1 = enable;
	}
	public boolean isEnableIpu2() {
		return enableIpu2;
	}
	public void setEnableIpu2(boolean enable) {
		this.enableIpu2 = enable;
	}
	public boolean isEnableIpu3() {
		return enableIpu3;
	}
	public void setEnableIpu3(boolean enable) {
		this.enableIpu3 = enable;
	}
	public boolean isEnableIpu4() {
		return enableIpu4;
	}
	public void setEnableIpu4(boolean enable) {
		this.enableIpu4 = enable;
	}
	public boolean isPlay() {
		return play;
	}
	public void setPlay(boolean play) {
		this.play = play;
	}
	public boolean isReady() {
		return ready;
	}
	public void setReady(boolean ready) {
		this.ready = ready;
	}
	public boolean isAcknowledge() {
		return acknowledge;
	}
	public void setAcknowledge(boolean acknowledge) {
		this.acknowledge = acknowledge;
	}
	public boolean isStop() {
		return stop;
	}
	public void setStop(boolean stop) {
		this.stop = stop;
	}
	public boolean isBlackstartApproval() {
		return blackstartApproval;
	}
	public void setBlackstartApproval(boolean blackstartApproval) {
		this.blackstartApproval = blackstartApproval;
	}
	public boolean isSyncApproval() {
		return syncApproval;
	}
	public void setSyncApproval(boolean syncApproval) {
		this.syncApproval = syncApproval;
	}
	public boolean isShortCircuitHandling() {
		return shortCircuitHandling;
	}
	public void setShortCircuitHandling(boolean shortCircuitHandling) {
		this.shortCircuitHandling = shortCircuitHandling;
	}
	public Mode getModeSelection() {
		return modeSelection;
	}
	public void setModeSelection(Mode modeSelection) {
		this.modeSelection = modeSelection;
	}
	public boolean isTriggerSia() {
		return triggerSia;
	}
	public void setTriggerSia(boolean triggerSia) {
		this.triggerSia = triggerSia;
	}
	public boolean isHarmonicCompensation() {
		return harmonicCompensation;
	}
	public void setHarmonicCompensation(boolean harmonicCompensation) {
		this.harmonicCompensation = harmonicCompensation;
	}
	public boolean isParameterSet1() {
		return parameterSet1;
	}
	public void setParameterSet1(boolean parameterSet1) {
		this.parameterSet1 = parameterSet1;
	}
	public boolean isParameterSet2() {
		return parameterSet2;
	}
	public void setParameterSet2(boolean parameterSet2) {
		this.parameterSet2 = parameterSet2;
	}
	public boolean isParameterSet3() {
		return parameterSet3;
	}
	public void setParameterSet3(boolean parameterSet3) {
		this.parameterSet3 = parameterSet3;
	}
	public boolean isParameterSet4() {
		return parameterSet4;
	}
	public void setParameterSet4(boolean parameterSet4) {
		this.parameterSet4 = parameterSet4;
	}
	public int getErrorCodeFeedback() {
		return errorCodeFeedback;
	}
	public void setErrorCodeFeedback(int errorCodeFeedback) {
		this.errorCodeFeedback = errorCodeFeedback;
	}
	public float getParameterU0() {
		return parameterU0;
	}
	public void setParameterU0(float parameterU0) {
		this.parameterU0 = parameterU0;
	}
	public float getParameterF0() {
		return parameterF0;
	}
	public void setParameterF0(float parameterF0) {
		this.parameterF0 = parameterF0;
	}
	public float getParameterQref() {
		return parameterQref;
	}
	public void setParameterQref(float parameterQref) {
		this.parameterQref = parameterQref;
	}
	public float getParameterPref() {
		return parameterPref;
	}
	public void setParameterPref(float parameterPref) {
		this.parameterPref = parameterPref;
	}
	public int getSyncDate() {
		return syncDate;
	}
	public void setSyncDate(int syncDate) {
		this.syncDate = syncDate;
	}
	public int getSyncTime() {
		return syncTime;
	}
	public void setSyncTime(int syncTime) {
		this.syncTime = syncTime;
	}
}
