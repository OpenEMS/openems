package io.openems.impl.device.refu;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.ModbusWriteChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.InputRegistersModbusRange;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.WritableModbusRange;

public class RefuEss extends ModbusDeviceNature implements SymmetricEssNature, ChannelUpdateListener {

	public RefuEss(String thingId) throws ConfigException {
		super(thingId);
	}

	/**
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this, Integer.class)
			.updateListener(this);

	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this, Integer.class).optional();

	@Override public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadChannel soc;
	private ModbusReadChannel allowedCharge;
	private ModbusReadChannel allowedDischarge;
	private StaticValueChannel<Long> allowedApparent = new StaticValueChannel<>("allowedApparent", this, 96600L)
			.unit("VA").unit("VA");
	private ModbusReadChannel apparentPower;
	private StaticValueChannel<Long> gridMode = new StaticValueChannel<Long>("GridMode", this, 1L).label(1L, ON_GRID);
	private ModbusReadChannel activePower;
	private ModbusReadChannel reactivePower;
	private ModbusReadChannel systemState;
	private ModbusWriteChannel setActivePower;
	private ModbusWriteChannel setReactivePower;
	private ModbusWriteChannel setWorkState;
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 96600L)
			.unit("VA").unit("VA");
	public StatusBitChannels warning;

	/*
	 * This Channels
	 */
	public StatusBitChannel communicationInformations;
	public ModbusReadChannel inverterStatus;
	public ModbusReadChannel errorCode;
	public StatusBitChannel dcDcStatus;
	public ModbusReadChannel dcDcError;
	public ModbusReadChannel batteryCurrent;
	public ModbusReadChannel batteryCurrentPcs;
	public ModbusReadChannel batteryVoltage;
	public ModbusReadChannel batteryVoltagePcs;
	public ModbusReadChannel batteryPower;
	public ModbusWriteChannel setSystemErrorReset;
	public ModbusWriteChannel setOperationMode;
	public ModbusReadChannel batteryState;
	public ModbusReadChannel batteryMode;
	public ModbusReadChannel allowedChargeCurrent;
	public ModbusReadChannel allowedDischargeCurrent;
	public ModbusReadChannel batteryChargeEnergy;
	public ModbusReadChannel batteryDischargeEnergy;
	public StatusBitChannel batteryOperationStatus;
	public ModbusReadChannel batteryHighestVoltage;
	public ModbusReadChannel batteryLowestVoltage;
	public ModbusReadChannel batteryHighestTemperature;
	public ModbusReadChannel batteryLowestTemperature;
	public ModbusReadChannel cosPhi3p;
	public ModbusReadChannel cosPhiL1;
	public ModbusReadChannel cosPhiL2;
	public ModbusReadChannel cosPhiL3;
	public ModbusReadChannel current;
	public ModbusReadChannel currentL1;
	public ModbusReadChannel currentL2;
	public ModbusReadChannel currentL3;
	public ModbusReadChannel activePowerL1;
	public ModbusReadChannel activePowerL2;
	public ModbusReadChannel activePowerL3;
	public ModbusReadChannel reactivePowerL1;
	public ModbusReadChannel reactivePowerL2;
	public ModbusReadChannel reactivePowerL3;
	public ModbusReadChannel maxAcPower;

