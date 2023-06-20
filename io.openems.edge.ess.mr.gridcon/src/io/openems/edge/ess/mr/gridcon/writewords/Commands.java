package io.openems.edge.ess.mr.gridcon.writewords;

import java.util.BitSet;

import io.openems.edge.ess.mr.gridcon.enums.BalancingMode;
import io.openems.edge.ess.mr.gridcon.enums.FundamentalFrequencyMode;
import io.openems.edge.ess.mr.gridcon.enums.HarmonicCompensationMode;
import io.openems.edge.ess.mr.gridcon.enums.Mode;

public class Commands {

	public static final int COMMANDS_ADRESS = 32560;

	public Commands() {
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
	private Mode mode = null; // Mode.CURRENT_CONTROL;
	private Boolean triggerSia = false;
	private BalancingMode balancingMode = BalancingMode.DISABLED;
	private FundamentalFrequencyMode fundamentalFrequencyMode = FundamentalFrequencyMode.DISABLED;
	private HarmonicCompensationMode harmonicCompensationMode = HarmonicCompensationMode.DISABLED;
	// private Boolean parameterSet1 = false;
	// private Boolean parameterSet2 = false;
	// private Boolean parameterSet3 = false;
	// private Boolean parameterSet4 = false;

	// 32562
	private Integer errorCodeFeedback = 0;
	private Float parameterU0 = 0f;
	private Float parameterF0 = 0f;
	private Float parameterQref = 0f;
	private Float parameterPref = 0f;
	private Integer syncDate = 0;
	private Integer syncTime = 0;

	public Boolean isEnableIpu1() {
		return this.enableIpu1;
	}

	public void setEnableIpu1(Boolean enable) {
		this.enableIpu1 = enable;
	}

	public Boolean isEnableIpu2() {
		return this.enableIpu2;
	}

	public void setEnableIpu2(Boolean enable) {
		this.enableIpu2 = enable;
	}

	public Boolean isEnableIpu3() {
		return this.enableIpu3;
	}

	public void setEnableIpu3(Boolean enable) {
		this.enableIpu3 = enable;
	}

	public Boolean isEnableIpu4() {
		return this.enableIpu4;
	}

	public void setEnableIpu4(Boolean enable) {
		this.enableIpu4 = enable;
	}

	public Boolean getPlayBit() {
		return this.playBit;
	}

	public void setPlayBit(Boolean playBit) {
		this.playBit = playBit;
	}

	public Boolean getReadyAndStopBit2nd() {
		return this.readyAndStopBit2nd;
	}

	public void setReadyAndStopBit2nd(Boolean readyAndStopBit2nd) {
		this.readyAndStopBit2nd = readyAndStopBit2nd;
	}

	public Boolean getAcknowledgeBit() {
		return this.acknowledgeBit;
	}

	public void setAcknowledgeBit(Boolean acknowledgeBit) {
		this.acknowledgeBit = acknowledgeBit;
	}

	public Boolean getStopBit1st() {
		return this.stopBit1st;
	}

	public void setStopBit1st(Boolean stopBit1st) {
		this.stopBit1st = stopBit1st;
	}

	public Boolean isBlackstartApproval() {
		return this.blackstartApproval;
	}

	// public void setBlackstartApproval(Boolean blackstartApproval) {
	// this.blackstartApproval = blackstartApproval;
	// }

	public Boolean isSyncApproval() {
		return this.syncApproval;
	}

	// public void setSyncApproval(Boolean syncApproval) {
	// this.syncApproval = syncApproval;
	// }

	public Boolean isShortCircuitHandling() {
		return this.shortCircuitHandling;
	}

	public Mode getMode() {
		return this.mode;
	}

	/*
	 * This is the only method that is reachable from outside to set the mode
	 */
	public void setMode(Mode mode) {
		this.mode = mode;

		boolean isSyncApproval = (mode == Mode.CURRENT_CONTROL);

		this.syncApproval = isSyncApproval;
		this.blackstartApproval = !isSyncApproval;
	}

	public Boolean isTriggerSia() {
		return this.triggerSia;
	}

	public BalancingMode getBalancingMode() {
		return this.balancingMode;
	}

	public void setBalancingMode(BalancingMode balancingMode) {
		this.balancingMode = balancingMode;
	}

	public FundamentalFrequencyMode getFundamentalFrequencyMode() {
		return this.fundamentalFrequencyMode;
	}

	public void setFundamentalFrequencyMode(FundamentalFrequencyMode fundamentalFrequencyMode) {
		this.fundamentalFrequencyMode = fundamentalFrequencyMode;
	}

	public HarmonicCompensationMode getHarmonicCompensationMode() {
		return this.harmonicCompensationMode;
	}

	public void setHarmonicCompensationMode(HarmonicCompensationMode harmonicCompensationMode) {
		this.harmonicCompensationMode = harmonicCompensationMode;
	}

	// public Boolean isParameterSet1() {
	// return parameterSet1;
	// }
	//
	// public void setParameterSet1(Boolean parameterSet1) {
	// this.parameterSet1 = parameterSet1;
	// }
	//
	// public Boolean isParameterSet2() {
	// return parameterSet2;
	// }
	//
	// public void setParameterSet2(Boolean parameterSet2) {
	// this.parameterSet2 = parameterSet2;
	// }
	//
	// public Boolean isParameterSet3() {
	// return parameterSet3;
	// }
	//
	// public void setParameterSet3(Boolean parameterSet3) {
	// this.parameterSet3 = parameterSet3;
	// }
	//
	// public Boolean isParameterSet4() {
	// return parameterSet4;
	// }
	//
	// public void setParameterSet4(Boolean parameterSet4) {
	// this.parameterSet4 = parameterSet4;
	// }

	public Integer getErrorCodeFeedback() {
		return this.errorCodeFeedback;
	}

	public void setErrorCodeFeedback(Integer errorCodeFeedback) {
		this.errorCodeFeedback = errorCodeFeedback;
	}

	public Float getParameterU0() {
		return this.parameterU0;
	}

	public void setParameterU0(Float parameterU0) {
		this.parameterU0 = parameterU0;
	}

	public Float getParameterF0() {
		return this.parameterF0;
	}

	public void setParameterF0(Float parameterF0) {
		this.parameterF0 = parameterF0;
	}

	public Float getParameterQref() {
		return this.parameterQref;
	}

	public void setParameterQref(Float parameterQref) {
		this.parameterQref = parameterQref;
	}

	public Float getParameterPref() {
		return this.parameterPref;
	}

	public void setParameterPref(Float parameterPref) {
		this.parameterPref = parameterPref;
	}

	public Integer getSyncDate() {
		return this.syncDate;
	}

	public void setSyncDate(Integer syncDate) {
		this.syncDate = syncDate;
	}

	public Integer getSyncTime() {
		return this.syncTime;
	}

	public void setSyncTime(Integer syncTime) {
		this.syncTime = syncTime;
	}

	@Override
	public String toString() {
		return "Commands [" //
				+ "enableIpu1=" + this.enableIpu1 + ", " //
				+ "enableIpu2=" + this.enableIpu2 + ", " //
				+ "enableIpu3=" + this.enableIpu3 + ", " //
				+ "enableIpu4=" + this.enableIpu4 + ", " //
				+ "playBit=" + this.playBit + ", " //
				+ "readyAndStopBit2nd=" + this.readyAndStopBit2nd + ", " //
				+ "acknowledgeBit=" + this.acknowledgeBit + ", " //
				+ "stopBit1st=" + this.stopBit1st + ", " //
				+ "blackstartApproval=" + this.blackstartApproval + ", " //
				+ "syncApproval=" + this.syncApproval + ", " //
				+ "shortCircuitHandling=" + this.shortCircuitHandling + ", " //
				+ "mode=" + this.mode + ", " //
				+ "triggerSia=" + this.triggerSia + ", " //
				+ "fundamentalFrequencyMode=" + this.fundamentalFrequencyMode + ", " //
				+ "balancingMode=" + this.balancingMode + ", " //
				+ "harmonicCompensationMode=" + this.harmonicCompensationMode + ", " //
				+ "errorCodeFeedback=" + this.errorCodeFeedback + ", " //
				+ "parameterU0=" + this.parameterU0 + ", " //
				+ "parameterF0=" + this.parameterF0 + ", " //
				+ "parameterQref=" + this.parameterQref + ", " //
				+ "parameterPref=" + this.parameterPref + ", " //
				+ "syncDate=" + this.syncDate + ", " //
				+ "syncTime=" + this.syncTime //
				+ "]" //
				+ "\n" //
				+ this.getHexRepresentation();
	}

	private String getHexRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(Commands.COMMANDS_ADRESS);
		sb.append(": ");
		sb.append(HexFormatter.formatShort(this.getCmdWord1(), true));
		sb.append(" ");
		sb.append(HexFormatter.format(this.getCmdWord2(), true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.errorCodeFeedback, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.parameterU0, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.parameterF0, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.parameterQref, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.parameterPref, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.syncDate, true));
		sb.append("  ");
		sb.append(HexFormatter.format(this.syncTime, true));

		return sb.toString();
	}

