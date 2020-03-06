package io.openems.edge.ess.mr.gridcon.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class DummyEss extends AbstractOpenemsComponent implements GridconPCS {

	protected DummyEss(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		// TODO Auto-generated constructor stub
	}

	public static int MAXIMUM_POWER = 10000;
	public static int DC_LINK_VOLTAGE = 800;
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
		return running;
	}

	@Override
	public boolean isStopped() {
		return !running;
	}

	@Override
	public boolean isError() {
		return error;
	}

	@Override
	public void setPower(int activePower, int reactivePower) {
		currentActivePower = activePower;
	}

	@Override
	public void setPlay(boolean play) {
		if (play) {
			running = true;		
		}
	}
	
	@Override
	public void setStop(boolean stop) {
		if (stop) {
			running = false;
		}
	}

	@Override
	public void setAcknowledge(boolean acknowledge) {
		if (acknowledge) {
			error = false;
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
		return dcLinkPositiveVoltage;
	}

	@Override
	public boolean isCommunicationBroken() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEnableIPU1(boolean enabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEnableIPU2(boolean enabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEnableIPU3(boolean enabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParameterSet(ParameterSet set1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setModeSelection(Mode currentControl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSyncApproval(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlackStartApproval(boolean b) {
		// TODO Auto-generated method stub
		
	}

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
	public void enableDCDC() {
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
		return currentActivePower;
	}

	@Override
	public int getErrorCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void disableDCDC() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDcDcStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIpusStarted(boolean enableIPU1, boolean enableIPU2, boolean enableIPU3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void doWriteTasks() throws OpenemsNamedException {
		// TODO Auto-generated method stub
		
	}
}
