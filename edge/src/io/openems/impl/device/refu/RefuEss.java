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

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
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
import io.openems.core.utilities.power.NoPBetweenLimitation;
import io.openems.core.utilities.power.PGreaterEqualLimitation;
import io.openems.core.utilities.power.PSmallerEqualLimitation;
import io.openems.core.utilities.power.SymmetricPowerImpl;
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
		this.thingState = new ThingStateChannel(this);
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
	private SymmetricPowerImpl power;
	private PGreaterEqualLimitation allowedChargeLimit;
	private PSmallerEqualLimitation allowedDischargeLimit;
	private NoPBetweenLimitation batFullLimit;
	private NoPBetweenLimitation batEmptyLimit;
	private ThingStateChannel thingState;
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
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusInputRegisterRange(0x100, //
						new UnsignedWordElement(0x100, //
								systemState = new ModbusReadLongChannel("SystemState", this) //
								.label(0, STOP) //
								.label(1, "Init") //
								.label(2, "Pre-operation") //
								.label(3, STANDBY) //
								.label(4, START) //
								.label(5, FAULT)),
						new UnsignedWordElement(0x101, //
								new ModbusBitWrappingChannel("SystemError1", this, this.thingState)//
								.faultBit(0, FaultEss.BMSInError)//
								.faultBit(1, FaultEss.BMSInErrorSecond)//
								.faultBit(2, FaultEss.BMSUndervoltage)//
								.faultBit(3, FaultEss.BMSOvercurrent)//
								.faultBit(4, FaultEss.ErrorBMSLimitsNotInitialized)//
								.faultBit(5, FaultEss.ConnectError)//
								.faultBit(6, FaultEss.OvervoltageWarning)//
								.faultBit(7, FaultEss.UndervoltageWarning)//
								.faultBit(8, FaultEss.OvercurrentWarning)//
								.faultBit(9, FaultEss.BMSReady)//
								.faultBit(10, FaultEss.TREXReady)//
								), //

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
						new UnsignedWordElement(0x130, //
								new ModbusBitWrappingChannel("BatteryAlarm1", this, this.thingState)//
								.warningBit(0, WarningEss.NormalChargingOverCurrent)//
								.warningBit(1, WarningEss.CharginigCurrentOverLimit)//
								.warningBit(2, WarningEss.DischargingCurrentOverLimit)//
								.warningBit(3, WarningEss.NormalHighVoltage)//
								.warningBit(4, WarningEss.NormalLowVoltage)//
								.warningBit(5, WarningEss.AbnormalVoltageVariation)//
								.warningBit(6, WarningEss.NormalHighTemperature)//
								.warningBit(7, WarningEss.NormalLowTemperature)//
								.warningBit(8, WarningEss.AbnormalTemperatureVariation)//
								.warningBit(9, WarningEss.SeriousHighVoltage)//
								.warningBit(10, WarningEss.SeriousLowVoltage)//
								.warningBit(11, WarningEss.SeriousLowTemperature)//
								.warningBit(12, WarningEss.ChargingSeriousOverCurrent)//
								.warningBit(13, WarningEss.DischargingSeriousOverCurrent)//
								.warningBit(14, WarningEss.AbnormalCapacityAlarm)//
								), //

						new UnsignedWordElement(0x131, //
								new ModbusBitWrappingChannel("BatteryAlarm2", this, this.thingState)//
								.warningBit(0, WarningEss.EEPROMParameterFailure)//
								.warningBit(1, WarningEss.SwitchOfInsideCombinedCabinet)
								.warningBit(5, WarningEss.ShouldNotBeConnectedToGridDueToTheDCSideCondition)
								.warningBit(7, WarningEss.EmergencyStopRequireFromSystemController)), //

						new UnsignedWordElement(0x132, //
								new ModbusBitWrappingChannel("BatteryAlarm3", this, this.thingState)//
								.warningBit(0, WarningEss.BatteryGroup1EnableAndNotConnectedToGrid)//
								.warningBit(1, WarningEss.BatteryGroup2EnableAndNotConnectedToGrid)//
								.warningBit(2, WarningEss.BatteryGroup3EnableAndNotConnectedToGrid)//
								.warningBit(3, WarningEss.BatteryGroup4EnableAndNotConnectedToGrid)//
								), //

						new UnsignedWordElement(0x133, //
								new ModbusBitWrappingChannel("BatteryAlarm4", this, this.thingState)//
								.warningBit(0, WarningEss.TheIsolationSwitchOfBatteryGroup1Open)
								.warningBit(1, WarningEss.TheIsolationSwitchOfBatteryGroup2Open)
								.warningBit(2, WarningEss.TheIsolationSwitchOfBatteryGroup3Open)
								.warningBit(3, WarningEss.TheIsolationSwitchOfBatteryGroup4Open)), //

						new DummyElement(0x134), //
						new UnsignedWordElement(0x135,
								new ModbusBitWrappingChannel("BatteryAlarm6", this, this.thingState)//
								.warningBit(0, WarningEss.BalancingSamplingFailureOfBatteryGroup1)//
								.warningBit(1, WarningEss.BalancingSamplingFailureOfBatteryGroup2)//
								.warningBit(2, WarningEss.BalancingSamplingFailureOfBatteryGroup3)//
								.warningBit(3, WarningEss.BalancingSamplingFailureOfBatteryGroup4)//
								), //

						new UnsignedWordElement(0x136, //
								new ModbusBitWrappingChannel("BatteryAlarm7", this, this.thingState)//
								.warningBit(0, WarningEss.BalancingControlFailureOfBatteryGroup1)//
								.warningBit(1, WarningEss.BalancingControlFailureOfBatteryGroup2)//
								.warningBit(2, WarningEss.BalancingControlFailureOfBatteryGroup3)//
								.warningBit(3, WarningEss.BalancingControlFailureOfBatteryGroup4)//
								), //

						new UnsignedWordElement(0x137, //
								new ModbusBitWrappingChannel("BatteryFault1", this, this.thingState)//
								.faultBit(0, FaultEss.NoEnableBateryGroupOrUsableBatteryGroup)//
								.faultBit(1, FaultEss.NormalLeakageOfBatteryGroup)//
								.faultBit(2, FaultEss.SeriousLeakageOfBatteryGroup)//
								.faultBit(3, FaultEss.BatteryStartFailure)//
								.faultBit(4, FaultEss.BatteryStopFailure)//
								.faultBit(5,
										FaultEss.InterruptionOfCANCommunicationBetweenBatteryGroupAndController)//
								.faultBit(10, FaultEss.EmergencyStopAbnormalOfAuxiliaryCollector)//
								.faultBit(11, FaultEss.LeakageSelfDetectionOnNegative)//
								.faultBit(12, FaultEss.LeakageSelfDetectionOnPositive)//
								.faultBit(13, FaultEss.SelfDetectionFailureOnBattery)//
								), //

						new UnsignedWordElement(0x138, //
								new ModbusBitWrappingChannel("BatteryFault2", this, this.thingState)//
								.faultBit(0, FaultEss.CANCommunicationInterruptionBetweenBatteryGroupAndGroup1)//
								.faultBit(1, FaultEss.CANCommunicationInterruptionBetweenBatteryGroupAndGroup2)//
								.faultBit(2, FaultEss.CANCommunicationInterruptionBetweenBatteryGroupAndGroup3)//
								.faultBit(3, FaultEss.CANCommunicationInterruptionBetweenBatteryGroupAndGroup4)//
								), //

						new UnsignedWordElement(0x139, //
								new ModbusBitWrappingChannel("BatteryFault3", this, this.thingState)//
								.faultBit(0, FaultEss.MainContractorAbnormalInBatterySelfDetectGroup1)//
								.faultBit(1, FaultEss.MainContractorAbnormalInBatterySelfDetectGroup2)//
								.faultBit(2, FaultEss.MainContractorAbnormalInBatterySelfDetectGroup3)//
								.faultBit(3, FaultEss.MainContractorAbnormalInBatterySelfDetectGroup4)//
								), //

						new UnsignedWordElement(0x13A, //
								new ModbusBitWrappingChannel("BatteryFault4", this, this.thingState)//
								.faultBit(0, FaultEss.PreChargeContractorAbnormalOnBatterySelfDetectGroup1)//
								.faultBit(1, FaultEss.PreChargeContractorAbnormalOnBatterySelfDetectGroup2)//
								.faultBit(2, FaultEss.PreChargeContractorAbnormalOnBatterySelfDetectGroup3)//
								.faultBit(3, FaultEss.PreChargeContractorAbnormalOnBatterySelfDetectGroup4)//
								), //

						new UnsignedWordElement(0x13B, //
								new ModbusBitWrappingChannel("BatteryFault5", this, this.thingState)//
								.faultBit(0, FaultEss.MainContactFailureOnBatteryControlGroup1)//
								.faultBit(1, FaultEss.MainContactFailureOnBatteryControlGroup2)//
								.faultBit(2, FaultEss.MainContactFailureOnBatteryControlGroup3)//
								.faultBit(3, FaultEss.MainContactFailureOnBatteryControlGroup4)//
								), //

						new UnsignedWordElement(0x13C, //
								new ModbusBitWrappingChannel("BatteryFault6", this, this.thingState)//
								.faultBit(0, FaultEss.PreChargeFailureOnBatteryControlGroup1)//
								.faultBit(1, FaultEss.PreChargeFailureOnBatteryControlGroup2)//
								.faultBit(2, FaultEss.PreChargeFailureOnBatteryControlGroup3)//
								.faultBit(3, FaultEss.PreChargeFailureOnBatteryControlGroup4)//
								), //

						new UnsignedWordElement(0x13D, //
								new ModbusBitWrappingChannel("BatteryFault7", this, this.thingState)//
								// .faultBit(0, FaultEss)//
								), //

						new UnsignedWordElement(0x13E, //
								new ModbusBitWrappingChannel("BatteryFault8", this, this.thingState)//
								// .faultBit(0, FaultEss)//
								), //
						new UnsignedWordElement(0x13F, //
								new ModbusBitWrappingChannel("BatteryFault9", this, this.thingState)//
								.faultBit(2, FaultEss.SamplingCircuitAbnormalForBMU)//
								.faultBit(3, FaultEss.PowerCableDisconnectFailure)//
								.faultBit(4, FaultEss.SamplingCircuitDisconnectFailure)//
								.faultBit(6, FaultEss.CANDisconnectForMasterAndSlave)//
								.faultBit(9, FaultEss.SammplingCircuitFailure)//
								.faultBit(10, FaultEss.SingleBatteryFailure)//
								.faultBit(11, FaultEss.CircuitDetectionAbnormalForMainContactor)//
								.faultBit(12, FaultEss.CircuitDetectionAbnormalForMainContactorSecond)//
								.faultBit(13, FaultEss.CircuitDetectionAbnormalForFancontactor)//
								.faultBit(14, FaultEss.BMUPowerContactorCircuitDetectionAbnormal)//
								.faultBit(15, FaultEss.CentralContactorCircuitDetectionAbnormal)//
								), //

						new UnsignedWordElement(0x140, //
								new ModbusBitWrappingChannel("BatteryFault10", this, this.thingState)//
								.faultBit(2, FaultEss.SeriousTemperatureFault)//
								.faultBit(3, FaultEss.CommunicationFaultForSystemController)//
								.faultBit(7, FaultEss.FrogAlarm)//
								.faultBit(8, FaultEss.FuseFault)//
								.faultBit(10, FaultEss.NormalLeakage)//
								.faultBit(11, FaultEss.SeriousLeakage)//
								.faultBit(12, FaultEss.CANDisconnectionBetweenBatteryGroupAndBatteryStack)//
								.faultBit(13, FaultEss.CentralContactorCircuitOpen)//
								.faultBit(14, FaultEss.BMUPowerContactorOpen)//
								), //

						new UnsignedWordElement(0x141, //
								new ModbusBitWrappingChannel("BatteryFault11", this, this.thingState)//
								// .faultBit(, FaultEss)//
								), //

						new UnsignedWordElement(0x142, //
								new ModbusBitWrappingChannel("BatteryFault12", this, this.thingState)//
								// .faultBit(, FaultEss)//
								), //

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
		this.power = new SymmetricPowerImpl(100000, setActivePower, setReactivePower);
		this.allowedChargeLimit = new PGreaterEqualLimitation(power);
		this.allowedChargeLimit.setP(this.allowedCharge.valueOptional().orElse(0L));
		this.batFullLimit = new NoPBetweenLimitation(power);
		this.power.addStaticLimitation(batFullLimit);
		this.allowedCharge.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				allowedChargeLimit.setP(allowedCharge.valueOptional().orElse(0L));
				if (allowedCharge.isValuePresent()) {
					if (allowedCharge.getValue() > -100) {
						batFullLimit.setP(0L, 5000L);
					} else {
						batFullLimit.setP(null, null);
					}
				}
			}
		});
		this.power.addStaticLimitation(this.allowedChargeLimit);
		this.allowedDischargeLimit = new PSmallerEqualLimitation(power);
		this.allowedDischargeLimit.setP(this.allowedDischarge.valueOptional().orElse(0L));
		this.batEmptyLimit = new NoPBetweenLimitation(power);
		this.power.addStaticLimitation(batEmptyLimit);
		this.allowedDischarge.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				allowedDischargeLimit.setP(allowedDischarge.valueOptional().orElse(0L));
				if (allowedDischarge.isValuePresent()) {
					if(allowedDischarge.getValue() < 100) {
						batEmptyLimit.setP(-5000L, 0L);
					}else {
						batEmptyLimit.setP(null, null);
					}
				}
			}
		});
		return protocol;
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

	@Override
	public SymmetricPowerImpl getPower() {
		return power;
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return thingState;
	}

}
