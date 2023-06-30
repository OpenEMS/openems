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
		// TODO Auto-generated constructor stub
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
		// TODO Auto-generated method stub
	}

	@Override
	public int getErrorCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getDcLinkPositiveVoltage() {
		return this.dcLinkPositiveVoltage;
	}

	@Override
	public boolean isCommunicationBroken() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEnableIpu1(boolean enabled) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEnableIpu2(boolean enabled) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEnableIpu3(boolean enabled) {
		// TODO Auto-generated method stub

	}

	// @Override
	// public void setParameterSet(ParameterSet set1) {
	// // TODO Auto-generated method stub
	//
	// }

	@Override
	public void setU0(float onGridVoltageFactor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setF0(float onGridFrequencyFactor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPControlMode(PControlMode activePowerControl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setQLimit(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxChargeIpu1(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxDischargeIpu1(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxChargeIpu2(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxDischargeIpu2(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxChargeIpu3(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxDischargeIpu3(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDcLinkVoltage(float dcLinkVoltageSetpoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWeightStringA(Float weight) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWeightStringB(Float weight) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWeightStringC(Float weight) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStringControlMode(int stringControlMode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableDcDc() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSyncDate(int date) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSyncTime(int time) {
		// TODO Auto-generated method stub

	}

	@Override
	public float getActivePower() {
		return this.currentActivePower;
	}

	@Override
	public int getErrorCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void disableDcDc() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDcDcStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIpusStarted(boolean enableIpu1, boolean enableIpu2, boolean enableIpu3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void doWriteTasks() throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

	@Override
	public float getReactivePower() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getActivePowerPreset() {
		return this.currentActivePower;
	}

	@Override
	public double getEfficiencyLossChargeFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getEfficiencyLossDischargeFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setIRefStringA(Float current) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIRefStringB(Float current) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIRefStringC(Float current) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isUndefined() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setMode(Mode mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBalancingMode(BalancingMode balancingMode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFundamentalFrequencyMode(FundamentalFrequencyMode fundamentalFrequencyMode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHarmonicCompensationMode(HarmonicCompensationMode harmonicCompensationMode) {
		// TODO Auto-generated method stub

	}

	@Override
	public float getCurrentL1Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getCurrentL2Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getCurrentL3Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getCurrentLNGrid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getActivePowerL1Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getActivePowerL2Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getActivePowerL3Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getActivePowerSumGrid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getReactivePowerL1Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getReactivePowerL2Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getReactivePowerL3Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getReactivePowerSumGrid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getApparentPowerL1Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getApparentPowerL2Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getApparentPowerL3Grid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getApparentPowerSumGrid() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setCosPhiSetPoint1(float cosPhiSetPoint1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCosPhiSetPoint2(float cosPhiSetPoint2) {
		// TODO Auto-generated method stub

	}

}
