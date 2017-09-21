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
package io.openems.impl.device.refu;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.range.ModbusInputRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRegisterRange;

@ThingInfo(title = "REFU battery inverter ESS")
public class RefuEss extends ModbusDeviceNature implements SymmetricEssNature, AsymmetricEssNature {

	/*
	 * Constructors
	 */
	public RefuEss(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		minSoc.addUpdateListener((channel, newValue) -> {
			// If chargeSoc was not set -> set it to minSoc minus 2
			if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
				chargeSoc.updateValue((Integer) newValue.get() - 2, false);
			}
		});
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);

	@Override
	public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override
	public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadLongChannel soc;
	private ModbusReadLongChannel allowedCharge;
	private ModbusReadLongChannel allowedDischarge;
	private StaticValueChannel<Long> allowedApparent = new StaticValueChannel<>("allowedApparent", this, 100000L)
			.unit("VA").unit("VA");
	private ModbusReadLongChannel apparentPower;
	private StaticValueChannel<Long> gridMode = new StaticValueChannel<Long>("GridMode", this, 1L).label(1L, ON_GRID);
	private ModbusReadLongChannel activePower;
	private ModbusReadLongChannel reactivePower;
	private ModbusReadLongChannel systemState;
	private ModbusWriteLongChannel setActivePower;
	private ModbusWriteLongChannel setActivePowerL1;
	private ModbusWriteLongChannel setActivePowerL2;
	private ModbusWriteLongChannel setActivePowerL3;
	private ModbusWriteLongChannel setReactivePower;
	private ModbusWriteLongChannel setReactivePowerL1;
	private ModbusWriteLongChannel setReactivePowerL2;
	private ModbusWriteLongChannel setReactivePowerL3;
	private ModbusWriteLongChannel setWorkState;
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 100000L)
			.unit("VA").unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 130000L).unit("Wh");
	public StatusBitChannels warning;

	/*
	 * This Channels
	 */
	public StatusBitChannel communicationInformations;
	public StatusBitChannel systemError1;
	public StatusBitChannel inverterStatus;
	public ModbusReadLongChannel errorCode;
	public StatusBitChannel dcDcStatus;
	public ModbusReadLongChannel dcDcError;
	public ModbusReadLongChannel batteryCurrent;
	public ModbusReadLongChannel batteryCurrentPcs;
	public ModbusReadLongChannel batteryVoltage;
	public ModbusReadLongChannel batteryVoltagePcs;
	public ModbusReadLongChannel batteryPower;
	public ModbusWriteLongChannel setSystemErrorReset;
	public ModbusWriteLongChannel setOperationMode;
	public ModbusReadLongChannel batteryState;
	public ModbusReadLongChannel batteryMode;
	public ModbusReadLongChannel allowedChargeCurrent;
	public ModbusReadLongChannel allowedDischargeCurrent;
	public ModbusReadLongChannel batteryChargeEnergy;
	public ModbusReadLongChannel batteryDischargeEnergy;
	public ModbusReadLongChannel pcsAllowedCharge;
	public ModbusReadLongChannel pcsAllowedDischarge;
	public StatusBitChannel batteryOperationStatus;
	public ModbusReadLongChannel batteryHighestVoltage;
	public ModbusReadLongChannel batteryLowestVoltage;
	public ModbusReadLongChannel batteryHighestTemperature;
	public ModbusReadLongChannel batteryLowestTemperature;
	public ModbusReadLongChannel batteryStopRequest;
	public ModbusReadLongChannel cosPhi3p;
	public ModbusReadLongChannel cosPhiL1;
	public ModbusReadLongChannel cosPhiL2;
	public ModbusReadLongChannel cosPhiL3;
	public ModbusReadLongChannel current;
	public ModbusReadLongChannel currentL1;
	public ModbusReadLongChannel currentL2;
	public ModbusReadLongChannel currentL3;
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel reactivePowerL1;
	private ModbusReadLongChannel reactivePowerL2;
	private ModbusReadLongChannel reactivePowerL3;
	public StatusBitChannel batteryFault1;
	public StatusBitChannel batteryFault2;
	public StatusBitChannel batteryFault3;
	public StatusBitChannel batteryFault4;
	public StatusBitChannel batteryFault5;
	public StatusBitChannel batteryFault6;
	public StatusBitChannel batteryFault7;
	public StatusBitChannel batteryFault8;
	public StatusBitChannel batteryFault9;
	public StatusBitChannel batteryFault10;
	public StatusBitChannel batteryFault11;
	public StatusBitChannel batteryFault12;
	public StatusBitChannel batteryFault13;
	public StatusBitChannel batteryFault14;
	public StatusBitChannel batteryAlarm1;
	public StatusBitChannel batteryAlarm2;
	public StatusBitChannel batteryAlarm3;
	public StatusBitChannel batteryAlarm4;
	// public StatusBitChannel batteryAlarm5;
	public StatusBitChannel batteryAlarm6;
	public StatusBitChannel batteryAlarm7;
	public StatusBitChannel batteryGroupControlStatus;
	public StatusBitChannel errorLog1;
	public StatusBitChannel errorLog2;
	public StatusBitChannel errorLog3;
	public StatusBitChannel errorLog4;
	public StatusBitChannel errorLog5;
	public StatusBitChannel errorLog6;
	public StatusBitChannel errorLog7;
	public StatusBitChannel errorLog8;
	public StatusBitChannel errorLog9;
	public StatusBitChannel errorLog10;
	public StatusBitChannel errorLog11;
	public StatusBitChannel errorLog12;
	public StatusBitChannel errorLog13;
	public StatusBitChannel errorLog14;
	public StatusBitChannel errorLog15;
	public StatusBitChannel errorLog16;

	@Override
	public ReadChannel<Long> gridMode() {
		return gridMode;
	}

	@Override
	public ReadChannel<Long> soc() {
		return soc;
	}

	@Override
	public ReadChannel<Long> systemState() {
		return systemState;
	}

	@Override
	public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override
	public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

	@Override
	public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	@Override
	public StatusBitChannels warning() {
		return warning;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

	@Override
	public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	@Override
	public WriteChannel<Long> setActivePower() {
		return setActivePower;
	}

	@Override
	public WriteChannel<Long> setReactivePower() {
		return setReactivePower;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		warning = new StatusBitChannels("Warning", this);
		return new ModbusProtocol( //
				new ModbusInputRegisterRange(0x100, //
						new UnsignedWordElement(0x100, //
								systemState = new ModbusReadLongChannel("SystemState", this) //
										.label(0, STOP) //
										.label(1, "Init") //
										.label(2, "Pre-operation") //
										.label(3, STANDBY) //
										.label(4, START) //
										.label(5, FAULT)),
						new UnsignedWordElement(0x101,
								systemError1 = warning.channel(new StatusBitChannel("SystemError1", this)//
										.label(1, "BMS In Error")//
										.label(2, "BMS Overvoltage")//
										.label(4, "BMS Undervoltage")//
										.label(8, "BMS Overcurrent")//
										.label(16, "Error BMS Limits not initialized")//
										.label(32, "Connect Error")//
										.label(64, "Overvoltage warning")//
										.label(128, "Undervoltage warning")//
										.label(256, "Overcurrent warning")//
										.label(512, "BMS Ready")//
										.label(1024, "TREX Ready")//
								)),
						new UnsignedWordElement(0x102,
								communicationInformations = new StatusBitChannel("CommunicationInformations", this)//
										.label(1, "Gateway Initialized")//
										.label(2, "Modbus Slave Status")//
										.label(4, "Modbus Master Status")//
										.label(8, "CAN Timeout")//
										.label(16, "First Communication Ok")//
						), new UnsignedWordElement(0x103, inverterStatus = new StatusBitChannel("InverterStatus", this)//
								.label(1, "Ready to Power on")//
								.label(2, "Ready for Operating")//
								.label(4, "Enabled")//
								.label(8, "Fault")//
								.label(256, "Warning")//
								.label(512, "Voltage/Current mode")//
								.label(1024, "Power mode")//
								.label(2048, "AC relays close")//
								.label(4096, "DC relays 1 close")//
								.label(8192, "DC relays 2 close")//
								.label(16384, "Mains OK")//
						), new UnsignedWordElement(0x104, errorCode = new ModbusReadLongChannel("ErrorCode", this)),
						new UnsignedWordElement(0x105, dcDcStatus = new StatusBitChannel("DCDCStatus", this)//
								.label(1, "Ready to Power on")//
								.label(2, "Ready for Operating")//
								.label(4, "Enabled")//
								.label(8, "DCDC Fault")//
								.label(128, "DCDC Warning")//
								.label(256, "Voltage/Current mode")//
								.label(512, "Power mode")//
						), new UnsignedWordElement(0x106, dcDcError = new ModbusReadLongChannel("DCDCError", this)),
						new SignedWordElement(0x107,
								batteryCurrentPcs = new ModbusReadLongChannel("BatteryCurrentPcs", this).unit("mA")
										.multiplier(2)), //
						new SignedWordElement(0x108,
								batteryVoltagePcs = new ModbusReadLongChannel("BatteryVoltagePcs", this).unit("mV")
										.multiplier(2)), //
						new SignedWordElement(0x109,
								current = new ModbusReadLongChannel("Current", this).unit("mA").multiplier(2)), //
						new SignedWordElement(0x10A,
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA").multiplier(2)), //
						new SignedWordElement(0x10B,
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA").multiplier(2)), //
						new SignedWordElement(0x10C,
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA").multiplier(2)), //
						new SignedWordElement(0x10D,
								activePower = new ModbusReadLongChannel("ActivePower", this).unit("W").multiplier(2)), //
						new SignedWordElement(0x10E,
								activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
										.multiplier(2)), //
						new SignedWordElement(0x10F,
								activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
										.multiplier(2)), //
						new SignedWordElement(0x110,
								activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
										.multiplier(2)), //
						new SignedWordElement(0x111,
								reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("Var")
										.multiplier(2)), //
						new SignedWordElement(0x112,
								reactivePowerL1 = new ModbusReadLongChannel("ReactivePowerL1", this).unit("Var")
										.multiplier(2)), //
						new SignedWordElement(0x113,
								reactivePowerL2 = new ModbusReadLongChannel("ReactivePowerL2", this).unit("Var")
										.multiplier(2)), //
						new SignedWordElement(0x114,
								reactivePowerL3 = new ModbusReadLongChannel("ReactivePowerL3", this).unit("Var")
										.multiplier(2)), //
						new SignedWordElement(0x115, cosPhi3p = new ModbusReadLongChannel("CosPhi3p", this).unit("")), //
						new SignedWordElement(0x116, cosPhiL1 = new ModbusReadLongChannel("CosPhiL1", this).unit("")), //
						new SignedWordElement(0x117, cosPhiL2 = new ModbusReadLongChannel("CosPhiL2", this).unit("")), //
						new SignedWordElement(0x118, cosPhiL3 = new ModbusReadLongChannel("CosPhiL3", this).unit(""))), //
				new ModbusInputRegisterRange(0x11A, //
						new SignedWordElement(0x11A,
								pcsAllowedCharge = new ModbusReadLongChannel("PcsAllowedCharge", this).unit("kW")
										.multiplier(2)),
						new SignedWordElement(0x11B,
								pcsAllowedDischarge = new ModbusReadLongChannel("PcsAllowedDischarge", this).unit("kW")
										.multiplier(2)),
						new UnsignedWordElement(0x11C, //
								batteryState = new ModbusReadLongChannel("BatteryState", this)//
										.label(0, "Initial")//
										.label(1, STOP)//
										.label(2, "Starting")//
										.label(3, START)//
										.label(4, "Stopping")//
										.label(5, "Fault")), //
						new UnsignedWordElement(0x11D, //
								batteryMode = new ModbusReadLongChannel("BatteryMode", this).label(0, "Normal Mode")),
						new SignedWordElement(0x11E,
								batteryVoltage = new ModbusReadLongChannel("BatteryVoltage", this).unit("mV")
										.multiplier(2)),
						new SignedWordElement(0x11F,
								batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this).unit("mA")
										.multiplier(2)),
						new SignedWordElement(0x120, //
								batteryPower = new ModbusReadLongChannel("BatteryPower", this).unit("W")//
										.multiplier(2)),
						new UnsignedWordElement(0x121, //
								soc = new ModbusReadLongChannel("Soc", this).unit("%")),
						new UnsignedWordElement(0x122, //
								allowedChargeCurrent = new ModbusReadLongChannel("AllowedChargeCurrent", this)
										.unit("mA")//
										.multiplier(2)//
										.negate()),
						new UnsignedWordElement(0x123, //
								allowedDischargeCurrent = new ModbusReadLongChannel("AllowedDischargeCurrent", this)
										.unit("mA").multiplier(2)),
						new UnsignedWordElement(0x124, //
								allowedCharge = new ModbusReadLongChannel("AllowedCharge", this).unit("W").multiplier(2)
										.negate()),
						new UnsignedWordElement(0x125, //
								allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this).unit("W")
										.multiplier(2)),
						new SignedDoublewordElement(0x126, //
								batteryChargeEnergy = new ModbusReadLongChannel("BatteryChargeEnergy",
										this).unit("kWh")).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(0x128, //
								batteryDischargeEnergy = new ModbusReadLongChannel(
										"BatteryDischargeEnergy", this).unit("kWh")).wordorder(WordOrder.LSWMSW),
						new UnsignedWordElement(0x12A, //
								batteryOperationStatus = new StatusBitChannel("BatteryOperationStatus", this)
										.label(1, "Battery group 1 operating")//
										.label(2, "Battery group 2 operating")//
										.label(4, "Battery group 3 operating")//
										.label(8, "Battery group 4 operating")),
						new UnsignedWordElement(0x12B, //
								batteryHighestVoltage = new ModbusReadLongChannel("BatteryHighestVoltage", this)
										.unit("mV")),
						new UnsignedWordElement(0x12C, //
								batteryLowestVoltage = new ModbusReadLongChannel("BatteryLowestVoltage", this)
										.unit("mV")),
						new SignedWordElement(0x12D, //
								batteryHighestTemperature = new ModbusReadLongChannel("BatteryHighestTemperature", this)
										.unit("�C")),
						new SignedWordElement(0x12E, //
								batteryLowestTemperature = new ModbusReadLongChannel("BatteryLowestTemperature", this)
										.unit("�C")),
						new UnsignedWordElement(0x12F, //
								batteryStopRequest = new ModbusReadLongChannel("BatteryStopRequest", this)),
						new UnsignedWordElement(0x130,
								batteryAlarm1 = warning.channel(new StatusBitChannel("BatteryAlarm1", this)//
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
						new UnsignedWordElement(0x131,
								batteryAlarm2 = warning.channel(new StatusBitChannel("BatteryAlarm2", this)//
										.label(1, "EEPROM parameter failure")//
										.label(2, "Switch off inside combined cabinet")//
										.label(32, "Should not be connected to grid due to the DC side condition")//
										.label(128, "Emergency stop require from system controller"))),
						new UnsignedWordElement(0x132,
								batteryAlarm3 = warning.channel(new StatusBitChannel("BatteryAlarm3", this)//
										.label(1, "Battery group 1 enable and not connected to grid")//
										.label(2, "Battery group 2 enable and not connected to grid")//
										.label(4, "Battery group 3 enable and not connected to grid")//
										.label(8, "Battery group 4 enable and not connected to grid"))),
						new UnsignedWordElement(0x133,
								batteryAlarm4 = warning.channel(new StatusBitChannel("BatteryAlarm4", this)//
										.label(1, "The isolation switch of battery group 1 open")//
										.label(2, "The isolation switch of battery group 2 open")//
										.label(4, "The isolation switch of battery group 3 open")//
										.label(8, "The isolation switch of battery group 4 open"))),
						new DummyElement(0x134),
						new UnsignedWordElement(0x135,
								batteryAlarm6 = warning.channel(new StatusBitChannel("BatteryAlarm6", this)//
										.label(1, "Balancing sampling failure of battery group 1")//
										.label(2, "Balancing sampling failure of battery group 2")//
										.label(4, "Balancing sampling failure of battery group 3")//
										.label(8, "Balancing sampling failure of battery group 4"))),
						new UnsignedWordElement(0x136,
								batteryAlarm7 = warning.channel(new StatusBitChannel("BatteryAlarm7", this)//
										.label(1, "Balancing control failure of battery group 1")//
										.label(2, "Balancing control failure of battery group 2")//
										.label(4, "Balancing control failure of battery group 3")//
										.label(8, "Balancing control failure of battery group 4"))),
						new UnsignedWordElement(0x137,
								batteryFault1 = warning.channel(new StatusBitChannel("BatteryFault1", this)//
										.label(1, "No enable batery group or usable battery group")//
										.label(2, "Normal leakage of battery group")//
										.label(4, "Serious leakage of battery group")//
										.label(8, "Battery start failure")//
										.label(16, "Battery stop failure")//
										.label(32,
												"Interruption of CAN Communication between battery group and controller")//
										.label(1024, "Emergency stop abnormal of auxiliary collector")//
										.label(2048, "Leakage self detection on negative")//
										.label(4096, "Leakage self detection on positive")//
										.label(8192, "Self detection failure on battery"))),
						new UnsignedWordElement(0x138,
								batteryFault2 = warning.channel(new StatusBitChannel("BatteryFault2", this)//
										.label(1, "CAN Communication interruption between battery group and group 1")//
										.label(2, "CAN Communication interruption between battery group and group 2")//
										.label(4, "CAN Communication interruption between battery group and group 3")//
										.label(8, "CAN Communication interruption between battery group and group 4"))),
						new UnsignedWordElement(0x139,
								batteryFault3 = warning.channel(new StatusBitChannel("BatteryFault3", this)//
										.label(1, "Main contractor abnormal in battery self detect group 1")//
										.label(2, "Main contractor abnormal in battery self detect group 2")//
										.label(4, "Main contractor abnormal in battery self detect group 3")//
										.label(8, "Main contractor abnormal in battery self detect group 4"))),
						new UnsignedWordElement(0x13A,
								batteryFault4 = warning.channel(new StatusBitChannel("BatteryFault4", this)//
										.label(1, "Pre-charge contractor abnormal on battery self detect group 1")//
										.label(2, "Pre-charge contractor abnormal on battery self detect group 2")//
										.label(4, "Pre-charge contractor abnormal on battery self detect group 3")//
										.label(8, "Pre-charge contractor abnormal on battery self detect group 4"))),
						new UnsignedWordElement(0x13B,
								batteryFault5 = warning.channel(new StatusBitChannel("BatteryFault5", this)//
										.label(1, "Main contact failure on battery control group 1")//
										.label(2, "Main contact failure on battery control group 2")//
										.label(4, "Main contact failure on battery control group 3")//
										.label(8, "Main contact failure on battery control group 4"))),
						new UnsignedWordElement(0x13C,
								batteryFault6 = warning.channel(new StatusBitChannel("BatteryFault6", this)//
										.label(1, "Pre-charge failure on battery control group 1")//
										.label(2, "Pre-charge failure on battery control group 2")//
										.label(4, "Pre-charge failure on battery control group 3")//
										.label(8, "Pre-charge failure on battery control group 4"))),
						new UnsignedWordElement(0x13D,
								batteryFault7 = warning.channel(new StatusBitChannel("BatteryFault7", this)//
								)), new UnsignedWordElement(0x13E,
										batteryFault8 = warning.channel(new StatusBitChannel("BatteryFault8", this)//
										)),
						new UnsignedWordElement(0x13F,
								batteryFault9 = warning.channel(new StatusBitChannel("BatteryFault9", this)//
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
						new UnsignedWordElement(0x140,
								batteryFault10 = warning.channel(new StatusBitChannel("BatteryFault10", this)//
										.label(4, "Serious temperature fault")//
										.label(8, "Communication fault for system controller")//
										.label(128, "Frog alarm")//
										.label(256, "Fuse fault")//
										.label(1024, "Normal leakage")//
										.label(2048, "Serious leakage")//
										.label(4096, "CAN disconnection between battery group and battery stack")//
										.label(8192, "Central contactor circuit open")//
										.label(16384, "BMU power contactor open"))),
						new UnsignedWordElement(0x141,
								batteryFault11 = warning.channel(new StatusBitChannel("BatteryFault11", this)//
								)), new UnsignedWordElement(0x142,
										batteryFault12 = warning.channel(new StatusBitChannel("BatteryFault12", this)//
										)),
						new UnsignedWordElement(0x143,
								batteryFault13 = warning.channel(new StatusBitChannel("BatteryFault13", this)//
								)), new UnsignedWordElement(0x144,
										batteryFault14 = warning.channel(new StatusBitChannel("BatteryFault14", this)//
										)), new UnsignedWordElement(0x145, batteryGroupControlStatus = warning
												.channel(new StatusBitChannel("BatteryGroupControlStatus", this)//
						)), new UnsignedWordElement(0x146,
								errorLog1 = warning.channel(new StatusBitChannel("ErrorLog1", this)//
								)), new UnsignedWordElement(0x147,
										errorLog2 = warning.channel(new StatusBitChannel("ErrorLog2", this)//
										)), new UnsignedWordElement(0x148,
												errorLog3 = warning.channel(new StatusBitChannel("ErrorLog3", this)//
												)),
						new UnsignedWordElement(0x149,
								errorLog4 = warning.channel(new StatusBitChannel("ErrorLog4", this)//
								)), new UnsignedWordElement(0x14a,
										errorLog5 = warning.channel(new StatusBitChannel("ErrorLog5", this)//
										)), new UnsignedWordElement(0x14b,
												errorLog6 = warning.channel(new StatusBitChannel("ErrorLog6", this)//
												)),
						new UnsignedWordElement(0x14c,
								errorLog7 = warning.channel(new StatusBitChannel("ErrorLog7", this)//
								)), new UnsignedWordElement(0x14d,
										errorLog8 = warning.channel(new StatusBitChannel("ErrorLog8", this)//
										)), new UnsignedWordElement(0x14e,
												errorLog9 = warning.channel(new StatusBitChannel("ErrorLog9", this)//
												)),
						new UnsignedWordElement(0x14f,
								errorLog10 = warning.channel(new StatusBitChannel("ErrorLog10", this)//
								)), new UnsignedWordElement(0x150,
										errorLog11 = warning.channel(new StatusBitChannel("ErrorLog11", this)//
										)), new UnsignedWordElement(0x151,
												errorLog12 = warning.channel(new StatusBitChannel("ErrorLog12", this)//
												)),
						new UnsignedWordElement(0x152,
								errorLog13 = warning.channel(new StatusBitChannel("ErrorLog13", this)//
								)), new UnsignedWordElement(0x153,
										errorLog14 = warning.channel(new StatusBitChannel("ErrorLog14", this)//
										)), new UnsignedWordElement(0x154,
												errorLog15 = warning.channel(new StatusBitChannel("ErrorLog15", this)//
												)),
						new UnsignedWordElement(0x155,
								errorLog16 = warning.channel(new StatusBitChannel("ErrorLog16", this)//
								))), new WriteableModbusRegisterRange(0x200, //
										new UnsignedWordElement(0x200, //
												setWorkState = new ModbusWriteLongChannel("SetWorkState", this) //
														.label(0, STOP) //
														.label(1, START)),
										new UnsignedWordElement(0x201, //
												setSystemErrorReset = new ModbusWriteLongChannel("SetSystemErrorReset",
														this)//
																.label(0, OFF)//
																.label(1, ON)),
										new UnsignedWordElement(0x202, //
												setOperationMode = new ModbusWriteLongChannel("SetOperationMode", this)//
														.label(0, "P/Q Set point")//
														.label(1, "IAC / cosphi set point"))),
				new WriteableModbusRegisterRange(0x203, new SignedWordElement(0x203, //
						setActivePower = new ModbusWriteLongChannel("SetActivePower", this)//
								.unit("W").multiplier(2))),
				new WriteableModbusRegisterRange(0x204, new SignedWordElement(0x204, //
						setActivePowerL1 = new ModbusWriteLongChannel("SetActivePowerL1", this)//
								.unit("W").multiplier(2)),
						new SignedWordElement(0x205, //
								setActivePowerL2 = new ModbusWriteLongChannel("SetActivePowerL2", this)//
										.unit("W").multiplier(2)),
						new SignedWordElement(0x206, //
								setActivePowerL3 = new ModbusWriteLongChannel("SetActivePowerL3", this)//
										.unit("W").multiplier(2))),
				new WriteableModbusRegisterRange(0x207, new SignedWordElement(0x207, //
						setReactivePower = new ModbusWriteLongChannel("SetReactivePower", this)//
								.unit("W").multiplier(2))),
				new WriteableModbusRegisterRange(0x208, new SignedWordElement(0x208, //
						setReactivePowerL1 = new ModbusWriteLongChannel("SetReactivePowerL1", this)//
								.unit("W").multiplier(2)),
						new SignedWordElement(0x209, //
								setReactivePowerL2 = new ModbusWriteLongChannel("SetReactivePowerL2", this)//
										.unit("W").multiplier(2)),
						new SignedWordElement(0x20A, //
								setReactivePowerL3 = new ModbusWriteLongChannel("SetReactivePowerL3", this)//
										.unit("W").multiplier(2))));
		// new ModbusInputRegisterRange(0x6040,
		// new UnsignedWordElement(0x6040, //
		// batteryInformation1 = new ModbusReadLongChannel("BatteryInformation1", this)),
		// new UnsignedWordElement(0x6041, //
		// batteryInformation2 = new ModbusReadLongChannel("BatteryInformation2", this)),
		// new UnsignedWordElement(0x6042, //
		// batteryInformation3 = new ModbusReadLongChannel("BatteryInformation3", this)),
		// new UnsignedWordElement(0x6043, //
		// batteryInformation4 = new ModbusReadLongChannel("BatteryInformation4", this))),
		// new ModbusInputRegisterRange(0x6840,
		// new UnsignedWordElement(0x6840, //
		// batteryInformation5 = new ModbusReadLongChannel("BatteryInformation5", this)),
		// new UnsignedWordElement(0x6841, //
		// batteryInformation6 = new ModbusReadLongChannel("BatteryInformation6", this)),
		// new UnsignedWordElement(0x6842, //
		// batteryInformation7 = new ModbusReadLongChannel("BatteryInformation7", this)),
		// new UnsignedWordElement(0x6843, //
		// batteryInformation8 = new ModbusReadLongChannel("BatteryInformation8", this))),
		// new ModbusInputRegisterRange(0x7640,
		// new UnsignedWordElement(0x7640, //
		// batteryInformation9 = new ModbusReadLongChannel("BatteryInformation9", this)),
		// new UnsignedWordElement(0x7641, //
		// batteryInformation10 = new ModbusReadLongChannel("BatteryInformation10", this)),
		// new UnsignedWordElement(0x7642, //
		// batteryInformation11 = new ModbusReadLongChannel("BatteryInformation11", this)),
		// new UnsignedWordElement(0x7643, //
		// batteryInformation12 = new ModbusReadLongChannel("BatteryInformation12", this))),
		// new ModbusInputRegisterRange(0x8440,
		// new UnsignedWordElement(0x8440, //
		// batteryInformation13 = new ModbusReadLongChannel("BatteryInformation13", this)),
		// new UnsignedWordElement(0x8441, //
		// batteryInformation14 = new ModbusReadLongChannel("BatteryInformation14", this)),
		// new UnsignedWordElement(0x8442, //
		// batteryInformation15 = new ModbusReadLongChannel("BatteryInformation15", this)),
		// new UnsignedWordElement(0x8443, //
		// batteryInformation16 = new ModbusReadLongChannel("BatteryInformation16", this))),
		// new ModbusInputRegisterRange(0x9240,
		// new UnsignedWordElement(0x9240, //
		// batteryInformation17 = new ModbusReadLongChannel("BatteryInformation17", this)),
		// new UnsignedWordElement(0x9241, //
		// batteryInformation18 = new ModbusReadLongChannel("BatteryInformation18", this)),
		// new UnsignedWordElement(0x9242, //
		// batteryInformation19 = new ModbusReadLongChannel("BatteryInformation19", this)),
		// new UnsignedWordElement(0x9243, //
		// batteryInformation20 = new ModbusReadLongChannel("BatteryInformation20", this))));
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public ReadChannel<Long> activePowerL1() {
		return activePowerL1;
	}

	@Override
	public ReadChannel<Long> activePowerL2() {
		return activePowerL2;
	}

	@Override
	public ReadChannel<Long> activePowerL3() {
		return activePowerL3;
	}

	@Override
	public ReadChannel<Long> reactivePowerL1() {
		return reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return reactivePowerL3;
	}

	@Override
	public WriteChannel<Long> setActivePowerL1() {
		return setActivePowerL1;
	}

	@Override
	public WriteChannel<Long> setActivePowerL2() {
		return setActivePowerL2;
	}

	@Override
	public WriteChannel<Long> setActivePowerL3() {
		return setActivePowerL3;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL1() {
		return setReactivePowerL1;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL2() {
		return setReactivePowerL2;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL3() {
		return setReactivePowerL3;
	}

}
