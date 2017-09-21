package io.openems.impl.device.custom.riedmann;

import io.openems.api.device.Device;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRegisterRange;

@ThingInfo(title = "Custom: Riedmann PLC")
public class RiedmannNatureImpl extends ModbusDeviceNature implements RiedmannNature {

	private ModbusReadChannel<Long> waterlevel;
	private ModbusReadChannel<Long> getPivotOn;
	private ModbusReadChannel<Long> getBorehole1On;
	private ModbusReadChannel<Long> getBorehole2On;
	private ModbusReadChannel<Long> getBorehole3On;
	private ModbusReadChannel<Long> getClima1On;
	private ModbusReadChannel<Long> getClima2On;
	private ModbusReadChannel<Long> getOfficeOn;
	private ModbusReadChannel<Long> getTraineeCenterOn;
	private ModbusReadChannel<Long> automaticMode;
	private ModbusReadChannel<Long> manualMode;
	private ModbusReadChannel<Long> emergencyStop;
	private ModbusReadChannel<Long> switchStatePivotPump;
	private ModbusReadChannel<Long> switchStatePivotDrive;
	private ModbusReadChannel<Long> error;
	private ModbusReadChannel<Long> getWaterLevelBorehole1On;
	private ModbusReadChannel<Long> getWaterLevelBorehole1Off;
	private ModbusReadChannel<Long> getWaterLevelBorehole2On;
	private ModbusReadChannel<Long> getWaterLevelBorehole2Off;
	private ModbusReadChannel<Long> getWaterLevelBorehole3On;
	private ModbusReadChannel<Long> getWaterLevelBorehole3Off;
	private ModbusWriteChannel<Long> setPivotOn;
	private ModbusWriteChannel<Long> setBorehole1On;
	private ModbusWriteChannel<Long> setBorehole2On;
	private ModbusWriteChannel<Long> setBorehole3On;
	private ModbusWriteChannel<Long> setClima1On;
	private ModbusWriteChannel<Long> setClima2On;
	private ModbusWriteChannel<Long> setOfficeOn;
	private ModbusWriteChannel<Long> setTraineeCenterOn;
	private ModbusWriteChannel<Long> signalBus1On;
	private ModbusWriteChannel<Long> signalBus2On;
	private ModbusWriteChannel<Long> signalGridOn;
	private ModbusWriteChannel<Long> signalSystemStop;
	private ModbusWriteChannel<Long> signalWatchdog;
	private ModbusWriteChannel<Long> setWaterLevelBorehole1On;
	private ModbusWriteChannel<Long> setWaterLevelBorehole1Off;
	private ModbusWriteChannel<Long> setWaterLevelBorehole2On;
	private ModbusWriteChannel<Long> setWaterLevelBorehole2Off;
	private ModbusWriteChannel<Long> setWaterLevelBorehole3On;
	private ModbusWriteChannel<Long> setWaterLevelBorehole3Off;

	@Override
	public ModbusReadChannel<Long> getWaterlevel() {
		return waterlevel;
	}

	@Override
	public ModbusReadChannel<Long> getGetPivotOn() {
		return getPivotOn;
	}

	@Override
	public ModbusReadChannel<Long> getBorehole1On() {
		return getBorehole1On;
	}

	@Override
	public ModbusReadChannel<Long> getBorehole2On() {
		return getBorehole2On;
	}

	@Override
	public ModbusReadChannel<Long> getBorehole3On() {
		return getBorehole3On;
	}

	@Override
	public ModbusReadChannel<Long> getClima1On() {
		return getClima1On;
	}

	@Override
	public ModbusReadChannel<Long> getClima2On() {
		return getClima2On;
	}

	@Override
	public ModbusReadChannel<Long> getOfficeOn() {
		return getOfficeOn;
	}

	@Override
	public ModbusReadChannel<Long> getTraineeCenterOn() {
		return getTraineeCenterOn;
	}

	@Override
	public ModbusReadChannel<Long> getAutomaticMode() {
		return automaticMode;
	}

	@Override
	public ModbusReadChannel<Long> getManualMode() {
		return manualMode;
	}

	@Override
	public ModbusReadChannel<Long> getEmergencyStop() {
		return emergencyStop;
	}

	@Override
	public ModbusReadChannel<Long> getSwitchStatePivotPump() {
		return switchStatePivotPump;
	}

