package io.openems.edge.ess.mr.gridcon;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

/**
 * Describes functions of the gridcon pcs system 
 *
 */
public interface GridconPCS {
	
	public static final int MAX_POWER_PER_INVERTER = 42_000;
	public static final float DC_LINK_VOLTAGE_SETPOINT = 800f;
	public static final float Q_LIMIT = 1f;
	public static final int POWER_PRECISION_WATT =  1; //100 TODO estimated value;
	
	float getMaxApparentPower();
	boolean isRunning();
	boolean isStopped();
	boolean isError();
	void setPower(int activePower, int reactivePower);
	void setStop(boolean stop);
	void setPlay(boolean play);
	void setAcknowledge(boolean acknowledge);
	void setErrorCodeFeedback(int errorCodeFeedback);
	int getErrorCode();
	float getActivePower();
//	float getActivePowerInverter1();
//	float getActivePowerInverter2();
//	float getActivePowerInverter3();
	float getDcLinkPositiveVoltage();
	boolean isCommunicationBroken();
	
	void setEnableIPU1(boolean enabled);
	void setEnableIPU2(boolean enabled);
	void setEnableIPU3(boolean enabled);
//	void setEnableIPU4(boolean enabled);
	void enableDCDC();
	void disableDCDC();	
	
	void setParameterSet(ParameterSet set1);
	void setModeSelection(Mode currentControl);
	
	void setSyncApproval(boolean b);
	void setBlackStartApproval(boolean b);
//	void setShortCircuitHAndling(boolean b);
	void setU0(float onGridVoltageFactor);
	void setF0(float onGridFrequencyFactor);
	void setPControlMode(PControlMode activePowerControl);
	void setQLimit(float f);
	
	void setPMaxChargeIPU1(float maxPower); 
	void setPMaxDischargeIPU1(float maxPower);
	void setPMaxChargeIPU2(float maxPower); 
	void setPMaxDischargeIPU2(float maxPower);
	void setPMaxChargeIPU3(float maxPower); 
	void setPMaxDischargeIPU3(float maxPower);
	
	void setDcLinkVoltage(float dcLinkVoltageSetpoint);
	void setWeightStringA(Float weight);
	void setWeightStringB(Float weight);
	void setWeightStringC(Float weight);
	void setStringControlMode(int stringControlMode);
	
	int getErrorCount();
	void setSyncDate(int date);
	void setSyncTime(int time);
	
	boolean isDcDcStarted();
	boolean isIpusStarted(boolean enableIPU1, boolean enableIPU2, boolean enableIPU3);
	void doWriteTasks() throws OpenemsNamedException;
}