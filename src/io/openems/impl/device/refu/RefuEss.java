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
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
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
	private ModbusReadChannel allowedApparent;
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
	public ModbusReadChannel batteryVoltage;
	public ModbusWriteChannel setSystemErrorReset;
	public ModbusWriteChannel setOperationMode;

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
				new ModbusRange(0x100, //
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
						new SignedDoublewordElement(0x107,
								batteryCurrent = new ModbusReadChannel("BatteryCurrent", this).unit("mA")
										.multiplier(2)),
						new SignedDoublewordElement(0x108,
								batteryVoltage = new ModbusReadChannel("BatteryVoltage", this).unit("mV")
										.multiplier(2)),
						new DummyElement(0x109, 0x10C),
						new SignedDoublewordElement(0x10D,
								activePower = new ModbusReadChannel("ActivePower", this).unit("W").multiplier(2)),
						new DummyElement(0x10E, 0x110), new SignedDoublewordElement(0x111,
								reactivePower = new ModbusReadChannel("ReactivePower", this).unit("Var").multiplier(2))

				), new WritableModbusRange(0x200, //
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
