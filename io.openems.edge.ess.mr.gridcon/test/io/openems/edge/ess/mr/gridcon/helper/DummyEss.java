package io.openems.edge.ess.mr.gridcon.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.enums.BalancingMode;
import io.openems.edge.ess.mr.gridcon.enums.FundamentalFrequencyMode;
import io.openems.edge.ess.mr.gridcon.enums.HarmonicCompensationMode;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class DummyEss extends AbstractOpenemsComponent implements GridconPcs {

	protected DummyEss(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public static final int MAXIMUM_POWER = 10000;
	public static final int DC_LINK_VOLTAGE = 800;
	private int currentActivePower = 0;
	private boolean running;
	private boolean error;
	private float dcLinkPositiveVoltage = DC_LINK_VOLTAGE;

	@Override
	public float getMaxApparentPower() {
		return MAXIMUM_POWER;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isStopped() {
		return !this.running;
	}

	@Override
	public boolean isError() {
		return this.error;
	}

	@Override
	public void setPower(int activePower, int reactivePower) {
		this.currentActivePower = activePower;
	}

	@Override
	public void setPlay(boolean play) {
		if (play) {
			this.running = true;
		}
	}

	@Override
	public void setStop(boolean stop) {
		if (stop) {
			this.running = false;
		}
	}

	@Override
	public void setAcknowledge(boolean acknowledge) {
		if (acknowledge) {
			this.error = false;
		}
	}

	@Override
	public void setErrorCodeFeedback(int errorCodeFeedback) {
	}

	@Override
	public int getErrorCode() {
		return 0;
	}

	@Override
	public float getDcLinkPositiveVoltage() {
		return this.dcLinkPositiveVoltage;
	}

	@Override
	public boolean isCommunicationBroken() {
		return false;
	}

	@Override
	public void setEnableIpu1(boolean enabled) {
	}

	@Override
	public void setEnableIpu2(boolean enabled) {
	}

	@Override
	public void setEnableIpu3(boolean enabled) {
	}

	@Override
	public void setU0(float onGridVoltageFactor) {
	}

	@Override
	public void setF0(float onGridFrequencyFactor) {
	}

	@Override
	public void setPControlMode(PControlMode activePowerControl) {
	}

	@Override
	public void setQLimit(float f) {
	}

	@Override
	public void setPMaxChargeIpu1(float maxPower) {
	}

	@Override
	public void setPMaxDischargeIpu1(float maxPower) {
	}

	@Override
	public void setPMaxChargeIpu2(float maxPower) {
	}

	@Override
	public void setPMaxDischargeIpu2(float maxPower) {
	}

	@Override
	public void setPMaxChargeIpu3(float maxPower) {
	}

	@Override
	public void setPMaxDischargeIpu3(float maxPower) {
	}

	@Override
	public void setDcLinkVoltage(float dcLinkVoltageSetpoint) {
	}

	@Override
	public void setWeightStringA(Float weight) {
	}

	@Override
	public void setWeightStringB(Float weight) {
	}

	@Override
	public void setWeightStringC(Float weight) {
	}

	@Override
	public void setStringControlMode(int stringControlMode) {
	}

	@Override
	public void enableDcDc() {
	}

	@Override
	public void setSyncDate(int date) {
	}

	@Override
	public void setSyncTime(int time) {
	}

	@Override
	public float getActivePower() {
		return this.currentActivePower;
	}

	@Override
	public int getErrorCount() {
		return 0;
	}

	@Override
	public void disableDcDc() {
	}

	@Override
	public boolean isDcDcStarted() {
		return false;
	}

	@Override
	public boolean isIpusStarted(boolean enableIpu1, boolean enableIpu2, boolean enableIpu3) {
		return false;
	}

	@Override
	public void doWriteTasks() throws OpenemsNamedException {
	}

	@Override
	public float getReactivePower() {
		return 0;
	}

	@Override
	public float getActivePowerPreset() {
		return this.currentActivePower;
	}

	@Override
	public double getEfficiencyLossChargeFactor() {
		return 0;
	}

	@Override
	public double getEfficiencyLossDischargeFactor() {
		return 0;
	}

	@Override
	public void setIRefStringA(Float current) {
	}

	@Override
	public void setIRefStringB(Float current) {
	}

	@Override
	public void setIRefStringC(Float current) {
	}

	@Override
	public boolean isUndefined() {
		return false;
	}

	@Override
	public void setMode(Mode mode) {
	}

	@Override
	public void setBalancingMode(BalancingMode balancingMode) {
	}

	@Override
	public void setFundamentalFrequencyMode(FundamentalFrequencyMode fundamentalFrequencyMode) {
	}

	@Override
	public void setHarmonicCompensationMode(HarmonicCompensationMode harmonicCompensationMode) {
	}

	@Override
	public float getCurrentL1Grid() {
		return 0;
	}

	@Override
	public float getCurrentL2Grid() {
		return 0;
	}

	@Override
	public float getCurrentL3Grid() {
		return 0;
	}

	@Override
	public float getCurrentLNGrid() {
		return 0;
	}

	@Override
	public float getActivePowerL1Grid() {
		return 0;
	}

	@Override
	public float getActivePowerL2Grid() {
		return 0;
	}

	@Override
	public float getActivePowerL3Grid() {
		return 0;
	}

	@Override
	public float getActivePowerSumGrid() {
		return 0;
	}

	@Override
	public float getReactivePowerL1Grid() {
		return 0;
	}

	@Override
	public float getReactivePowerL2Grid() {
		return 0;
	}

	@Override
	public float getReactivePowerL3Grid() {
		return 0;
	}

	@Override
	public float getReactivePowerSumGrid() {
		return 0;
	}

	@Override
	public float getApparentPowerL1Grid() {
		return 0;
	}

	@Override
	public float getApparentPowerL2Grid() {
		return 0;
	}

	@Override
	public float getApparentPowerL3Grid() {
		return 0;
	}

	@Override
	public float getApparentPowerSumGrid() {
		return 0;
	}

	@Override
	public void setCosPhiSetPoint1(float cosPhiSetPoint1) {
	}

	@Override
	public void setCosPhiSetPoint2(float cosPhiSetPoint2) {
	}

}
