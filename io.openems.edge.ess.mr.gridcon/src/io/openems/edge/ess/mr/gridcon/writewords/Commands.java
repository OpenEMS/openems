package io.openems.edge.ess.mr.gridcon.writewords;

import java.util.BitSet;

import io.openems.edge.ess.mr.gridcon.enums.Mode;

public class Commands {
	
private static Commands instance;

	public static int COMMANDS_ADRESS = 32560;

	private Commands() {
		
	}
	
	public static Commands getCommands() {
		if (instance == null) {
			instance = new Commands();
		}
		return instance;
	}

	// 32560
	private Boolean enableIpu1 = false;
	private Boolean enableIpu2 = false;
	private Boolean enableIpu3 = false;
	private Boolean enableIpu4 = false;

	// 32561
	private Boolean playBit = false;
	private Boolean readyAndStopBit2nd = false;
	private Boolean acknowledgeBit = false;
	private Boolean stopBit1st = false;
	private Boolean blackstartApproval = false;
	private Boolean syncApproval = false;
	private Boolean shortCircuitHandling = false;
	private Mode modeSelection = Mode.CURRENT_CONTROL;
	private Boolean triggerSia = false;
	private Boolean harmonicCompensation = false;
	private Boolean parameterSet1 = false;
	private Boolean parameterSet2 = false;
	private Boolean parameterSet3 = false;
	private Boolean parameterSet4 = false;

	// 32562
	private Integer errorCodeFeedback = 0;
	private Float parameterU0 = 0f;
	private Float parameterF0 = 0f;
	private Float parameterQref = 0f;
	private Float parameterPref = 0f;
	private Integer syncDate = 0;
	private Integer syncTime = 0;
	
	public Boolean isEnableIpu1() {
		return enableIpu1;
	}
	public void setEnableIpu1(Boolean enable) {
		this.enableIpu1 = enable;
	}
	public Boolean isEnableIpu2() {
		return enableIpu2;
	}
	public void setEnableIpu2(Boolean enable) {
		this.enableIpu2 = enable;
	}
	public Boolean isEnableIpu3() {
		return enableIpu3;
	}
	public void setEnableIpu3(Boolean enable) {
		this.enableIpu3 = enable;
	}
	public Boolean isEnableIpu4() {
		return enableIpu4;
	}
	public void setEnableIpu4(Boolean enable) {
		this.enableIpu4 = enable;
	}
	public Boolean isBlackstartApproval() {
		return blackstartApproval;
	}
	public Boolean getPlayBit() {
		return playBit;
	}

	public void setPlayBit(Boolean playBit) {
		this.playBit = playBit;
	}

	public Boolean getReadyAndStopBit2nd() {
		return readyAndStopBit2nd;
	}

	public void setReadyAndStopBit2nd(Boolean readyAndStopBit2nd) {
		this.readyAndStopBit2nd = readyAndStopBit2nd;
	}

	public Boolean getAcknowledgeBit() {
		return acknowledgeBit;
	}

	public void setAcknowledgeBit(Boolean acknowledgeBit) {
		this.acknowledgeBit = acknowledgeBit;
	}

	public Boolean getStopBit1st() {
		return stopBit1st;
	}

	public void setStopBit1st(Boolean stopBit1st) {
		this.stopBit1st = stopBit1st;
	}

	public void setBlackstartApproval(Boolean blackstartApproval) {
		this.blackstartApproval = blackstartApproval;
	}
	public Boolean isSyncApproval() {
		return syncApproval;
	}
	public void setSyncApproval(Boolean syncApproval) {
		this.syncApproval = syncApproval;
	}
	public Boolean isShortCircuitHandling() {
		return shortCircuitHandling;		
	}	
	public Mode getModeSelection() {
		return modeSelection;
	}
	public void setModeSelection(Mode modeSelection) {
		this.modeSelection = modeSelection;
	}
	public Boolean isTriggerSia() {
		return triggerSia;
	}	
	public Boolean isHarmonicCompensation() {
		return harmonicCompensation;
	}
	public Boolean isParameterSet1() {
		return parameterSet1;
	}
	public void setParameterSet1(Boolean parameterSet1) {
		this.parameterSet1 = parameterSet1;
	}
	public Boolean isParameterSet2() {
		return parameterSet2;
	}
	public void setParameterSet2(Boolean parameterSet2) {
		this.parameterSet2 = parameterSet2;
	}
	public Boolean isParameterSet3() {
		return parameterSet3;
	}
	public void setParameterSet3(Boolean parameterSet3) {
		this.parameterSet3 = parameterSet3;
	}
	public Boolean isParameterSet4() {
		return parameterSet4;
	}
	public void setParameterSet4(Boolean parameterSet4) {
		this.parameterSet4 = parameterSet4;
	}
	public Integer getErrorCodeFeedback() {
		return errorCodeFeedback;
	}
	public void setErrorCodeFeedback(Integer errorCodeFeedback) {
		this.errorCodeFeedback = errorCodeFeedback;
	}
	public Float getParameterU0() {
		return parameterU0;
	}
	public void setParameterU0(Float parameterU0) {
		this.parameterU0 = parameterU0;
	}
	public Float getParameterF0() {
		return parameterF0;
	}
	public void setParameterF0(Float parameterF0) {
		this.parameterF0 = parameterF0;
	}
	public Float getParameterQref() {
		return parameterQref;
	}
	public void setParameterQref(Float parameterQref) {
		this.parameterQref = parameterQref;
	}
	public Float getParameterPref() {
		return parameterPref;
	}
	public void setParameterPref(Float parameterPref) {
		this.parameterPref = parameterPref;
	}
	public Integer getSyncDate() {
		return syncDate;
	}
	public void setSyncDate(Integer syncDate) {
		this.syncDate = syncDate;
	}
	public Integer getSyncTime() {
		return syncTime;
	}
	public void setSyncTime(Integer syncTime) {
		this.syncTime = syncTime;
	}

