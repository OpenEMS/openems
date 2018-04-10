/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.device.kippzonen;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.pyra.PyranometerNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusCoilReadChannel;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.DummyCoilElement;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "KippZonen Pyranometer")
public class KippZonenPyranometer extends ModbusDeviceNature implements PyranometerNature {

	public KippZonenPyranometer(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel devType;
	public ModbusReadLongChannel dataSet;
	public ModbusReadLongChannel devMode;
	public ModbusReadLongChannel statusFlags;
	public ModbusReadLongChannel scaleFactor;
	public ModbusReadLongChannel sensor1;
	public ModbusReadLongChannel rawData1;
	public ModbusReadLongChannel stDev1;
	public ModbusReadLongChannel bodyTemp;
	public ModbusReadLongChannel vSupply;
	public ModbusReadLongChannel vDAC;
	public ModbusReadLongChannel dacInp;
	public ModbusReadLongChannel inputVSensor;
	public ModbusReadLongChannel inputVBodyTemp;
	public ModbusReadLongChannel errorCode;
	public ModbusReadLongChannel protocolError;
	public ModbusReadLongChannel errorCountPrio1;
	public ModbusReadLongChannel errorCountPrio2;
	public ModbusReadLongChannel restartCount;
	public ModbusReadLongChannel falseStartCount;
	public ModbusReadLongChannel sensorOnTimeM;
	public ModbusReadLongChannel sensorOnTimeL;
	public ModbusReadLongChannel batchNumber;
	public ModbusReadLongChannel serialNumber;
	public ModbusReadLongChannel softwareVersion;
	public ModbusReadLongChannel hardwareVersion;
	public ModbusReadLongChannel nodeID;
	public ModbusWriteLongChannel statusFlag;
	public ModbusWriteLongChannel setWorkState;
	public ModbusCoilReadChannel ioVoidDataFlag;
	public ModbusCoilReadChannel ioOverFlowError;
	public ModbusCoilReadChannel ioUnderFlowError;
	public ModbusCoilReadChannel ioErrorFlag;
	public ModbusCoilReadChannel ioADCError;
	public ModbusCoilReadChannel ioDACError;
	public ModbusCoilReadChannel ioCalibrationError;
	public ModbusCoilReadChannel ioUpdateFailed;
	public ModbusCoilWriteChannel clearError;
	public ModbusCoilWriteChannel restartModbus;
	public ModbusCoilWriteChannel roundOFF;
	public ModbusCoilWriteChannel autoRange;
	public ModbusCoilWriteChannel fastResponse;
	public ModbusCoilWriteChannel trackingFilter;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		@SuppressWarnings("unchecked") ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(0, //
						new UnsignedWordElement(0, devType = new ModbusReadLongChannel("DeviceTypeOfTheSensor", this)), //
						new UnsignedWordElement(1, dataSet = new ModbusReadLongChannel("DataModelVersion", this)), //
						new UnsignedWordElement(2, devMode = new ModbusReadLongChannel("OperationalMode", this)), //
						new UnsignedWordElement(3, statusFlags = new ModbusReadLongChannel("StatusFlags", this)), //
						new SignedWordElement(4, scaleFactor = new ModbusReadLongChannel("ScaleFactor", this)), //
						new DummyElement(5), //
						new SignedWordElement(6,
								rawData1 = new ModbusReadLongChannel("NetRadiation", this).unit("W/m2")), //
						new SignedWordElement(7, stDev1 = new ModbusReadLongChannel("StandartDeviationSensor1", this).multiplier(-1).unit("W/m2")), //
						new SignedWordElement(8,
								bodyTemp = new ModbusReadLongChannel("BodyTemperature", this).multiplier(-1).unit("C")), //
						new SignedWordElement(9,
								vSupply = new ModbusReadLongChannel("ExternalPowerVoltage ", this).multiplier(-1).unit("V")), //
						new DummyElement(10, 15), //
						new UnsignedWordElement(16,
								vDAC = new ModbusReadLongChannel("DACOutputVoltage", this).unit("V")), //
						new UnsignedWordElement(17,
								dacInp = new ModbusReadLongChannel("DACSelectedInputVoltage", this))), //

				new ModbusRegisterRange(18, //
						new SignedDoublewordElement(18,
								inputVSensor = new ModbusReadLongChannel("InputVoltageSensor1", this).multiplier(-2)
								.unit("µV")), //
						new DummyElement(20, 21),
						new SignedDoublewordElement(22,
								inputVBodyTemp = new ModbusReadLongChannel("InputVoltageBodyTemperatureSensor", this)
								.multiplier(-2).unit("µV"))), //

				new ModbusRegisterRange(26, //
						new UnsignedWordElement(26, errorCode = new ModbusReadLongChannel("ActualErrorCode", this)), //
						new UnsignedWordElement(27,
								protocolError = new ModbusReadLongChannel("CommunicationError", this)), //
						new UnsignedWordElement(28,
								errorCountPrio1 = new ModbusReadLongChannel("IOErrorCountPrio1", this)), //
						new UnsignedWordElement(29,
								errorCountPrio2 = new ModbusReadLongChannel("IOErrorCountPrio2", this)), //
						new UnsignedWordElement(30,
								restartCount = new ModbusReadLongChannel("NumberOfControlledRestart", this)), //
						new UnsignedWordElement(31,
								falseStartCount = new ModbusReadLongChannel("NumberOfUncontrolledRestarts", this)), //
						new UnsignedWordElement(32,
								sensorOnTimeM = new ModbusReadLongChannel("OnTimeInSecondsMSB", this)), //
						new UnsignedWordElement(33,
								sensorOnTimeL = new ModbusReadLongChannel("OnTimeInSecondsLSB", this))), //

				new ModbusRegisterRange(41, //
						new UnsignedWordElement(41,
								batchNumber = new ModbusReadLongChannel("ProductionBatchNumber", this)), //
						new UnsignedWordElement(42, serialNumber = new ModbusReadLongChannel("SerialNumber", this)), //
						new UnsignedWordElement(43,
								softwareVersion = new ModbusReadLongChannel("SoftwareVersion", this)), //
						new UnsignedWordElement(44,
								hardwareVersion = new ModbusReadLongChannel("HardwareVersion", this)), //
						new UnsignedWordElement(45,
								nodeID = new ModbusReadLongChannel("ModbusDeviceAddressRS485", this))), //

				new ModbusCoilRange(2, //
						new CoilElement(2, ioVoidDataFlag = new ModbusCoilReadChannel("IOVoidDataFlag", this)), //
						new CoilElement(3, ioOverFlowError = new ModbusCoilReadChannel("IOOverFlowError", this)), //
						new CoilElement(4, ioUnderFlowError = new ModbusCoilReadChannel("IOUnderFlowError", this)), //
						new CoilElement(5, ioErrorFlag = new ModbusCoilReadChannel("IOErrorFlag", this)), //
						new CoilElement(6, ioADCError = new ModbusCoilReadChannel("IOADCError", this)), //
						new CoilElement(7, ioDACError = new ModbusCoilReadChannel("IODACError", this)), //
						new CoilElement(8, ioCalibrationError = new ModbusCoilReadChannel("IOCalibrationError", this)), //
						new CoilElement(9, ioUpdateFailed = new ModbusCoilReadChannel("IOUpdateFailed", this))), //

				new ModbusCoilRange(10,
						new CoilElement(10, clearError = new ModbusCoilWriteChannel("ClearError", this)), //
						new DummyCoilElement(11, 17), //
						new CoilElement(18, restartModbus = new ModbusCoilWriteChannel("RestartModbus", this)), //
						new DummyCoilElement(19), //
						new CoilElement(20, roundOFF = new ModbusCoilWriteChannel("RoundOFF", this)), //
						new CoilElement(21, autoRange = new ModbusCoilWriteChannel("AutoRange", this)), //
						new CoilElement(22, fastResponse = new ModbusCoilWriteChannel("FastResponse", this)), //
						new CoilElement(23, trackingFilter = new ModbusCoilWriteChannel("TrackingFilter", this))));
		return protocol;

	}

	@Override
	public ThingStateChannels getStateChannel() {
		//
		return thingState;
	}

	@Override
	public ReadChannel<Long> getDevType() {
		return this.devType;
	}

	@Override
	public ReadChannel<Long> getDataSet() {
		return dataSet;
	}

	@Override
	public ReadChannel<Long> getDevMode() {
		return devMode;
	}

	@Override
	public ReadChannel<Long> getStatusFlags() {
		return statusFlags;
	}

	@Override
	public ReadChannel<Long> getScaleFactor() {
		return scaleFactor;
	}

	@Override
	public ReadChannel<Long> getSensor1() {
		return sensor1;
	}

	@Override
	public ReadChannel<Long> getrRawData1() {
		return rawData1;
	}

	@Override
	public ReadChannel<Long> getStDev1() {
		return stDev1;
	}

	@Override
	public ReadChannel<Long> getBodyTemp() {
		return bodyTemp;
	}

	@Override
	public ReadChannel<Long> getVSupply() {
		return vSupply;
	}

	@Override
	public ReadChannel<Long> getVDAC() {
		return vDAC;
	}

	@Override
	public ReadChannel<Long> getDacInp() {
		return dacInp;
	}

	@Override
	public ReadChannel<Long> getInputVSensor() {
		return inputVSensor;
	}

	@Override
	public ReadChannel<Long> getInputVBodyTemp() {
		return inputVBodyTemp;
	}

	@Override
	public ReadChannel<Long> getErrorCode() {
		return errorCode;
	}

	@Override
	public ReadChannel<Long> getProtocolError() {
		return protocolError;
	}

	@Override
	public ReadChannel<Long> getErrorCountPrio1() {
		return errorCountPrio1;
	}

	@Override
	public ReadChannel<Long> getErrorCountPrio2() {
		return errorCountPrio2;
	}

	@Override
	public ReadChannel<Long> getRestartCount() {
		return restartCount;
	}

	@Override
	public ReadChannel<Long> getFalseStartCount() {
		return falseStartCount;
	}

	@Override
	public ReadChannel<Long> getSensorOnTimeM() {
		return sensorOnTimeM;
	}

	@Override
	public ReadChannel<Long> getSensorOnTimeL() {
		return sensorOnTimeL;
	}

	@Override
	public ReadChannel<Long> getBatchNumber() {
		return batchNumber;
	}

	@Override
	public ReadChannel<Long> getSerialNumber() {
		return serialNumber;
	}

	@Override
	public ReadChannel<Long> getSoftwareVersion() {
		return softwareVersion;
	}

	@Override
	public ReadChannel<Long> getHardwareVersion() {
		return hardwareVersion;
	}

	@Override
	public ReadChannel<Long> getNodeID() {
		return nodeID;
	}

	@Override
	public WriteChannel<Long> getStatusFlag() {

		return statusFlag;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

	@Override
	public ReadChannel<Boolean> getIoVoidDataFlag() {
		return ioVoidDataFlag;
	}

	@Override
	public ReadChannel<Boolean> getIoOverFlowError() {
		return ioOverFlowError;
	}

	@Override
	public ReadChannel<Boolean> getIoUnderFlowError() {
		return ioUnderFlowError;
	}

	@Override
	public ReadChannel<Boolean> getIoErrorFlag() {
		return ioErrorFlag;
	}

	@Override
	public ReadChannel<Boolean> getIoADCError() {
		return ioADCError;
	}

	@Override
	public ReadChannel<Boolean> getIoDACError() {
		return ioDACError;
	}

	@Override
	public ReadChannel<Boolean> getIoCalibrationError() {
		return ioCalibrationError;
	}

	@Override
	public ReadChannel<Boolean> getIoUpdateFailed() {
		return ioUpdateFailed;
	}

	@Override
	public WriteChannel<Boolean> setClearError() {
		return clearError;
	}

	@Override
	public WriteChannel<Boolean> setRestartModbus() {
		return restartModbus;
	}

	@Override
	public WriteChannel<Boolean> setRoundOFF() {
		return roundOFF;
	}

	@Override
	public WriteChannel<Boolean> setAutoRange() {
		return autoRange;
	}

	@Override
	public WriteChannel<Boolean> setFastResponse() {
		return fastResponse;
	}

	@Override
	public WriteChannel<Boolean> setTrackingFilter() {
		return trackingFilter;
	}

}