	private short getCmdWord1() {
		BitSet set = new BitSet(16);
		set.set(0, this.getStopBit1st()); //
		set.set(1, this.getPlayBit());
		set.set(2, this.getReadyAndStopBit2nd());
		set.set(3, this.getAcknowledgeBit());
		set.set(4, this.isBlackstartApproval());
		set.set(5, this.isSyncApproval());
		set.set(6, this.isShortCircuitHandling());
		set.set(7, this.mode.value);
		set.set(8, this.isTriggerSia());
		set.set(9, this.fundamentalFrequencyMode.isBit1());
		set.set(10, this.fundamentalFrequencyMode.isBit2());
		set.set(11, this.getBalancingMode().isBit1());
		set.set(12, this.getBalancingMode().isBit2());
		set.set(13, this.harmonicCompensationMode.isBit1());
		set.set(14, this.harmonicCompensationMode.isBit2());
		// set.set(10, isParameterSet1());
		// set.set(11, isParameterSet2());
		// set.set(12, isParameterSet3());
		// set.set(13, isParameterSet4());

		long val = 0;
		long[] l = set.toLongArray();
		if (l.length == 0) {
			val = 0;
		} else {
			val = l[0];
		}
		return (short) val;
	}

	private int getCmdWord2() {
		BitSet set = new BitSet(16);
		set.set(28 - 16, this.enableIpu4); //
		set.set(29 - 16, this.enableIpu3); //
		set.set(30 - 16, this.enableIpu2); //
		set.set(31 - 16, this.enableIpu1); //

		long val = 0;
		long[] l = set.toLongArray();
		if (l.length == 0) {
			val = 0;
		} else {
			val = l[0];
		}
		return (int) val;
	}
}
