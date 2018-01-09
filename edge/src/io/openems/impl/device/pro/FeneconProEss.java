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
package io.openems.impl.device.pro;

import java.util.Locale;
import java.util.ResourceBundle;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.ValueToBooleanChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.realtimeclock.RealTimeClockNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusBitWrappingChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRegisterRange;

@ThingInfo(title = "FENECON Pro ESS")
public class FeneconProEss extends ModbusDeviceNature implements AsymmetricEssNature, RealTimeClockNature {

	/*
	 * Constructors
	 */
	public FeneconProEss(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		minSoc.addUpdateListener((channel, newValue) -> {
			// If chargeSoc was not set -> set it to minSoc minus 2
			if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
				chargeSoc.updateValue((Integer) newValue.get() - 2, false);
			}
		});
		ResourceBundle.getBundle("Messages", Locale.GERMAN);
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);

	private ThingStateChannel state = new ThingStateChannel(this);

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
	// ESS
	private ModbusReadLongChannel allowedCharge;
	private ModbusReadLongChannel allowedDischarge;
	private ReadChannel<Long> gridMode;
	private ModbusReadLongChannel soc;
	private ModbusReadLongChannel systemState;
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel reactivePowerL1;
	private ModbusReadLongChannel reactivePowerL2;
	private ModbusReadLongChannel reactivePowerL3;
	private ModbusWriteLongChannel setWorkState;
	private ModbusWriteLongChannel setActivePowerL1;
	private ModbusWriteLongChannel setActivePowerL2;
	private ModbusWriteLongChannel setActivePowerL3;
	private ModbusWriteLongChannel setReactivePowerL1;
	private ModbusWriteLongChannel setReactivePowerL2;
	private ModbusWriteLongChannel setReactivePowerL3;
	private ReadChannel<Long> allowedApparent;
	// RealTimeClock
	private ModbusWriteLongChannel rtcYear;
	private ModbusWriteLongChannel rtcMonth;
	private ModbusWriteLongChannel rtcDay;
	private ModbusWriteLongChannel rtcHour;
	private ModbusWriteLongChannel rtcMinute;
	private ModbusWriteLongChannel rtcSecond;
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 12000L).unit("Wh");
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 9000L)
			.unit("VA");

	@Override
	public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override
	public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

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
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
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
	public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
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
	public WriteChannel<Long> rtcYear() {
		return rtcYear;
	}

	@Override
	public WriteChannel<Long> rtcMonth() {
		return rtcMonth;
	}

	@Override
	public WriteChannel<Long> rtcDay() {
		return rtcDay;
	}

	@Override
	public WriteChannel<Long> rtcHour() {
		return rtcHour;
	}

	@Override
	public WriteChannel<Long> rtcMinute() {
		return rtcMinute;
	}

	@Override
	public WriteChannel<Long> rtcSecond() {
		return rtcSecond;
	}

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel phaseAllowedApparent;
	public ModbusReadLongChannel frequencyL3;
	public ModbusReadLongChannel frequencyL2;
	public ModbusReadLongChannel frequencyL1;
	public ModbusReadLongChannel currentL1;
	public ModbusReadLongChannel currentL2;
	public ModbusReadLongChannel currentL3;
	public ModbusReadLongChannel voltageL1;
	public ModbusReadLongChannel voltageL2;
	public ModbusReadLongChannel voltageL3;
	public ModbusReadLongChannel pcsOperationState;
	public ModbusReadLongChannel batteryPower;
	public ModbusReadLongChannel batteryCurrent;
	public ModbusReadLongChannel batteryVoltage;
	public ModbusReadLongChannel batteryVoltageSection1;
	public ModbusReadLongChannel batteryVoltageSection2;
	public ModbusReadLongChannel batteryVoltageSection3;
	public ModbusReadLongChannel batteryVoltageSection4;
	public ModbusReadLongChannel batteryVoltageSection5;
	public ModbusReadLongChannel batteryVoltageSection6;
	public ModbusReadLongChannel batteryVoltageSection7;
	public ModbusReadLongChannel batteryVoltageSection8;
	public ModbusReadLongChannel batteryVoltageSection9;
	public ModbusReadLongChannel batteryVoltageSection10;
	public ModbusReadLongChannel batteryVoltageSection11;
	public ModbusReadLongChannel batteryVoltageSection12;
	public ModbusReadLongChannel batteryVoltageSection13;
	public ModbusReadLongChannel batteryVoltageSection14;
	public ModbusReadLongChannel batteryVoltageSection15;
	public ModbusReadLongChannel batteryVoltageSection16;
	public ModbusReadLongChannel batteryTemperatureSection1;
	public ModbusReadLongChannel batteryTemperatureSection2;
	public ModbusReadLongChannel batteryTemperatureSection3;
	public ModbusReadLongChannel batteryTemperatureSection4;
	public ModbusReadLongChannel batteryTemperatureSection5;
	public ModbusReadLongChannel batteryTemperatureSection6;
	public ModbusReadLongChannel batteryTemperatureSection7;
	public ModbusReadLongChannel batteryTemperatureSection8;
	public ModbusReadLongChannel batteryTemperatureSection9;
	public ModbusReadLongChannel batteryTemperatureSection10;
	public ModbusReadLongChannel batteryTemperatureSection11;
	public ModbusReadLongChannel batteryTemperatureSection12;
	public ModbusReadLongChannel batteryTemperatureSection13;
	public ModbusReadLongChannel batteryTemperatureSection14;
	public ModbusReadLongChannel batteryTemperatureSection15;
	public ModbusReadLongChannel batteryTemperatureSection16;
	public ModbusReadLongChannel batteryGroupState;
	public ModbusReadLongChannel totalBatteryDischargeEnergy;
	public ModbusReadLongChannel totalBatteryChargeEnergy;
	public ModbusReadLongChannel workMode;
	public ModbusReadLongChannel controlMode;
	public ModbusWriteLongChannel setPcsMode;
	public ModbusWriteLongChannel setSetupMode;
	public ModbusReadLongChannel setupMode;
	public ModbusReadLongChannel pcsMode;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protokol = new ModbusProtocol(new ModbusRegisterRange(100, //
				new UnsignedWordElement(100, //
						systemState = new ModbusReadLongChannel("SystemState", this) //
						.label(0, STANDBY) //
						.label(1, "Start Off-Grid") //
						.label(2, START) //
						.label(3, FAULT) //
						.label(4, "Off-grid PV")),
				new UnsignedWordElement(101, //
						controlMode = new ModbusReadLongChannel("ControlMode", this) //
						.label(1, "Remote") //
						.label(2, "Local")), //
				new UnsignedWordElement(102, //
						workMode = new ModbusReadLongChannel("WorkMode", this) //
						.label(2, "Economy") //
						.label(6, "Remote") //
						.label(8, "Timing")), //
				new DummyElement(103), //
				new UnsignedDoublewordElement(104, //
						totalBatteryChargeEnergy = new ModbusReadLongChannel("TotalBatteryChargeEnergy", this)
						.unit("Wh")), //
				new UnsignedDoublewordElement(106, //
						totalBatteryDischargeEnergy = new ModbusReadLongChannel("TotalBatteryDischargeEnergy", this)
						.unit("Wh")), //
				new UnsignedWordElement(108, //
						batteryGroupState = new ModbusReadLongChannel("BatteryGroupState", this) //
						.label(0, "Initial") //
						.label(1, "Stop") //
						.label(2, "Starting") //
						.label(3, "Running") //
						.label(4, "Stopping") //
						.label(5, "Fail")),
				new UnsignedWordElement(109, //
						soc = new ModbusReadLongChannel("Soc", this).unit("%").interval(0, 100)),
				new UnsignedWordElement(110, //
						batteryVoltage = new ModbusReadLongChannel("BatteryVoltage", this).unit("mV").multiplier(2)),
				new SignedWordElement(111, //
						batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this).unit("mA").multiplier(2)),
				new SignedWordElement(112, //
						batteryPower = new ModbusReadLongChannel("BatteryPower", this).unit("W")),
				new UnsignedWordElement(113, new ModbusBitWrappingChannel("BatteryGroupAlarm", this, state)//
						.warningBit(0, WarningEss.FailTheSystemShouldBeStopped) // Fail, The system should be stopped
						.warningBit(1, WarningEss.CommonLowVoltageAlarm) // Common low voltage alarm
						.warningBit(2, WarningEss.CommonHighVoltageAlarm) // Common high voltage alarm
						.warningBit(3, WarningEss.ChargingOverCurrentAlarm) // Charging over current alarm
						.warningBit(4, WarningEss.DischargingOverCurrentAlarm) // Discharging over current alarm
						.warningBit(5, WarningEss.OverTemperatureAlarm) // Over temperature alarm
						.warningBit(6, WarningEss.InteralCommunicationAbnormal)), // Interal communication abnormal
				new UnsignedWordElement(114, //
						pcsOperationState = new ModbusReadLongChannel("PcsOperationState", this)
						.label(0, "Self-checking") //
						.label(1, "Standby") //
						.label(2, "Off grid PV") //
						.label(3, "Off grid") //
						.label(4, ON_GRID) //
						.label(5, "Fail") //
						.label(6, "bypass 1") //
						.label(7, "bypass 2")),
				new DummyElement(115, 117), //
				new SignedWordElement(118, //
						currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA").multiplier(2)),
				new SignedWordElement(119, //
						currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA").multiplier(2)),
				new SignedWordElement(120, //
						currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA").multiplier(2)),
				new UnsignedWordElement(121, //
						voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(2)),
				new UnsignedWordElement(122, //
						voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV").multiplier(2)),
				new UnsignedWordElement(123, //
						voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV").multiplier(2)),
				new SignedWordElement(124, //
						activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")),
				new SignedWordElement(125, //
						activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")),
				new SignedWordElement(126, //
						activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")),
				new SignedWordElement(127, //
						reactivePowerL1 = new ModbusReadLongChannel("ReactivePowerL1", this).unit("var")),
				new SignedWordElement(128, //
						reactivePowerL2 = new ModbusReadLongChannel("ReactivePowerL2", this).unit("var")),
				new SignedWordElement(129, //
						reactivePowerL3 = new ModbusReadLongChannel("ReactivePowerL3", this).unit("var")),
				new DummyElement(130), new UnsignedWordElement(131, //
						frequencyL1 = new ModbusReadLongChannel("FrequencyL1", this).unit("mHz").multiplier(1)),
				new UnsignedWordElement(132, //
						frequencyL2 = new ModbusReadLongChannel("FrequencyL2", this).unit("mHz").multiplier(1)),
				new UnsignedWordElement(133, //
						frequencyL3 = new ModbusReadLongChannel("FrequencyL3", this).unit("mHz").multiplier(1)),
				new UnsignedWordElement(134, //
						phaseAllowedApparent = new ModbusReadLongChannel("PhaseAllowedApparentPower", this).unit("VA")),
				new DummyElement(135, 140), new UnsignedWordElement(141, //
						allowedCharge = new ModbusReadLongChannel("AllowedCharge", this).negate().unit("W")),
				new UnsignedWordElement(142, //
						allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this).unit("W"))),
				new ModbusRegisterRange(150,
						new UnsignedWordElement(150, new ModbusBitWrappingChannel("PcsAlarm1L1", this, state)//
								.warningBit(0, WarningEss.GridUndervoltageL1) // Grid undervoltage
								.warningBit(1, WarningEss.GridOvervoltageL1) // Grid overvoltage
								.warningBit(2, WarningEss.GridUnderFrequencyL1) // Grid under frequency
								.warningBit(3, WarningEss.GridOverFrequencyL1) // Grid over frequency
								.warningBit(4, WarningEss.GridPowerSupplyOffL1) // Grid power supply off
								.warningBit(5, WarningEss.GridConditionUnmeetL1) // Grid condition unmeet
								.warningBit(6, WarningEss.DCUnderVoltageL1) // DC under voltage
								.warningBit(7, WarningEss.InputOverResistanceL1) // Input over resistance
								.warningBit(8, WarningEss.CombinationErrorL1) // Combination error
								.warningBit(9, WarningEss.CommWithInverterErrorL1) // Comm with inverter error
								.warningBit(10, WarningEss.TmeErrorL1)), // Tme error
						new UnsignedWordElement(151, new ModbusBitWrappingChannel("PcsAlarm2L1", this, state)),
						new UnsignedWordElement(152, new ModbusBitWrappingChannel("PcsFault1L1", this, state)//
								.faultBit(0, FaultEss.ControlCurrentOverload100PercentL1) // Control current overload 100%
								.faultBit(1, FaultEss.ControlCurrentOverload110PercentL1) // Control current overload 110%
								.faultBit(2, FaultEss.ControlCurrentOverload150PercentL1) // Control current overload 150%
								.faultBit(3, FaultEss.ControlCurrentOverload200PercentL1) // Control current overload 200%
								.faultBit(4, FaultEss.ControlCurrentOverload120PercentL1) // Control current overload 120%
								.faultBit(5, FaultEss.ControlCurrentOverload300PercentL1) // Control current overload 300%
								.faultBit(6, FaultEss.ControlTransientLoad300PercentL1) // Control transient load 300%
								.faultBit(7, FaultEss.GridOverCurrentL1) // Grid over current
								.faultBit(8, FaultEss.LockingWaveformTooManyTimesL1) // Locking waveform too many times
								.faultBit(9, FaultEss.InverterVoltageZeroDriftErrorL1) // Inverter voltage zero drift error
								.faultBit(10, FaultEss.GridVoltageZeroDriftErrorL1) // Grid voltage zero drift error
								.faultBit(11, FaultEss.ControlCurrentZeroDriftErrorL1) // Control current zero drift error
								.faultBit(12, FaultEss.InverterCurrentZeroDriftErrorL1) // Inverter current zero drift error
								.faultBit(13, FaultEss.GridCurrentZeroDriftErrorL1) // Grid current zero drift error
								.faultBit(14, FaultEss.PDPProtectionL1) // PDP protection
								.faultBit(15, FaultEss.HardwareControlCurrentProtectionL1)), // Hardware control current protection
						new UnsignedWordElement(153, new ModbusBitWrappingChannel("PcsFault2L1", this, state)//
								.faultBit(0, FaultEss.HardwareACVoltageProtectionL1) // Hardware AC volt. protection
								.faultBit(1, FaultEss.HardwareDCCurrentProtectionL1) // Hardware DC curr. protection
								.faultBit(2, FaultEss.HardwareTemperatureProtectionL1) // Hardware temperature protection
								.faultBit(3, FaultEss.NoCapturingSignalL1) // No capturing signal
								.faultBit(4, FaultEss.DCOvervoltageL1) // DC overvoltage
								.faultBit(5, FaultEss.DCDisconnectedL1) // DC disconnected
								.faultBit(6, FaultEss.InverterUndervoltageL1) // Inverter undervoltage
								.faultBit(7, FaultEss.InverterOvervoltageL1) // Inverter overvoltage
								.faultBit(8, FaultEss.CurrentSensorFailL1) // Current sensor fail
								.faultBit(9, FaultEss.VoltageSensorFailL1) // Voltage sensor fail
								.faultBit(10, FaultEss.PowerUncontrollableL1) // Power uncontrollable
								.faultBit(11, FaultEss.CurrentUncontrollableL1) // Current uncontrollable
								.faultBit(12, FaultEss.FanErrorL1) // Fan error
								.faultBit(13, FaultEss.PhaseLackL1) // Phase lack
								.faultBit(14, FaultEss.InverterRelayFaultL1) // Inverter relay fault
								.faultBit(15, FaultEss.GridRealyFaultL1)), // Grid relay fault
						new UnsignedWordElement(154, new ModbusBitWrappingChannel("PcsFault3L1", this, state)//
								.faultBit(0, FaultEss.ControlPanelOvertempL1) // Control panel overtemp
								.faultBit(1, FaultEss.PowerPanelOvertempL1) // Power panel overtemp
								.faultBit(2, FaultEss.DCInputOvercurrentL1) // DC input overcurrent
								.faultBit(3, FaultEss.CapacitorOvertempL1) // Capacitor overtemp
								.faultBit(4, FaultEss.RadiatorOvertempL1) // Radiator overtemp
								.faultBit(5, FaultEss.TransformerOvertempL1) // Transformer overtemp
								.faultBit(6, FaultEss.CombinationCommErrorL1) // Combination comm error
								.faultBit(7, FaultEss.EEPROMErrorL1) // EEPROM error
								.faultBit(8, FaultEss.LoadCurrentZeroDriftErrorL1) // Load current zero drift error
								.faultBit(9, FaultEss.CurrentLimitRErrorL1) // Current limit-R error
								.faultBit(10, FaultEss.PhaseSyncErrorL1) // Phase sync error
								.faultBit(11, FaultEss.ExternalPVCurrentZeroDriftErrorL1) // External PV current zero drift error
								.faultBit(12, FaultEss.ExternalGridCurrentZeroDriftErrorL1)), // External grid current zero drift error
						new UnsignedWordElement(155, new ModbusBitWrappingChannel("PcsAlarm1L2", this, state)//
								.warningBit(0, WarningEss.GridUndervoltageL2) // Grid undervoltage
								.warningBit(1, WarningEss.GridOvervoltageL2) // Grid overvoltage
								.warningBit(2, WarningEss.GridUnderFrequencyL2) // Grid under frequency
								.warningBit(3, WarningEss.GridOverFrequencyL2) // Grid over frequency
								.warningBit(4, WarningEss.GridPowerSupplyOffL2) // Grid power supply off
								.warningBit(5, WarningEss.GridConditionUnmeetL2) // Grid condition unmeet
								.warningBit(6, WarningEss.DCUnderVoltageL2) // DC under voltage
								.warningBit(7, WarningEss.InputOverResistanceL2) // Input over resistance
								.warningBit(8, WarningEss.CombinationErrorL2) // Combination error
								.warningBit(9, WarningEss.CommWithInverterErrorL2) // Comm with inverter error
								.warningBit(10, WarningEss.TmeErrorL2)), // Tme error
						new UnsignedWordElement(156, new ModbusBitWrappingChannel("PcsAlarm2L2", this, state)),
						new UnsignedWordElement(157, new ModbusBitWrappingChannel("PcsFault1L2", this, state)//
								.faultBit(0, FaultEss.ControlCurrentOverload100PercentL2) // Control current overload 100%
								.faultBit(1, FaultEss.ControlCurrentOverload110PercentL2) // Control current overload 110%
								.faultBit(2, FaultEss.ControlCurrentOverload150PercentL2) // Control current overload 150%
								.faultBit(3, FaultEss.ControlCurrentOverload200PercentL2) // Control current overload 200%
								.faultBit(4, FaultEss.ControlCurrentOverload120PercentL2) // Control current overload 120%
								.faultBit(5, FaultEss.ControlCurrentOverload300PercentL2) // Control current overload 300%
								.faultBit(6, FaultEss.ControlTransientLoad300PercentL2) // Control transient load 300%
								.faultBit(7, FaultEss.GridOverCurrentL2) // Grid over current
								.faultBit(8, FaultEss.LockingWaveformTooManyTimesL2) // Locking waveform too many times
								.faultBit(9, FaultEss.InverterVoltageZeroDriftErrorL2) // Inverter voltage zero drift error
								.faultBit(10, FaultEss.GridVoltageZeroDriftErrorL2) // Grid voltage zero drift error
								.faultBit(11, FaultEss.ControlCurrentZeroDriftErrorL2) // Control current zero drift error
								.faultBit(12, FaultEss.InverterCurrentZeroDriftErrorL2) // Inverter current zero drift error
								.faultBit(13, FaultEss.GridCurrentZeroDriftErrorL2) // Grid current zero drift error
								.faultBit(14, FaultEss.PDPProtectionL2) // PDP protection
								.faultBit(15, FaultEss.HardwareControlCurrentProtectionL2)), // Hardware control current protection
						new UnsignedWordElement(158, new ModbusBitWrappingChannel("PcsFault2L2", this, state)//
								.faultBit(0, FaultEss.HardwareACVoltageProtectionL2) // Hardware AC volt. protection
								.faultBit(1, FaultEss.HardwareDCCurrentProtectionL2) // Hardware DC curr. protection
								.faultBit(2, FaultEss.HardwareTemperatureProtectionL2) // Hardware temperature protection
								.faultBit(3, FaultEss.NoCapturingSignalL2) // No capturing signal
								.faultBit(4, FaultEss.DCOvervoltageL2) // DC overvoltage
								.faultBit(5, FaultEss.DCDisconnectedL2) // DC disconnected
								.faultBit(6, FaultEss.InverterUndervoltageL2) // Inverter undervoltage
								.faultBit(7, FaultEss.InverterOvervoltageL2) // Inverter overvoltage
								.faultBit(8, FaultEss.CurrentSensorFailL2) // Current sensor fail
								.faultBit(9, FaultEss.VoltageSensorFailL2) // Voltage sensor fail
								.faultBit(10, FaultEss.PowerUncontrollableL2) // Power uncontrollable
								.faultBit(11, FaultEss.CurrentUncontrollableL2) // Current uncontrollable
								.faultBit(12, FaultEss.FanErrorL2) // Fan error
								.faultBit(13, FaultEss.PhaseLackL2) // Phase lack
								.faultBit(14, FaultEss.InverterRelayFaultL2) // Inverter relay fault
								.faultBit(15, FaultEss.GridRealyFaultL2)), // Grid relay fault
						new UnsignedWordElement(159, new ModbusBitWrappingChannel("PcsFault3L2", this, state)//
								.faultBit(0, FaultEss.ControlPanelOvertempL2) // Control panel overtemp
								.faultBit(1, FaultEss.PowerPanelOvertempL2) // Power panel overtemp
								.faultBit(2, FaultEss.DCInputOvercurrentL2) // DC input overcurrent
								.faultBit(3, FaultEss.CapacitorOvertempL2) // Capacitor overtemp
								.faultBit(4, FaultEss.RadiatorOvertempL2) // Radiator overtemp
								.faultBit(5, FaultEss.TransformerOvertempL2) // Transformer overtemp
								.faultBit(6, FaultEss.CombinationCommErrorL2) // Combination comm error
								.faultBit(7, FaultEss.EEPROMErrorL2) // EEPROM error
								.faultBit(8, FaultEss.LoadCurrentZeroDriftErrorL2) // Load current zero drift error
								.faultBit(9, FaultEss.CurrentLimitRErrorL2) // Current limit-R error
								.faultBit(10, FaultEss.PhaseSyncErrorL2) // Phase sync error
								.faultBit(11, FaultEss.ExternalPVCurrentZeroDriftErrorL2) // External PV current zero drift error
								.faultBit(12, FaultEss.ExternalGridCurrentZeroDriftErrorL2)), // External grid current zero drift error
						new UnsignedWordElement(160, new ModbusBitWrappingChannel("PcsAlarm1L3", this, state)//
								.warningBit(0, WarningEss.GridUndervoltageL3) // Grid undervoltage
								.warningBit(1, WarningEss.GridOvervoltageL3) // Grid overvoltage
								.warningBit(2, WarningEss.GridUnderFrequencyL3) // Grid under frequency
								.warningBit(3, WarningEss.GridOverFrequencyL3) // Grid over frequency
								.warningBit(4, WarningEss.GridPowerSupplyOffL3) // Grid power supply off
								.warningBit(5, WarningEss.GridConditionUnmeetL3) // Grid condition unmeet
								.warningBit(6, WarningEss.DCUnderVoltageL3) // DC under voltage
								.warningBit(7, WarningEss.InputOverResistanceL3) // Input over resistance
								.warningBit(8, WarningEss.CombinationErrorL3) // Combination error
								.warningBit(9, WarningEss.CommWithInverterErrorL3) // Comm with inverter error
								.warningBit(10, WarningEss.TmeErrorL3)), // Tme error
						new UnsignedWordElement(161, new ModbusBitWrappingChannel("PcsAlarm2L3", this, state)),
						new UnsignedWordElement(162, new ModbusBitWrappingChannel("PcsFault1L3", this, state)//
								.faultBit(0, FaultEss.ControlCurrentOverload100PercentL3) // Control current overload 100%
								.faultBit(1, FaultEss.ControlCurrentOverload110PercentL3) // Control current overload 110%
								.faultBit(2, FaultEss.ControlCurrentOverload150PercentL3) // Control current overload 150%
								.faultBit(3, FaultEss.ControlCurrentOverload200PercentL3) // Control current overload 200%
								.faultBit(4, FaultEss.ControlCurrentOverload120PercentL3) // Control current overload 120%
								.faultBit(5, FaultEss.ControlCurrentOverload300PercentL3) // Control current overload 300%
								.faultBit(6, FaultEss.ControlTransientLoad300PercentL3) // Control transient load 300%
								.faultBit(7, FaultEss.GridOverCurrentL3) // Grid over current
								.faultBit(8, FaultEss.LockingWaveformTooManyTimesL3) // Locking waveform too many times
								.faultBit(9, FaultEss.InverterVoltageZeroDriftErrorL3) // Inverter voltage zero drift error
								.faultBit(10, FaultEss.GridVoltageZeroDriftErrorL3) // Grid voltage zero drift error
								.faultBit(11, FaultEss.ControlCurrentZeroDriftErrorL3) // Control current zero drift error
								.faultBit(12, FaultEss.InverterCurrentZeroDriftErrorL3) // Inverter current zero drift error
								.faultBit(13, FaultEss.GridCurrentZeroDriftErrorL3) // Grid current zero drift error
								.faultBit(14, FaultEss.PDPProtectionL3) // PDP protection
								.faultBit(15, FaultEss.HardwareControlCurrentProtectionL3)), // Hardware control current protection
						new UnsignedWordElement(163, new ModbusBitWrappingChannel("PcsFault2L3", this, state)//
								.faultBit(0, FaultEss.HardwareACVoltageProtectionL3) // Hardware AC volt. protection
								.faultBit(1, FaultEss.HardwareDCCurrentProtectionL3) // Hardware DC curr. protection
								.faultBit(2, FaultEss.HardwareTemperatureProtectionL3) // Hardware temperature protection
								.faultBit(3, FaultEss.NoCapturingSignalL3) // No capturing signal
								.faultBit(4, FaultEss.DCOvervoltageL3) // DC overvoltage
								.faultBit(5, FaultEss.DCDisconnectedL3) // DC disconnected
								.faultBit(6, FaultEss.InverterUndervoltageL3) // Inverter undervoltage
								.faultBit(7, FaultEss.InverterOvervoltageL3) // Inverter overvoltage
								.faultBit(8, FaultEss.CurrentSensorFailL3) // Current sensor fail
								.faultBit(9, FaultEss.VoltageSensorFailL3) // Voltage sensor fail
								.faultBit(10, FaultEss.PowerUncontrollableL3) // Power uncontrollable
								.faultBit(11, FaultEss.CurrentUncontrollableL3) // Current uncontrollable
								.faultBit(12, FaultEss.FanErrorL3) // Fan error
								.faultBit(13, FaultEss.PhaseLackL3) // Phase lack
								.faultBit(14, FaultEss.InverterRelayFaultL3) // Inverter relay fault
								.faultBit(15, FaultEss.GridRealyFaultL3)), // Grid relay fault
						new UnsignedWordElement(164, new ModbusBitWrappingChannel("PcsFault3L3", this, state)//
								.faultBit(0, FaultEss.ControlPanelOvertempL3) // Control panel overtemp
								.faultBit(1, FaultEss.PowerPanelOvertempL3) // Power panel overtemp
								.faultBit(2, FaultEss.DCInputOvercurrentL3) // DC input overcurrent
								.faultBit(3, FaultEss.CapacitorOvertempL3) // Capacitor overtemp
								.faultBit(4, FaultEss.RadiatorOvertempL3) // Radiator overtemp
								.faultBit(5, FaultEss.TransformerOvertempL3) // Transformer overtemp
								.faultBit(6, FaultEss.CombinationCommErrorL3) // Combination comm error
								.faultBit(7, FaultEss.EEPROMErrorL3) // EEPROM error
								.faultBit(8, FaultEss.LoadCurrentZeroDriftErrorL3) // Load current zero drift error
								.faultBit(9, FaultEss.CurrentLimitRErrorL3) // Current limit-R error
								.faultBit(10, FaultEss.PhaseSyncErrorL3) // Phase sync error
								.faultBit(11, FaultEss.ExternalPVCurrentZeroDriftErrorL3) // External PV current zero drift error
								.faultBit(12, FaultEss.ExternalGridCurrentZeroDriftErrorL3))), // External grid current zero drift error
				new WriteableModbusRegisterRange(200, //
						new UnsignedWordElement(200, setWorkState = new ModbusWriteLongChannel("SetWorkState", this)//
						.label(0, "Local control") //
						.label(1, START) // "Remote control on grid starting"
						.label(2, "Remote control off grid starting") //
						.label(3, STOP)//
						.label(4, "Emergency Stop"))),
				new WriteableModbusRegisterRange(206, //
						new SignedWordElement(206,
								setActivePowerL1 = new ModbusWriteLongChannel("SetActivePowerL1", this).unit("W")), //
						new SignedWordElement(207,
								setReactivePowerL1 = new ModbusWriteLongChannel("SetReactivePowerL1", this)
								.unit("Var")), //
						new SignedWordElement(208,
								setActivePowerL2 = new ModbusWriteLongChannel("SetActivePowerL2", this).unit("W")), //
						new SignedWordElement(209,
								setReactivePowerL2 = new ModbusWriteLongChannel("SetReactivePowerL2", this)
								.unit("Var")), //
						new SignedWordElement(210,
								setActivePowerL3 = new ModbusWriteLongChannel("SetActivePowerL3", this).unit("W")), //
						new SignedWordElement(211,
								setReactivePowerL3 = new ModbusWriteLongChannel("SetReactivePowerL3", this).unit("Var")//
								)), //
				new ModbusRegisterRange(3020, new UnsignedWordElement(3020,
						batteryVoltageSection1 = new ModbusReadLongChannel("BatteryVoltageSection1", this).unit("mV")),
						new UnsignedWordElement(3021,
								batteryVoltageSection2 = new ModbusReadLongChannel("BatteryVoltageSection2", this)
								.unit("mV")),
						new UnsignedWordElement(3022,
								batteryVoltageSection3 = new ModbusReadLongChannel("BatteryVoltageSection3", this)
								.unit("mV")),
						new UnsignedWordElement(3023,
								batteryVoltageSection4 = new ModbusReadLongChannel("BatteryVoltageSection4", this)
								.unit("mV")),
						new UnsignedWordElement(3024,
								batteryVoltageSection5 = new ModbusReadLongChannel("BatteryVoltageSection5", this)
								.unit("mV")),
						new UnsignedWordElement(3025,
								batteryVoltageSection6 = new ModbusReadLongChannel("BatteryVoltageSection6", this)
								.unit("mV")),
						new UnsignedWordElement(3026,
								batteryVoltageSection7 = new ModbusReadLongChannel("BatteryVoltageSection7", this)
								.unit("mV")),
						new UnsignedWordElement(3027,
								batteryVoltageSection8 = new ModbusReadLongChannel("BatteryVoltageSection8", this)
								.unit("mV")),
						new UnsignedWordElement(3028,
								batteryVoltageSection9 = new ModbusReadLongChannel("BatteryVoltageSection9", this)
								.unit("mV")),
						new UnsignedWordElement(3029,
								batteryVoltageSection10 = new ModbusReadLongChannel("BatteryVoltageSection10", this)
								.unit("mV")),
						new UnsignedWordElement(3030,
								batteryVoltageSection11 = new ModbusReadLongChannel("BatteryVoltageSection11", this)
								.unit("mV")),
						new UnsignedWordElement(3031,
								batteryVoltageSection12 = new ModbusReadLongChannel("BatteryVoltageSection12", this)
								.unit("mV")),
						new UnsignedWordElement(3032,
								batteryVoltageSection13 = new ModbusReadLongChannel("BatteryVoltageSection13", this)
								.unit("mV")),
						new UnsignedWordElement(3033,
								batteryVoltageSection14 = new ModbusReadLongChannel("BatteryVoltageSection14", this)
								.unit("mV")),
						new UnsignedWordElement(3034,
								batteryVoltageSection15 = new ModbusReadLongChannel("BatteryVoltageSection15", this)
								.unit("mV")),
						new UnsignedWordElement(3035,
								batteryVoltageSection16 = new ModbusReadLongChannel("BatteryVoltageSection16", this)
								.unit("mV")),
						new UnsignedWordElement(3036,
								batteryTemperatureSection1 = new ModbusReadLongChannel("BatteryTemperatureSection1",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3037,
								batteryTemperatureSection2 = new ModbusReadLongChannel("BatteryTemperatureSection2",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3038,
								batteryTemperatureSection3 = new ModbusReadLongChannel("BatteryTemperatureSection3",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3039,
								batteryTemperatureSection4 = new ModbusReadLongChannel("BatteryTemperatureSection4",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3040,
								batteryTemperatureSection5 = new ModbusReadLongChannel("BatteryTemperatureSection5",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3041,
								batteryTemperatureSection6 = new ModbusReadLongChannel("BatteryTemperatureSection6",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3042,
								batteryTemperatureSection7 = new ModbusReadLongChannel("BatteryTemperatureSection7",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3043,
								batteryTemperatureSection8 = new ModbusReadLongChannel("BatteryTemperatureSection8",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3044,
								batteryTemperatureSection9 = new ModbusReadLongChannel("BatteryTemperatureSection9",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3045,
								batteryTemperatureSection10 = new ModbusReadLongChannel("BatteryTemperatureSection10",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3046,
								batteryTemperatureSection11 = new ModbusReadLongChannel("BatteryTemperatureSection11",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3047,
								batteryTemperatureSection12 = new ModbusReadLongChannel("BatteryTemperatureSection12",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3048,
								batteryTemperatureSection13 = new ModbusReadLongChannel("BatteryTemperatureSection13",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3049,
								batteryTemperatureSection14 = new ModbusReadLongChannel("BatteryTemperatureSection14",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3050,
								batteryTemperatureSection15 = new ModbusReadLongChannel("BatteryTemperatureSection15",
										this).unit("°C").delta(-40L)),
						new UnsignedWordElement(3051,
								batteryTemperatureSection16 = new ModbusReadLongChannel("BatteryTemperatureSection16",
										this).unit("°C").delta(-40L))),
				new WriteableModbusRegisterRange(9014, //
						new UnsignedWordElement(9014, rtcYear = new ModbusWriteLongChannel("Year", this)),
						new UnsignedWordElement(9015, rtcMonth = new ModbusWriteLongChannel("Month", this)),
						new UnsignedWordElement(9016, rtcDay = new ModbusWriteLongChannel("Day", this)),
						new UnsignedWordElement(9017, rtcHour = new ModbusWriteLongChannel("Hour", this)),
						new UnsignedWordElement(9018, rtcMinute = new ModbusWriteLongChannel("Minute", this)),
						new UnsignedWordElement(9019, rtcSecond = new ModbusWriteLongChannel("Second", this))),
				new WriteableModbusRegisterRange(30558,
						new UnsignedWordElement(30558,
								setSetupMode = new ModbusWriteLongChannel("SetSetupMode", this).label(0, EssNature.OFF)
								.label(1, EssNature.ON))),
				new WriteableModbusRegisterRange(30559,
						new UnsignedWordElement(30559, setPcsMode = new ModbusWriteLongChannel("SetPcsMode", this)//
						.label(0, "Emergency")//
						.label(1, "ConsumersPeakPattern")//
						.label(2, "Economic")//
						.label(3, "Eco")//
						.label(4, "Debug")//
						.label(5, "SmoothPv")//
						.label(6, "Remote"))),
				new ModbusRegisterRange(30157,
						new UnsignedWordElement(30157, setupMode = new ModbusReadLongChannel("SetupMode", this)//
						.label(0, EssNature.OFF)//
						.label(1, EssNature.ON)),
						new UnsignedWordElement(30158, pcsMode = new ModbusReadLongChannel("PcsMode", this)//
						.label(0, "Emergency")//
						.label(1, "ConsumersPeakPattern")//
						.label(2, "Economic")//
						.label(3, "Eco")//
						.label(4, "Debug")//
						.label(5, "SmoothPv")//
						.label(6, "Remote"))));
		gridMode = new FunctionalReadChannel<Long>("GridMode", this, (channels) -> {
			ReadChannel<Long> state = channels[0];
			try {
				if (state.value() == 1L) {
					return 0L;
				} else {
					return 1L;
				}
			} catch (InvalidValueException e) {
				return null;
			}
		}, systemState).label(0L, OFF_GRID).label(1L, ON_GRID);
		allowedApparent = new FunctionalReadChannel<Long>("AllowedApparent", this, (channels) -> {
			ReadChannel<Long> apparent = channels[0];
			try {
				return apparent.value() * 3;
			} catch (InvalidValueException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0l;
		}, phaseAllowedApparent);

		// FaultChannels
		state.addFaultChannel(new ValueToBooleanChannel(FaultEss.SystemFault.getChannelId(), this, systemState, 3L));
		state.addFaultChannel(new ValueToBooleanChannel(FaultEss.BatteryFault.getChannelId(), this, batteryGroupState, 5L));
		state.addFaultChannel(new ValueToBooleanChannel(FaultEss.PCSFault.getChannelId(), this, pcsOperationState, 5L));
		// WarningChannels
		state.addWarningChannel(new ValueToBooleanChannel(WarningEss.OFFGrid.getChannelId(), this, systemState, 1L));

		return protokol;
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return state;
	}

}