	@Override
	public String toString() {
		return "Commands [enableIpu1=" + enableIpu1 + ", enableIpu2=" + enableIpu2 + ", enableIpu3=" + enableIpu3
				+ ", enableIpu4=" + enableIpu4 + ", playBit=" + playBit + ", readyAndStopBit2nd=" + readyAndStopBit2nd
				+ ", acknowledgeBit=" + acknowledgeBit + ", stopBit1st=" + stopBit1st + ", blackstartApproval="
				+ blackstartApproval + ", syncApproval=" + syncApproval + ", shortCircuitHandling="
				+ shortCircuitHandling + ", modeSelection=" + modeSelection + ", triggerSia=" + triggerSia
				+ ", harmonicCompensation=" + harmonicCompensation + ", parameterSet1=" + parameterSet1
				+ ", parameterSet2=" + parameterSet2 + ", parameterSet3=" + parameterSet3 + ", parameterSet4="
				+ parameterSet4 + ", errorCodeFeedback=" + errorCodeFeedback + ", parameterU0=" + parameterU0
				+ ", parameterF0=" + parameterF0 + ", parameterQref=" + parameterQref + ", parameterPref="
				+ parameterPref + ", syncDate=" + syncDate + ", syncTime=" + syncTime + "\n --> getBitsandBytes: " + getBitsandBytes() + "\n" + getHexRepresentation() + "]";
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(Commands.COMMANDS_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.formatShort(getCmdWord1(), true));
		sb.append(" ");
		sb.append(HexFormatter.format(getCmdWord2(), true));
		sb.append("  ");
		sb.append(HexFormatter.format(errorCodeFeedback, true));
		sb.append("  ");
		sb.append(HexFormatter.format(parameterU0, true));
		sb.append("  ");
		sb.append(HexFormatter.format(parameterF0, true));
		sb.append("  ");
		sb.append(HexFormatter.format(parameterQref, true));
		sb.append("  ");
		sb.append(HexFormatter.format(parameterPref, true));
		sb.append("  ");
		sb.append(HexFormatter.format(syncDate, true));
		sb.append("  ");
		sb.append(HexFormatter.format(syncTime, true));
		
		return sb.toString();
	}
	
	private short getCmdWord1() {
		BitSet set = new BitSet(16);
		set.set(0, getStopBit1st()); //
		set.set(1, getPlayBit());
		set.set(2, getReadyAndStopBit2nd());
		set.set(3, getAcknowledgeBit());
		set.set(4, isBlackstartApproval());
		set.set(5, isSyncApproval());
		set.set(6, isShortCircuitHandling());
		set.set(7, modeSelection.value);
		set.set(8, isTriggerSia());
		set.set(9, isHarmonicCompensation());
		set.set(10, isParameterSet1());
		set.set(11, isParameterSet2());
		set.set(12, isParameterSet3());
		set.set(13, isParameterSet4());
		
		long val = 0;		
		long[] l = set.toLongArray();
		if (l.length == 0) {
			val = 0;
		} else {
			val = l[0];
		}
		
		Short v1 = (short) val;
		return v1;
	}
		
	private int getCmdWord2() {
		BitSet set = new BitSet(16);
		set.set(28 - 16, enableIpu4); //
		set.set(29 - 16, enableIpu3); //
		set.set(30 - 16, enableIpu2); //
		set.set(31 - 16, enableIpu1); //
		
		long val = 0;		
		long[] l = set.toLongArray();
		if (l.length == 0) {
			val = 0;
		} else {
			val = l[0];
		}
		
		int v1 =  (int) val;
		return v1;
	}
	
	private String getBitsandBytes() {
		BitSet set = new BitSet(32);
		set.set(0, getStopBit1st()); //
		set.set(1, getPlayBit());
		set.set(2, getReadyAndStopBit2nd());
		set.set(3, getAcknowledgeBit());
		set.set(4, isBlackstartApproval());
		set.set(5, isSyncApproval());
		set.set(6, isShortCircuitHandling());
		set.set(7, modeSelection.value);
		set.set(8, isTriggerSia());
		set.set(9, isHarmonicCompensation());
		set.set(10, isParameterSet1());
		set.set(11, isParameterSet2());
		set.set(12, isParameterSet3());
		set.set(13, isParameterSet4());
		
		set.set(12 + 16, isEnableIpu4());
		set.set(13 + 16, isEnableIpu3());
		set.set(14 + 16, isEnableIpu2());
		set.set(15 + 16, isEnableIpu1());
		
		long val = 0;
		long[] l = set.toLongArray();
		if (l.length == 0) {
			val = 0;
		} else {
			val = l[0];
		}
		
		long l1 = val;
		
		Integer v1 = (int) (l1 >> 16);
		Integer v2 =  (int) (l1 & 0xff); 
		
		return "\nr1:" + v1 + " r2:" + v2;
	}

}