	@Override public void channelUpdated(Channel channel, Optional<?> newValue) {
		// If chargeSoc was not set -> set it to minSoc minus 2
		if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
			chargeSoc.updateValue((Integer) newValue.get() - 2, false);
		}
	}

	@Override public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	@Override public ReadChannel<Long> gridMode() {
		return gridMode;
	}

	@Override public ReadChannel<Long> soc() {
		return soc;
	}

	@Override public ReadChannel<Long> systemState() {
		return systemState;
	}

	@Override public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

	@Override public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	@Override public StatusBitChannels warning() {
		return warning;
	}

	@Override public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

	@Override public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	@Override public WriteChannel<Long> setActivePower() {
		return setActivePower;
	}

	@Override public WriteChannel<Long> setReactivePower() {
		return setReactivePower;
	}

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		warning = new StatusBitChannels("Warning", this);
		return new ModbusProtocol( //
				new InputRegistersModbusRange(0x100, //
						new UnsignedWordElement(0x100, //
								systemState = new ModbusReadChannel("SystemState", this) //
										.label(0, STOP) //
										.label(1, "Init") //
										.label(2, "Pre-operation") //
										.label(3, STANDBY) //
										.label(4, START) //
										.label(5, "Error")),
						new UnsignedWordElement(0x101,
								warning.channel(new StatusBitChannel("SystemError1", this).label(1, "BMS In Error")//
										.label(2, "BMS Overvoltage")//
										.label(4, "BMS Undervoltage")//
										.label(8, "BMS Overcurrent"))),
						new UnsignedWordElement(0x102,
								communicationInformations = new StatusBitChannel("CommunicationInformations", this)//
										.label(1, "Gateway Initialized")//
										.label(2, "Modbus Slave Status")//
										.label(4, "Modbus Master Status")//
										.label(8, "CAN Timeout")),
						new UnsignedWordElement(0x103,
								inverterStatus = new StatusBitChannel("InverterStatus", this)//
										.label(1, "Ready to Power on")//
										.label(2, "Ready for Operating")),
						new UnsignedWordElement(0x104, errorCode = new ModbusReadChannel("ErrorCode", this)),
						new UnsignedWordElement(0x105,
								dcDcStatus = new StatusBitChannel("DCDCStatus", this)//
										.label(0, "CurrentControl")//
										.label(1, "AC Power Control")//
										.label(2, "VoltageControl").label(3, "AC Power Control")),
						new UnsignedWordElement(0x106, dcDcError = new ModbusReadChannel("DCDCError", this)),
						new SignedWordElement(0x107,
								batteryCurrentPcs = new ModbusReadChannel("BatteryCurrentPcs", this).unit("mA")
										.multiplier(2)),
						new SignedWordElement(0x108,
								batteryVoltagePcs = new ModbusReadChannel("BatteryVoltagePcs", this).unit("mV")
										.multiplier(2)),
						new SignedWordElement(0x109,
								current = new ModbusReadChannel("Current", this).unit("mA").multiplier(2)),
						new SignedWordElement(0x10A,
								currentL1 = new ModbusReadChannel("CurrentL1", this).unit("mA").multiplier(2)),
						new SignedWordElement(0x10B,
								currentL2 = new ModbusReadChannel("CurrentL2", this).unit("mA").multiplier(2)),
						new SignedWordElement(0x10C,
								currentL3 = new ModbusReadChannel("CurrentL3", this).unit("mA").multiplier(2)),
						new SignedWordElement(0x10D,
								activePower = new ModbusReadChannel("ActivePower", this).unit("W").multiplier(2)),
						new SignedWordElement(0x10E,
								activePowerL1 = new ModbusReadChannel("ActivePowerL1", this).unit("W").multiplier(2)),
						new SignedWordElement(0x10F,
								activePowerL2 = new ModbusReadChannel("ActivePowerL2", this).unit("W").multiplier(2)),
						new SignedWordElement(0x110,
								activePowerL3 = new ModbusReadChannel("ActivePowerL3", this).unit("W").multiplier(2)),
						new SignedWordElement(0x111,
								reactivePower = new ModbusReadChannel("ReactivePower", this).unit("Var").multiplier(2)),
						new SignedWordElement(0x112,
								reactivePowerL1 = new ModbusReadChannel("ReactivePowerL1", this).unit("Var")
										.multiplier(2)),
						new SignedWordElement(0x113,
								reactivePowerL2 = new ModbusReadChannel("ReactivePowerL2", this).unit("Var")
										.multiplier(2)),
						new SignedWordElement(0x114,
								reactivePowerL3 = new ModbusReadChannel("ReactivePowerL3", this).unit("Var")
										.multiplier(2)),
						new SignedWordElement(0x115, cosPhi3p = new ModbusReadChannel("CosPhi3p", this).unit("")),
						new SignedWordElement(0x116, cosPhiL1 = new ModbusReadChannel("CosPhiL1", this).unit("")),
						new SignedWordElement(0x117, cosPhiL2 = new ModbusReadChannel("CosPhiL2", this).unit("")),
						new SignedWordElement(0x118, cosPhiL3 = new ModbusReadChannel("CosPhiL3", this).unit("")),
						new SignedWordElement(0x119, maxAcPower = new ModbusReadChannel("MaxAcPower", this).unit(""))),
				new InputRegistersModbusRange(0x11A, //
						new UnsignedWordElement(0x11A, //
								batteryState = new ModbusReadChannel("BatteryState", this)//
										.label(0, "Initial")//
										.label(1, STOP)//
										.label(2, "Starting")//
										.label(3, START)//
										.label(4, "Stopping")//
										.label(5, "Fault")), //
						new UnsignedWordElement(0x11B, //
								batteryMode = new ModbusReadChannel("BatteryMode", this).label(0, "Normal Mode")),
						new SignedWordElement(0x11C,
								batteryVoltage = new ModbusReadChannel("BatteryVoltage", this).unit("mV")
										.multiplier(2)),
						new SignedWordElement(0x11D,
								batteryCurrent = new ModbusReadChannel("BatteryCurrent", this).unit("mA")
										.multiplier(2)),
						new SignedWordElement(0x11E, //
								batteryPower = new ModbusReadChannel("BatteryPower", this).unit("W")//
										.multiplier(2)),
						new UnsignedWordElement(0x11F, //
								soc = new ModbusReadChannel("Soc", this).unit("%")),
						new UnsignedWordElement(0x120, //
								allowedChargeCurrent = new ModbusReadChannel("AllowedChargeCurrent", this).unit("mA")//
										.multiplier(2)//
										.negate()),
						new UnsignedWordElement(0x121, //
								allowedDischargeCurrent = new ModbusReadChannel("AllowedDischargeCurrent", this)
										.unit("mA").multiplier(2)),
						new UnsignedWordElement(0x122, //
								allowedCharge = new ModbusReadChannel("AllowedCharge", this).unit("W").multiplier(2)
										.negate()),
						new UnsignedWordElement(0x123, //
								allowedDischarge = new ModbusReadChannel("AllowedDischarge", this).unit("W")
										.multiplier(2)),
						new SignedDoublewordElement(0x124, //
								batteryChargeEnergy = new ModbusReadChannel("BatteryChargeEnergy", this).unit("kWh"))
										.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(0x126, //
								batteryDischargeEnergy = new ModbusReadChannel("BatteryDischargeEnergy", this)
										.unit("kWh")).wordorder(WordOrder.LSWMSW),
						new UnsignedWordElement(0x128, //
								batteryOperationStatus = new StatusBitChannel("BatteryOperationStatus", this)
										.label(1, "Battery group 1 operating")//
										.label(2, "Battery group 2 operating")//
										.label(4, "Battery group 3 operating")//
										.label(8, "Battery group 4 operating")),
						new UnsignedWordElement(0x129, //
								batteryHighestVoltage = new ModbusReadChannel("BatteryHighestVoltage", this)
										.unit("mV")),
						new UnsignedWordElement(0x12A, //
								batteryLowestVoltage = new ModbusReadChannel("BatteryLowestVoltage", this).unit("mV")),
						new SignedWordElement(0x12B, //
								batteryHighestTemperature = new ModbusReadChannel("BatteryHighestTemperature", this)
										.unit("°C")),
						new SignedWordElement(0x12C, //
								batteryLowestTemperature = new ModbusReadChannel("BatteryLowestTemperature", this)
										.unit("°C")),
						new DummyElement(0x12D),
						new UnsignedWordElement(0x12E,
								warning.channel(new StatusBitChannel("BatteryAlarm1", this)//
										.label(1, "Normal charging over-current ")//
										.label(2, "Charginig current over limit")//
										.label(4, "Discharging current over limit")//
										.label(8, "Normal high voltage")//
										.label(16, "Normal low voltage")//
										.label(32, "Abnormal voltage variation")//
										.label(64, "Normal high temperature")//
										.label(128, "Normal low temperature")//
										.label(256, "Abnormal temperature variation")//
										.label(512, "Serious high voltage")//
										.label(1024, "Serious low voltage")//
										.label(2048, "Serious low temperature")//
										.label(4096, "Charging serious over current")//
										.label(8192, "Discharging serious over current")//
										.label(16384, "Abnormal capacity alarm"))),
						new UnsignedWordElement(0x12F,
								warning.channel(new StatusBitChannel("BatteryAlarm2", this)//
										.label(1, "EEPROM parameter failure")//
										.label(2, "Switch off inside combined cabinet")//
										.label(32, "Should not be connected to grid due to the DC side condition")//
										.label(128, "Emergency stop require from system controller"))),
						new UnsignedWordElement(0x130,
								warning.channel(new StatusBitChannel("BatteryAlarm3", this)//
										.label(1, "Battery group 1 enable and not connected to grid")//
										.label(2, "Battery group 2 enable and not connected to grid")//
										.label(4, "Battery group 3 enable and not connected to grid")//
										.label(8, "Battery group 4 enable and not connected to grid"))),
						new UnsignedWordElement(0x131,
								warning.channel(new StatusBitChannel("BatteryAlarm4", this)//
										.label(1, "The isolation switch of battery group 1 open")//
										.label(2, "The isolation switch of battery group 2 open")//
										.label(4, "The isolation switch of battery group 3 open")//
										.label(8, "The isolation switch of battery group 4 open"))),
						new DummyElement(0x132),
						new UnsignedWordElement(0x133,
								warning.channel(new StatusBitChannel("BatteryAlarm6", this)//
										.label(1, "Balancing sampling failure of battery group 1")//
										.label(2, "Balancing sampling failure of battery group 2")//
										.label(4, "Balancing sampling failure of battery group 3")//
										.label(8, "Balancing sampling failure of battery group 4"))),
						new UnsignedWordElement(0x134,
								warning.channel(new StatusBitChannel("BatteryAlarm7", this)//
										.label(1, "Balancing control failure of battery group 1")//
										.label(2, "Balancing control failure of battery group 2")//
										.label(4, "Balancing control failure of battery group 3")//
										.label(8, "Balancing control failure of battery group 4"))),
						new UnsignedWordElement(0x135, warning.channel(new StatusBitChannel("BatteryFault1", this)//
								.label(1, "No enable batery group or usable battery group")//
								.label(2, "Normal leakage of battery group")//
								.label(4, "Serious leakage of battery group")//
								.label(8, "Battery start failure")//
								.label(16, "Battery stop failure")//
								.label(32, "Interruption of CAN Communication between battery group and controller")//
								.label(1024, "Emergency stop abnormal of auxiliary collector")//
								.label(2048, "Leakage self detection on negative")//
								.label(4096, "Leakage self detection on positive")//
								.label(8192, "Self detection failure on battery"))),
						new UnsignedWordElement(0x136,
								warning.channel(new StatusBitChannel("BatteryFault2", this)//
										.label(1, "CAN Communication interruption between battery group and group 1")//
										.label(2, "CAN Communication interruption between battery group and group 2")//
										.label(4, "CAN Communication interruption between battery group and group 3")//
										.label(8, "CAN Communication interruption between battery group and group 4"))),
						new UnsignedWordElement(0x137,
								warning.channel(new StatusBitChannel("BatteryFault3", this)//
										.label(1, "Main contractor abnormal in battery self detect group 1")//
										.label(2, "Main contractor abnormal in battery self detect group 2")//
										.label(4, "Main contractor abnormal in battery self detect group 3")//
										.label(8, "Main contractor abnormal in battery self detect group 4"))),
						new UnsignedWordElement(0x138,
								warning.channel(new StatusBitChannel("BatteryFault4", this)//
										.label(1, "Pre-charge contractor abnormal on battery self detect group 1")//
										.label(2, "Pre-charge contractor abnormal on battery self detect group 2")//
										.label(4, "Pre-charge contractor abnormal on battery self detect group 3")//
										.label(8, "Pre-charge contractor abnormal on battery self detect group 4"))),
						new UnsignedWordElement(0x139,
								warning.channel(new StatusBitChannel("BatteryFault5", this)//
										.label(1, "Main contact failure on battery control group 1")//
										.label(2, "Main contact failure on battery control group 2")//
										.label(4, "Main contact failure on battery control group 3")//
										.label(8, "Main contact failure on battery control group 4"))),
						new UnsignedWordElement(0x13A,
								warning.channel(new StatusBitChannel("BatteryFault6", this)//
										.label(1, "Pre-charge failure on battery control group 1")//
										.label(2, "Pre-charge failure on battery control group 2")//
										.label(4, "Pre-charge failure on battery control group 3")//
										.label(8, "Pre-charge failure on battery control group 4"))),
						new UnsignedWordElement(0x13B,
								warning.channel(new StatusBitChannel("BatteryFault7", this)//
										.label(4, "Sampling circuit abnormal for BMU")//
										.label(8, "Power cable disconnect failure")//
										.label(16, "Sampling circuit disconnect failure")//
										.label(64, "CAN disconnect for master and slave")//
										.label(512, "Sammpling circuit failure")//
										.label(1024, "Single battery failure")//
										.label(2048, "Circuit detection abnormal for main contactor")//
										.label(4096, "Circuit detection abnormal for main contactor")//
										.label(8192, "Circuit detection abnormal for Fancontactor")//
										.label(16384, "BMUPower contactor circuit detection abnormal")//
										.label(32768, "Central contactor circuit detection abnormal"))),
						new UnsignedWordElement(0x13C,
								warning.channel(new StatusBitChannel("BatteryFault8", this)//
										.label(4, "Serious temperature fault")//
										.label(8, "Communication fault for system controller")//
										.label(128, "Frog alarm")//
										.label(256, "Fuse fault")//
										.label(1024, "Normal leakage")//
										.label(2048, "Serious leakage")//
										.label(4096, "CAN disconnection between battery group and battery stack")//
										.label(8192, "Central contactor circuit open")//
										.label(16384, "BMU power contactor open")))),
				new WritableModbusRange(0x200, //
						new UnsignedWordElement(0x200, //
								setWorkState = new ModbusWriteChannel("SetWorkState", this) //
										.label(0, STOP) //
										.label(1, START)),
						new UnsignedWordElement(0x201, //
								setSystemErrorReset = new ModbusWriteChannel("SetSystemErrorReset", this)//
										.label(0, OFF)//
										.label(1, ON)),
						new UnsignedWordElement(0x202, //
								setOperationMode = new ModbusWriteChannel("SetOperationMode", this)//
										.label(0, "P/Q Set point")//
										.label(1, "IAC / cosphi set point")),
						new SignedWordElement(0x203, //
								setActivePower = new ModbusWriteChannel("SetActivePower", this)//
										.unit("W").multiplier(2)),
						new DummyElement(0x204, 0x206),
						new SignedWordElement(0x207, //
								setReactivePower = new ModbusWriteChannel("SetReactivePower", this)//
										.unit("W").multiplier(2))));
	}

}