	@Override
	public ModbusReadChannel<Long> getSwitchStatePivotDrive() {
		return switchStatePivotDrive;
	}

	@Override
	public ModbusReadChannel<Long> getError() {
		return error;
	}

	@Override
	public ModbusReadChannel<Long> getWaterLevelBorehole1On() {
		return getWaterLevelBorehole1On;
	}

	@Override
	public ModbusReadChannel<Long> getWaterLevelBorehole1Off() {
		return getWaterLevelBorehole1Off;
	}

	@Override
	public ModbusReadChannel<Long> getWaterLevelBorehole2On() {
		return getWaterLevelBorehole2On;
	}

	@Override
	public ModbusReadChannel<Long> getWaterLevelBorehole2Off() {
		return getWaterLevelBorehole2Off;
	}

	@Override
	public ModbusReadChannel<Long> getWaterLevelBorehole3On() {
		return getWaterLevelBorehole3On;
	}

	@Override
	public ModbusReadChannel<Long> getWaterLevelBorehole3Off() {
		return getWaterLevelBorehole3Off;
	}

	@Override
	public ModbusWriteChannel<Long> getSetPivotOn() {
		return setPivotOn;
	}

	@Override
	public ModbusWriteChannel<Long> getSetBorehole1On() {
		return setBorehole1On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetBorehole2On() {
		return setBorehole2On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetBorehole3On() {
		return setBorehole3On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetClima1On() {
		return setClima1On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetClima2On() {
		return setClima2On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetOfficeOn() {
		return setOfficeOn;
	}

	@Override
	public ModbusWriteChannel<Long> getSetTraineeCenterOn() {
		return setTraineeCenterOn;
	}

	@Override
	public ModbusWriteChannel<Long> getSignalBus1On() {
		return signalBus1On;
	}

	@Override
	public ModbusWriteChannel<Long> getSignalBus2On() {
		return signalBus2On;
	}

	@Override
	public ModbusWriteChannel<Long> getSignalGridOn() {
		return signalGridOn;
	}

	@Override
	public ModbusWriteChannel<Long> getSignalSystemStop() {
		return signalSystemStop;
	}

	@Override
	public ModbusWriteChannel<Long> getSignalWatchdog() {
		return signalWatchdog;
	}

	@Override
	public ModbusWriteChannel<Long> getSetWaterLevelBorehole1On() {
		return setWaterLevelBorehole1On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetWaterLevelBorehole1Off() {
		return setWaterLevelBorehole1Off;
	}

	@Override
	public ModbusWriteChannel<Long> getSetWaterLevelBorehole2On() {
		return setWaterLevelBorehole2On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetWaterLevelBorehole2Off() {
		return setWaterLevelBorehole2Off;
	}

	@Override
	public ModbusWriteChannel<Long> getSetWaterLevelBorehole3On() {
		return setWaterLevelBorehole3On;
	}

	@Override
	public ModbusWriteChannel<Long> getSetWaterLevelBorehole3Off() {
		return setWaterLevelBorehole3Off;
	}

	public RiedmannNatureImpl(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new WriteableModbusRegisterRange(0,
						new SignedWordElement(0, setPivotOn = new ModbusWriteLongChannel("SetPivotOn", this)),
						new SignedWordElement(1, setBorehole1On = new ModbusWriteLongChannel("SetBorehole1On", this)),
						new SignedWordElement(2, setBorehole2On = new ModbusWriteLongChannel("SetBorehole2On", this)),
						new SignedWordElement(3, setBorehole3On = new ModbusWriteLongChannel("SetBorehole3On", this)),
						new SignedWordElement(4, setClima1On = new ModbusWriteLongChannel("SetClima1On", this)),
						new SignedWordElement(5, setClima2On = new ModbusWriteLongChannel("SetClima2On", this)),
						new SignedWordElement(6, setOfficeOn = new ModbusWriteLongChannel("SetOfficeOn", this)),
						new SignedWordElement(7,
								setTraineeCenterOn = new ModbusWriteLongChannel("SetTraineeCenterOn", this)),
						new SignedWordElement(8, signalBus1On = new ModbusWriteLongChannel("SignalBus1On", this)),
						new SignedWordElement(9, signalBus2On = new ModbusWriteLongChannel("SignalBus2On", this)),
						new SignedWordElement(10, signalGridOn = new ModbusWriteLongChannel("SignalGridOn", this)),
						new SignedWordElement(11,
								signalSystemStop = new ModbusWriteLongChannel("SignalSystemStop", this)),
						new SignedWordElement(12, signalWatchdog = new ModbusWriteLongChannel("SignalWatchdog", this))),
				new WriteableModbusRegisterRange(20, new SignedWordElement(20,
						setWaterLevelBorehole1On = new ModbusWriteLongChannel("SetWaterLevelBorehole1On", this)),
						new SignedWordElement(21,
								setWaterLevelBorehole1Off = new ModbusWriteLongChannel("SetWaterLevelBorehole1Off",
										this)),
						new SignedWordElement(22,
								setWaterLevelBorehole2On = new ModbusWriteLongChannel("SetWaterLevelBorehole2On",
										this)),
						new SignedWordElement(23,
								setWaterLevelBorehole2Off = new ModbusWriteLongChannel("SetWaterLevelBorehole2Off",
										this)),
						new SignedWordElement(24,
								setWaterLevelBorehole3On = new ModbusWriteLongChannel("SetWaterLevelBorehole3On",
										this)),
						new SignedWordElement(25, setWaterLevelBorehole3Off = new ModbusWriteLongChannel(
								"SetWaterLevelBorehole3Off", this))),
				new ModbusRegisterRange(50, //
						new SignedWordElement(50, //
								waterlevel = new ModbusReadLongChannel("WaterLevel", this).unit("cm")), //
						new SignedWordElement(51, //
								getPivotOn = new ModbusReadLongChannel("GetPivotOn", this)), //
						new SignedWordElement(52, //
								getBorehole1On = new ModbusReadLongChannel("GetBorehole1On", this)), //
						new SignedWordElement(53, //
								getBorehole2On = new ModbusReadLongChannel("GetBorehole2On", this)), //
						new SignedWordElement(54, //
								getBorehole3On = new ModbusReadLongChannel("GetBorehole3On", this)), //
						new SignedWordElement(55, //
								getClima1On = new ModbusReadLongChannel("GetClima1On", this)), //
						new SignedWordElement(56, //
								getClima2On = new ModbusReadLongChannel("GetClima2On", this)), //
						new SignedWordElement(57, //
								getOfficeOn = new ModbusReadLongChannel("GetOfficeOn", this)), //
						new SignedWordElement(58, //
								getTraineeCenterOn = new ModbusReadLongChannel("GetTraineeCentereOn", this)), //
						new SignedWordElement(59, //
								automaticMode = new ModbusReadLongChannel("AutomaticMode", this)), //
						new SignedWordElement(60, //
								manualMode = new ModbusReadLongChannel("ManualMode", this)), //
						new SignedWordElement(61, //
								emergencyStop = new ModbusReadLongChannel("EmergencyStop", this)), //
						new SignedWordElement(62, //
								switchStatePivotPump = new ModbusReadLongChannel("SwitchStatePivotPump", this)), //
						new SignedWordElement(63, //
								switchStatePivotDrive = new ModbusReadLongChannel("SwitchStatePivotDrive", this)), //
						new SignedWordElement(64, //
								error = new ModbusReadLongChannel("error", this)), //
						new DummyElement(65, 69), new SignedWordElement(70, //
								getWaterLevelBorehole1On = new ModbusReadLongChannel("GetWaterLevelBorehole1On", this)), //
						new SignedWordElement(71, //
								getWaterLevelBorehole1Off = new ModbusReadLongChannel("GetWaterLevelBorehole1Off",
										this)), //
						new SignedWordElement(72, //
								getWaterLevelBorehole2On = new ModbusReadLongChannel("GetWaterLevelBorehole2On", this)), //
						new SignedWordElement(73, //
								getWaterLevelBorehole2Off = new ModbusReadLongChannel("GetWaterLevelBorehole2Off",
										this)), //
						new SignedWordElement(74, //
								getWaterLevelBorehole3On = new ModbusReadLongChannel("GetWaterLevelBorehole3On", this)), //
						new SignedWordElement(75, //
								getWaterLevelBorehole3Off = new ModbusReadLongChannel("GetWaterLevelBorehole3Off",
										this)) //
				));
	}

}
