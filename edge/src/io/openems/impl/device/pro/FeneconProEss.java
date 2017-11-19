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

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.realtimeclock.RealTimeClockNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
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
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this);
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
	// ESS
	private StatusBitChannels warning;
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
	public StatusBitChannels warning() {
		return warning;
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
	public ModbusReadLongChannel batteryGroupAlarm;
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
	public StatusBitChannel pcsAlarm1L1;
	public StatusBitChannel pcsAlarm2L1;
	public StatusBitChannel pcsFault1L1;
	public StatusBitChannel pcsFault2L1;
	public StatusBitChannel pcsFault3L1;
	public StatusBitChannel pcsAlarm1L2;
	public StatusBitChannel pcsAlarm2L2;
	public StatusBitChannel pcsFault1L2;
	public StatusBitChannel pcsFault2L2;
	public StatusBitChannel pcsFault3L2;
	public StatusBitChannel pcsAlarm1L3;
	public StatusBitChannel pcsAlarm2L3;
	public StatusBitChannel pcsFault1L3;
	public StatusBitChannel pcsFault2L3;
	public StatusBitChannel pcsFault3L3;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		warning = new StatusBitChannels("Warning", this);

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
				new UnsignedWordElement(113, //
						batteryGroupAlarm = new ModbusReadLongChannel("BatteryGroupAlarm", this)
						.label(1, "Fail, The system should be stopped") //
						.label(2, "Common low voltage alarm") //
						.label(4, "Common high voltage alarm") //
						.label(8, "Charging over current alarm") //
						.label(16, "Discharging over current alarm") //
						.label(32, "Over temperature alarm")//
						.label(64, "Interal communication abnormal")),
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
						new UnsignedWordElement(150,
								pcsAlarm1L1 = warning.channel(new StatusBitChannel("PcsAlarm1L1", this)//
										.label(1, "Grid undervoltage") //
										.label(2, "Grid overvoltage") //
										.label(4, "Grid under frequency") //
										.label(8, "Grid over frequency") //
										.label(16, "Grid power supply off") //
										.label(32, "Grid condition unmeet")//
										.label(64, "DC under voltage")//
										.label(128, "Input over resistance")//
										.label(256, "Combination error")//
										.label(512, "Comm with inverter error")//
										.label(1024, "Tme error")//
										)), new UnsignedWordElement(151,
												pcsAlarm2L1 = warning.channel(new StatusBitChannel("PcsAlarm2L1", this)//
														)),
						new UnsignedWordElement(152,
								warning.channel(pcsFault1L1 = new StatusBitChannel("PcsFault1L1", this)//
								.label(1, "Control current overload 100%")//
								.label(2, "Control current overload 110%")//
								.label(4, "Control current overload 150%")//
								.label(8, "Control current overload 200%")//
								.label(16, "Control current overload 120%")//
								.label(32, "Control current overload 300%")//
								.label(64, "Control transient load 300%")//
								.label(128, "Grid over current")//
								.label(256, "Locking waveform too many times")//
								.label(512, "Inverter voltage zero drift error")//
								.label(1024, "Grid voltage zero drift error")//
								.label(2048, "Control current zero drift error")//
								.label(4096, "Inverter current zero drift error")//
								.label(8192, "Grid current zero drift error")//
								.label(16384, "PDP protection")//
								.label(32768, "Hardware control current protection")//
										)),
						new UnsignedWordElement(153,
								warning.channel(pcsFault2L1 = new StatusBitChannel("PcsFault2L1", this)//
								.label(1, "Hardware AC volt. protection")//
								.label(2, "Hardware DC curr. protection")//
								.label(4, "Hardware temperature protection")//
								.label(8, "No capturing signal")//
								.label(16, "DC overvoltage")//
								.label(32, "DC disconnected")//
								.label(64, "Inverter undervoltage")//
								.label(128, "Inverter overvoltage")//
								.label(256, "Current sensor fail")//
								.label(512, "Voltage sensor fail")//
								.label(1024, "Power uncontrollable")//
								.label(2048, "Current uncontrollable")//
								.label(4096, "Fan error")//
								.label(8192, "Phase lack")//
								.label(16384, "Inverter relay fault")//
								.label(32768, "Grid relay fault")//
										)),
						new UnsignedWordElement(154,
								warning.channel(pcsFault3L1 = new StatusBitChannel("PcsFault3L1", this)//
								.label(1, "Control panel overtemp")//
								.label(2, "Power panel overtemp")//
								.label(4, "DC input overcurrent")//
								.label(8, "Capacitor overtemp")//
								.label(16, "Radiator overtemp")//
								.label(32, "Transformer overtemp")//
								.label(64, "Combination comm error")//
								.label(128, "EEPROM error")//
								.label(256, "Load current zero drift error")//
								.label(512, "Current limit-R error")//
								.label(1024, "Phase sync error")//
								.label(2048, "External PV current zero drift error")//
								.label(4096, "External grid current zero drift error")//
										)),
						new UnsignedWordElement(155,
								warning.channel(pcsAlarm1L2 = new StatusBitChannel("PcsAlarm1L2", this)//
								.label(1, "Grid undervoltage") //
								.label(2, "Grid overvoltage") //
								.label(4, "Grid under frequency") //
								.label(8, "Grid over frequency") //
								.label(16, "Grid power supply off") //
								.label(32, "Grid condition unmeet")//
								.label(64, "DC under voltage")//
								.label(128, "Input over resistance")//
								.label(256, "Combination error")//
								.label(512, "Comm with inverter error")//
								.label(1024, "Tme error")//
										)), new UnsignedWordElement(156,
												warning.channel(pcsAlarm2L2 = new StatusBitChannel("PcsAlarm2L2", this)//
														)),
						new UnsignedWordElement(157,
								warning.channel(pcsFault1L2 = new StatusBitChannel("PcsFault1L2", this)//
								.label(1, "Control current overload 100%")//
								.label(2, "Control current overload 110%")//
								.label(4, "Control current overload 150%")//
								.label(8, "Control current overload 200%")//
								.label(16, "Control current overload 120%")//
								.label(32, "Control current overload 300%")//
								.label(64, "Control transient load 300%")//
								.label(128, "Grid over current")//
								.label(256, "Locking waveform too many times")//
								.label(512, "Inverter voltage zero drift error")//
								.label(1024, "Grid voltage zero drift error")//
								.label(2048, "Control current zero drift error")//
								.label(4096, "Inverter current zero drift error")//
								.label(8192, "Grid current zero drift error")//
								.label(16384, "PDP protection")//
								.label(32768, "Hardware control current protection")//
										)),
						new UnsignedWordElement(158,
								warning.channel(pcsFault2L2 = new StatusBitChannel("PcsFault2L2", this)//
								.label(1, "Hardware AC volt. protection")//
								.label(2, "Hardware DC curr. protection")//
								.label(4, "Hardware temperature protection")//
								.label(8, "No capturing signal")//
								.label(16, "DC overvoltage")//
								.label(32, "DC disconnected")//
								.label(64, "Inverter undervoltage")//
								.label(128, "Inverter overvoltage")//
								.label(256, "Current sensor fail")//
								.label(512, "Voltage sensor fail")//
								.label(1024, "Power uncontrollable")//
								.label(2048, "Current uncontrollable")//
								.label(4096, "Fan error")//
								.label(8192, "Phase lack")//
								.label(16384, "Inverter relay fault")//
								.label(32768, "Grid relay fault")//
										)),
						new UnsignedWordElement(159,
								warning.channel(pcsFault3L2 = new StatusBitChannel("PcsFault3L2", this)//
								.label(1, "Control panel overtemp")//
								.label(2, "Power panel overtemp")//
								.label(4, "DC input overcurrent")//
								.label(8, "Capacitor overtemp")//
								.label(16, "Radiator overtemp")//
								.label(32, "Transformer overtemp")//
								.label(64, "Combination comm error")//
								.label(128, "EEPROM error")//
								.label(256, "Load current zero drift error")//
								.label(512, "Current limit-R error")//
								.label(1024, "Phase sync error")//
								.label(2048, "External PV current zero drift error")//
								.label(4096, "External grid current zero drift error")//
										)),
						new UnsignedWordElement(160,
								warning.channel(pcsAlarm1L3 = new StatusBitChannel("PcsAlarm1L3", this)//
								.label(1, "Grid undervoltage") //
								.label(2, "Grid overvoltage") //
								.label(4, "Grid under frequency") //
								.label(8, "Grid over frequency") //
								.label(16, "Grid power supply off") //
								.label(32, "Grid condition unmeet")//
								.label(64, "DC under voltage")//
								.label(128, "Input over resistance")//
								.label(256, "Combination error")//
								.label(512, "Comm with inverter error")//
								.label(1024, "Tme error")//
										)), new UnsignedWordElement(161,
												warning.channel(pcsAlarm2L3 = new StatusBitChannel("PcsAlarm2L3", this)//
														)),
						new UnsignedWordElement(162,
								warning.channel(pcsFault1L3 = new StatusBitChannel("PcsFault1L3", this)//
								.label(1, "Control current overload 100%")//
								.label(2, "Control current overload 110%")//
								.label(4, "Control current overload 150%")//
								.label(8, "Control current overload 200%")//
								.label(16, "Control current overload 120%")//
								.label(32, "Control current overload 300%")//
								.label(64, "Control transient load 300%")//
								.label(128, "Grid over current")//
								.label(256, "Locking waveform too many times")//
								.label(512, "Inverter voltage zero drift error")//
								.label(1024, "Grid voltage zero drift error")//
								.label(2048, "Control current zero drift error")//
								.label(4096, "Inverter current zero drift error")//
								.label(8192, "Grid current zero drift error")//
								.label(16384, "PDP protection")//
								.label(32768, "Hardware control current protection")//
										)),
						new UnsignedWordElement(163,
								warning.channel(pcsFault2L3 = new StatusBitChannel("PcsFault2L3", this)//
								.label(1, "Hardware AC volt. protection")//
								.label(2, "Hardware DC curr. protection")//
								.label(4, "Hardware temperature protection")//
								.label(8, "No capturing signal")//
								.label(16, "DC overvoltage")//
								.label(32, "DC disconnected")//
								.label(64, "Inverter undervoltage")//
								.label(128, "Inverter overvoltage")//
								.label(256, "Current sensor fail")//
								.label(512, "Voltage sensor fail")//
								.label(1024, "Power uncontrollable")//
								.label(2048, "Current uncontrollable")//
								.label(4096, "Fan error")//
								.label(8192, "Phase lack")//
								.label(16384, "Inverter relay fault")//
								.label(32768, "Grid relay fault")//
										)),
						new UnsignedWordElement(164,
								warning.channel(pcsFault3L3 = new StatusBitChannel("PcsFault3L3", this)//
								.label(1, "Control panel overtemp")//
								.label(2, "Power panel overtemp")//
								.label(4, "DC input overcurrent")//
								.label(8, "Capacitor overtemp")//
								.label(16, "Radiator overtemp")//
								.label(32, "Transformer overtemp")//
								.label(64, "Combination comm error")//
								.label(128, "EEPROM error")//
								.label(256, "Load current zero drift error")//
								.label(512, "Current limit-R error")//
								.label(1024, "Phase sync error")//
								.label(2048, "External PV current zero drift error")//
								.label(4096, "External grid current zero drift error")//
										))), //
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

}
