package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class DummyGridcon extends AbstractOpenemsComponent implements GridconPCS {

	
	boolean ipu1Enabled = false;
	boolean ipu2Enabled = false;
	boolean ipu3Enabled = false;
	boolean dcDcEnabled = false;
	
	boolean ipu1Running = false;
	boolean ipu2Running = false;
	boolean ipu3Running = false;
	boolean dcDcRunning = false;
	
	float activePower = 0;
	float reactivePower = 0;
	private ParameterSet parameterSet;
	private Mode mode;
	private boolean syncApproval;
	private boolean blackStartApproval;
	private float u0;
	private PControlMode pControlMode;
	private float f0;
	private float qLimit;
	private float dcLinkVoltage;
	private Float weightA;
	private Float weightB;
	private Float weightC;
	private int stringControlMode;
	private double efficiencyLossDischargeFactor = 0;
	private double efficiencyLossChargeFactor = 0;
	
	public DummyGridcon(//
	) { //
		super(//
				OpenemsComponent.ChannelId.values(), //
				GridConChannelId.values() //
		);
	}
	
	@Override
	public float getMaxApparentPower() {
		float max = 0;
		if (ipu1Enabled)
			max = max + GridconPCS.MAX_POWER_PER_INVERTER;
		if (ipu2Enabled)
			max = max + GridconPCS.MAX_POWER_PER_INVERTER;
		if (ipu3Enabled)
			max = max + GridconPCS.MAX_POWER_PER_INVERTER;
		return max;
	}

	@Override
	public boolean isRunning() {
		boolean running = isDcDcStarted() && isIpusStarted(ipu3Enabled, ipu2Enabled, ipu3Enabled);
		return running;		
	}

	@Override
	public boolean isStopped() {
		boolean stopped = true;
		stopped = stopped && !dcDcRunning;
		
		if (ipu1Enabled)
			stopped = stopped && !ipu1Running;
		if (ipu2Enabled)
			stopped = stopped && !ipu2Running;
		if (ipu3Enabled)
			stopped = stopped && !ipu3Running;		
		
		return stopped;
	}

	@Override
	public boolean isError() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPower(int activePower, int reactivePower) {
		if (isRunning()) {
			this.activePower = activePower;
			this.reactivePower = reactivePower;
		}
	}

	@Override
	public void setStop(boolean stop) {
		dcDcRunning = false;
		ipu1Running = false;
		ipu2Running = false;
		ipu3Running = false;
	}

	@Override
	public void setPlay(boolean play) {		
		if (dcDcEnabled)
			dcDcRunning = true;
		if (ipu1Enabled)
			ipu1Running = true;
		if (ipu2Enabled)
			ipu2Running = true;
		if (ipu3Enabled)
			ipu3Running = true;		
	}

	@Override
	public void setAcknowledge(boolean acknowledge) {
		// TODO Auto-generated method stub
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
	public float getActivePower() {
		return activePower;
	}

	@Override
	public float getDcLinkPositiveVoltage() {
		float voltage = 0;
		if (isDcDcStarted())
			voltage = dcLinkVoltage;
		
		return voltage;
	}

	@Override
	public boolean isCommunicationBroken() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEnableIPU1(boolean enabled) {
		ipu1Enabled = enabled;
	}

	@Override
	public void setEnableIPU2(boolean enabled) {
		ipu2Enabled = enabled;
	}

	@Override
	public void setEnableIPU3(boolean enabled) {
		ipu3Enabled = enabled;
	}

	@Override
	public void enableDCDC() {
		dcDcEnabled = true;
	}

	@Override
	public void disableDCDC() {
		dcDcEnabled = false;
	}

	@Override
	public void setParameterSet(ParameterSet set) {
		parameterSet = set;
	}

	@Override
	public void setModeSelection(Mode mode) {
		this.mode = mode;
	}

	@Override
	public void setSyncApproval(boolean b) {
		syncApproval = b;
	}

	@Override
	public void setBlackStartApproval(boolean b) {
		blackStartApproval = b;
	}

	@Override
	public void setU0(float u0) {
		this.u0 = u0;
	}

	@Override
	public void setF0(float frequencyFactor) {
		f0 = frequencyFactor;
	}

	@Override
	public void setPControlMode(PControlMode mode) {
		pControlMode = mode;
	}

	@Override
	public void setQLimit(float f) {
		qLimit = f;
	}

	@Override
	public void setPMaxChargeIPU1(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxDischargeIPU1(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxChargeIPU2(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxDischargeIPU2(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxChargeIPU3(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPMaxDischargeIPU3(float maxPower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDcLinkVoltage(float dcLinkVoltageSetpoint) {
		dcLinkVoltage = dcLinkVoltageSetpoint;
	}

	@Override
	public void setWeightStringA(Float weight) {
		weightA = weight;
	}

	@Override
	public void setWeightStringB(Float weight) {
		weightB = weight;
	}

	@Override
	public void setWeightStringC(Float weight) {
		weightC = weight;
	}

	@Override
	public void setStringControlMode(int stringControlMode) {
		this.stringControlMode = stringControlMode;
	}

	@Override
	public int getErrorCount() {
		// TODO Auto-generated method stub
		return 0;
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
	public boolean isDcDcStarted() {		
		return dcDcEnabled && dcDcRunning;
	}

	@Override
	public boolean isIpusStarted(boolean enableIPU1, boolean enableIPU2, boolean enableIPU3) {
		boolean running = true;
		if (!ipu1Enabled && !ipu2Enabled && !ipu3Enabled) 
			running = false;
		
		if (ipu1Enabled)
			running = running && ipu1Running;
		if (ipu2Enabled)
			running = running && ipu2Running;
		if (ipu3Enabled)
			running = running && ipu3Running;
		
		return running;
	}

	@Override
	public void doWriteTasks() throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

	@Override
	public float getReactivePower() {
		return reactivePower;
	}

	@Override
	public float getActivePowerPreset() {
		// TODO Auto-generated method stub
		return activePower;
	}

	@Override
	public double getEfficiencyLossChargeFactor() {
		return efficiencyLossChargeFactor;
	}

	@Override
	public double getEfficiencyLossDischargeFactor() {
		return efficiencyLossDischargeFactor;
	}

}
