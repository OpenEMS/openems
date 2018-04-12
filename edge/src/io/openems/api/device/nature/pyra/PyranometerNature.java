package io.openems.api.device.nature.pyra;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ThingInfo;

@ThingInfo(title = "Pyranometer")
public interface PyranometerNature extends DeviceNature {

	public ReadChannel<Long> getDevType();
	public ReadChannel<Long> getDataSet();
	public ReadChannel<Long> getDevMode();
	public ReadChannel<Long> getStatusFlags();
	public ReadChannel<Long> getScaleFactor();
	public ReadChannel<Long> getSensor1();
	public ReadChannel<Long> getrRawData1();
	public ReadChannel<Long> getStDev1();
	public ReadChannel<Long> getBodyTemp();
	public ReadChannel<Long> getVSupply();
	public ReadChannel<Long> getVDAC();
	public ReadChannel<Long> getDacInp();
	public ReadChannel<Long> getInputVSensor();
	public ReadChannel<Long> getInputVBodyTemp();
	public ReadChannel<Long> getErrorCode();
	public ReadChannel<Long> getProtocolError();
	public ReadChannel<Long> getErrorCountPrio1();
	public ReadChannel<Long> getErrorCountPrio2();
	public ReadChannel<Long> getRestartCount();
	public ReadChannel<Long> getFalseStartCount();
	public ReadChannel<Long> getSensorOnTimeM();
	public ReadChannel<Long> getSensorOnTimeL();
	public ReadChannel<Long> getBatchNumber();
	public ReadChannel<Long> getSerialNumber();
	public ReadChannel<Long> getSoftwareVersion();
	public ReadChannel<Long> getHardwareVersion();
	public ReadChannel<Long> getNodeID();
	public WriteChannel<Long> getStatusFlag();
	public WriteChannel<Long> setWorkState();

	public ReadChannel<Boolean> getIoVoidDataFlag();
	public ReadChannel<Boolean>  getIoOverFlowError();
	public ReadChannel<Boolean>  getIoUnderFlowError();
	public ReadChannel<Boolean>  getIoErrorFlag();
	public ReadChannel<Boolean>  getIoADCError();
	public ReadChannel<Boolean>  getIoDACError();
	public ReadChannel<Boolean>  getIoCalibrationError();
	public ReadChannel<Boolean>  getIoUpdateFailed();
	public WriteChannel<Boolean> setClearError();
	public WriteChannel<Boolean> setRestartModbus();
	public WriteChannel<Boolean> setRoundOFF();
	public WriteChannel<Boolean> setAutoRange();
	public WriteChannel<Boolean> setFastResponse();
	public WriteChannel<Boolean> setTrackingFilter();



}
