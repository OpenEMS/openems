/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.realtimeclock.RealTimeClockNature;
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

public class FeneconProEss extends ModbusDeviceNature
		implements AsymmetricEssNature, RealTimeClockNature, ChannelUpdateListener {

	public FeneconProEss(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this, Integer.class)
			.addUpdateListener(this);

	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this, Integer.class).optional();

	@Override
	public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		// If chargeSoc was not set -> set it to minSoc minus 2
		if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
			chargeSoc.updateValue((Integer) newValue.get() - 2, false);
		}
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
	private ModbusReadLongChannel allowedApparent;
	private ModbusReadLongChannel reactivePowerL1;
	private ModbusReadLongChannel reactivePowerL2;
	private ModbusReadLongChannel reactivePowerL3;
	// RealTimeClock
	private ModbusWriteLongChannel rtcYear;
	private ModbusWriteLongChannel rtcMonth;
	private ModbusWriteLongChannel rtcDay;
	private ModbusWriteLongChannel rtcHour;
	private ModbusWriteLongChannel rtcMinute;
	private ModbusWriteLongChannel rtcSecond;

	private ModbusWriteLongChannel setWorkState;
	private ModbusWriteLongChannel setActivePowerL1;
	private ModbusWriteLongChannel setActivePowerL2;
	private ModbusWriteLongChannel setActivePowerL3;
	private ModbusWriteLongChannel setReactivePowerL1;
	private ModbusWriteLongChannel setReactivePowerL2;
	private ModbusWriteLongChannel setReactivePowerL3;

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

	@Override
	public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	/*
	 * This Channels
	 */
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
	public ModbusReadLongChannel batteryGroupState;
	public ModbusReadLongChannel totalBatteryDischargeEnergy;
	public ModbusReadLongChannel totalBatteryChargeEnergy;
	public ModbusReadLongChannel workMode;
	public ModbusReadLongChannel controlMode;
	public ModbusWriteLongChannel setPcsMode;
	public ModbusWriteLongChannel setSetupMode;
	public ModbusReadLongChannel setupMode;
	public ModbusReadLongChannel pcsMode;

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		warning = new StatusBitChannels("Warning", this);

		ModbusProtocol protokol = new ModbusProtocol(new ModbusRegisterRange(100, //
				new UnsignedWordElement(100, //
						systemState = new ModbusReadLongChannel("SystemState", this) //
								.label(0, STANDBY) //
								.label(1, EssNature.OFF_GRID) //
								.label(2, EssNature.ON_GRID) //
								.label(3, "Fail") //
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
				new UnsignedWordElement(111, //
						batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this).unit("mA").multiplier(2)),
				new UnsignedWordElement(112, //
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
				new DummyElement(130),
				new UnsignedWordElement(131, //
						frequencyL1 = new ModbusReadLongChannel("FrequencyL1", this).unit("mHz").multiplier(1)),
				new UnsignedWordElement(132, //
						frequencyL2 = new ModbusReadLongChannel("FrequencyL2", this).unit("mHz").multiplier(1)),
				new UnsignedWordElement(133, //
						frequencyL3 = new ModbusReadLongChannel("FrequencyL3", this).unit("mHz").multiplier(1)),
				new UnsignedWordElement(134, //
						allowedApparent = new ModbusReadLongChannel("AllowedApparentPower", this).unit("VA")),
				new DummyElement(135, 140),
				new UnsignedWordElement(141, //
						allowedCharge = new ModbusReadLongChannel("AllowedCharge", this).unit("W")),
				new UnsignedWordElement(142, //
						allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this).unit("W")),
				new DummyElement(143, 149),
				new UnsignedWordElement(150,
						warning.channel(new StatusBitChannel("PcsAlarm1L1", this)//
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
						)), new UnsignedWordElement(151, warning.channel(new StatusBitChannel("PcsAlarm2L1", this)//
				)), new UnsignedWordElement(152, warning.channel(new StatusBitChannel("PcsFault1L1", this)//
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
				)), new UnsignedWordElement(153, warning.channel(new StatusBitChannel("PcsFault2L1", this)//
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
				)), new UnsignedWordElement(154, warning.channel(new StatusBitChannel("PcsFault3L1", this)//
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
				)), new UnsignedWordElement(155, warning.channel(new StatusBitChannel("PcsAlarm1L2", this)//
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
				)), new UnsignedWordElement(156, warning.channel(new StatusBitChannel("PcsAlarm2L2", this)//
				)), new UnsignedWordElement(157, warning.channel(new StatusBitChannel("PcsFault1L2", this)//
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
				)), new UnsignedWordElement(158, warning.channel(new StatusBitChannel("PcsFault2L2", this)//
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
				)), new UnsignedWordElement(159, warning.channel(new StatusBitChannel("PcsFault3L2", this)//
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
				)), new UnsignedWordElement(160, warning.channel(new StatusBitChannel("PcsAlarm1L3", this)//
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
				)), new UnsignedWordElement(161, warning.channel(new StatusBitChannel("PcsAlarm2L3", this)//
				)), new UnsignedWordElement(162, warning.channel(new StatusBitChannel("PcsFault1L3", this)//
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
				)), new UnsignedWordElement(163, warning.channel(new StatusBitChannel("PcsFault2L3", this)//
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
				)), new UnsignedWordElement(164, warning.channel(new StatusBitChannel("PcsFault3L3", this)//
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
						new UnsignedWordElement(200,
								setWorkState = new ModbusWriteLongChannel("SetWorkState", this)//
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
						new UnsignedWordElement(30559,
								setPcsMode = new ModbusWriteLongChannel("SetPcsMode", this)//
										.label(0, "Emergency")//
										.label(1, "ConsumersPeakPattern")//
										.label(2, "Economic")//
										.label(3, "Eco")//
										.label(4, "Debug")//
										.label(5, "SmoothPv")//
										.label(6, "Remote"))),
				new ModbusRegisterRange(30157,
						new UnsignedWordElement(30157,
								setupMode = new ModbusReadLongChannel("SetupMode", this)//
										.label(0, EssNature.OFF)//
										.label(1, EssNature.ON)),
						new UnsignedWordElement(30158,
								pcsMode = new ModbusReadLongChannel("PcsMode", this)//
										.label(0, "Emergency")//
										.label(1, "ConsumersPeakPattern")//
										.label(2, "Economic")//
										.label(3, "Eco")//
										.label(4, "Debug")//
										.label(5, "SmoothPv")//
										.label(6, "Remote")))

		// new DummyElement(143, 149),
		// new ElementBuilder().address(150).channel(_pcsAlarm1PhaseA).build(), //
		// new ElementBuilder().address(151).channel(_pcsAlarm2PhaseA).build(), //
		// new ElementBuilder().address(152).channel(_pcsFault1PhaseA).build(), //
		// new ElementBuilder().address(153).channel(_pcsFault2PhaseA).build(), //
		// new ElementBuilder().address(154).channel(_pcsFault3PhaseA).build(), //
		// new ElementBuilder().address(155).channel(_pcsAlarm1PhaseB).build(), //
		// new ElementBuilder().address(156).channel(_pcsAlarm2PhaseB).build(), //
		// new ElementBuilder().address(157).channel(_pcsFault1PhaseB).build(), //
		// new ElementBuilder().address(158).channel(_pcsFault2PhaseB).build(), //
		// new ElementBuilder().address(159).channel(_pcsFault3PhaseB).build(), //
		// new ElementBuilder().address(160).channel(_pcsAlarm1PhaseC).build(), //
		// new ElementBuilder().address(161).channel(_pcsAlarm2PhaseC).build(), //
		// new ElementBuilder().address(162).channel(_pcsFault1PhaseC).build(), //
		// new ElementBuilder().address(163).channel(_pcsFault2PhaseC).build(), //
		// new ElementBuilder().address(164).channel(_pcsFault3PhaseC).build()), //

		//
		);
		gridMode = new FunctionalChannel<Long>("GridMode", this, (channels) -> {
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

		return protokol;
	}

	// @IsChannel(id = "PcsAlarm1PhaseA")
	// public final ModbusChannel _pcsAlarm1PhaseA = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Grid undervoltage") //
	// .label(2, "Grid overvoltage") //
	// .label(4, "Grid under frequency") //
	// .label(8, "Grid over frequency") //
	// .label(16, "Grid power supply off") //
	// .label(32, "Grid condition unmeet")//
	// .label(64, "DC under voltage")//
	// .label(128, "Input over resistance")//
	// .label(256, "Combination error")//
	// .label(512, "Comm with inverter error")//
	// .label(1024, "Tme error")//
	// .build();
	// @IsChannel(id = "PcsAlarm2PhaseA")
	// public final ModbusChannel _pcsAlarm2PhaseA = new ModbusChannelBuilder().nature(this) //
	// .build();
	// @IsChannel(id = "PcsFault1PhaseA")
	// public final ModbusChannel _pcsFault1PhaseA = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Control current overload 100%")//
	// .label(2, "Control current overload 110%")//
	// .label(4, "Control current overload 150%")//
	// .label(8, "Control current overload 200%")//
	// .label(16, "Control current overload 120%")//
	// .label(32, "Control current overload 300%")//
	// .label(64, "Control transient load 300%")//
	// .label(128, "Grid over current")//
	// .label(256, "Locking waveform too many times")//
	// .label(512, "Inverter voltage zero drift error")//
	// .label(1024, "Grid voltage zero drift error")//
	// .label(2048, "Control current zero drift error")//
	// .label(4096, "Inverter current zero drift error")//
	// .label(8192, "Grid current zero drift error")//
	// .label(16384, "PDP protection")//
	// .label(32768, "Hardware control current protection")//
	// .build();
	// @IsChannel(id = "PcsFault2PhaseA")
	// public final ModbusChannel _pcsFault2PhaseA = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Hardware AC volt. protection")//
	// .label(2, "Hardware DC curr. protection")//
	// .label(4, "Hardware temperature protection")//
	// .label(8, "No capturing signal")//
	// .label(16, "DC overvoltage")//
	// .label(32, "DC disconnected")//
	// .label(64, "Inverter undervoltage")//
	// .label(128, "Inverter overvoltage")//
	// .label(256, "Current sensor fail")//
	// .label(512, "Voltage sensor fail")//
	// .label(1024, "Power uncontrollable")//
	// .label(2048, "Current uncontrollable")//
	// .label(4096, "Fan error")//
	// .label(8192, "Phase lack")//
	// .label(16384, "Inverter relay fault")//
	// .label(32768, "Grid relay fault")//
	// .build();
	// @IsChannel(id = "PcsFault3PhaseA")
	// public final ModbusChannel _pcsFault3PhaseA = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Control panel overtemp")//
	// .label(2, "Power panel overtemp")//
	// .label(4, "DC input overcurrent")//
	// .label(8, "Capacitor overtemp")//
	// .label(16, "Radiator overtemp")//
	// .label(32, "Transformer overtemp")//
	// .label(64, "Combination comm error")//
	// .label(128, "EEPROM error")//
	// .label(256, "Load current zero drift error")//
	// .label(512, "Current limit-R error")//
	// .label(1024, "Phase sync error")//
	// .label(2048, "External PV current zero drift error")//
	// .label(4096, "External grid current zero drift error")//
	// .build();
	// @IsChannel(id = "PcsAlarm1PhaseB")
	// public final ModbusChannel _pcsAlarm1PhaseB = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Grid undervoltage") //
	// .label(2, "Grid overvoltage") //
	// .label(4, "Grid under frequency") //
	// .label(8, "Grid over frequency") //
	// .label(16, "Grid power supply off") //
	// .label(32, "Grid condition unmeet")//
	// .label(64, "DC under voltage")//
	// .label(128, "Input over resistance")//
	// .label(256, "Combination error")//
	// .label(512, "Comm with inverter error")//
	// .label(1024, "Tme error")//
	// .build();
	// @IsChannel(id = "PcsAlarm2PhaseB")
	// public final ModbusChannel _pcsAlarm2PhaseB = new ModbusChannelBuilder().nature(this) //
	// .build();
	// @IsChannel(id = "PcsFault1PhaseB")
	// public final ModbusChannel _pcsFault1PhaseB = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Control current overload 100%")//
	// .label(2, "Control current overload 110%")//
	// .label(4, "Control current overload 150%")//
	// .label(8, "Control current overload 200%")//
	// .label(16, "Control current overload 120%")//
	// .label(32, "Control current overload 300%")//
	// .label(64, "Control transient load 300%")//
	// .label(128, "Grid over current")//
	// .label(256, "Locking waveform too many times")//
	// .label(512, "Inverter voltage zero drift error")//
	// .label(1024, "Grid voltage zero drift error")//
	// .label(2048, "Control current zero drift error")//
	// .label(4096, "Inverter current zero drift error")//
	// .label(8192, "Grid current zero drift error")//
	// .label(16384, "PDP protection")//
	// .label(32768, "Hardware control current protection")//
	// .build();
	// @IsChannel(id = "PcsFault2PhaseB")
	// public final ModbusChannel _pcsFault2PhaseB = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Hardware AC volt. protection")//
	// .label(2, "Hardware DC curr. protection")//
	// .label(4, "Hardware temperature protection")//
	// .label(8, "No capturing signal")//
	// .label(16, "DC overvoltage")//
	// .label(32, "DC disconnected")//
	// .label(64, "Inverter undervoltage")//
	// .label(128, "Inverter overvoltage")//
	// .label(256, "Current sensor fail")//
	// .label(512, "Voltage sensor fail")//
	// .label(1024, "Power uncontrollable")//
	// .label(2048, "Current uncontrollable")//
	// .label(4096, "Fan error")//
	// .label(8192, "Phase lack")//
	// .label(16384, "Inverter relay fault")//
	// .label(32768, "Grid relay fault")//
	// .build();
	// @IsChannel(id = "PcsFault3PhaseB")
	// public final ModbusChannel _pcsFault3PhaseB = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Control panel overtemp")//
	// .label(2, "Power panel overtemp")//
	// .label(4, "DC input overcurrent")//
	// .label(8, "Capacitor overtemp")//
	// .label(16, "Radiator overtemp")//
	// .label(32, "Transformer overtemp")//
	// .label(64, "Combination comm error")//
	// .label(128, "EEPROM error")//
	// .label(256, "Load current zero drift error")//
	// .label(512, "Current limit-R error")//
	// .label(1024, "Phase sync error")//
	// .label(2048, "External PV current zero drift error")//
	// .label(4096, "External grid current zero drift error")//
	// .build();
	// @IsChannel(id = "PcsAlarm1PhaseC")
	// public final ModbusChannel _pcsAlarm1PhaseC = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Grid undervoltage") //
	// .label(2, "Grid overvoltage") //
	// .label(4, "Grid under frequency") //
	// .label(8, "Grid over frequency") //
	// .label(16, "Grid power supply off") //
	// .label(32, "Grid condition unmeet")//
	// .label(64, "DC under voltage")//
	// .label(128, "Input over resistance")//
	// .label(256, "Combination error")//
	// .label(512, "Comm with inverter error")//
	// .label(1024, "Tme error")//
	// .build();
	// @IsChannel(id = "PcsAlarm2PhaseC")
	// public final ModbusChannel _pcsAlarm2PhaseC = new ModbusChannelBuilder().nature(this) //
	// .build();
	// @IsChannel(id = "PcsFault1PhaseC")
	// public final ModbusChannel _pcsFault1PhaseC = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Control current overload 100%")//
	// .label(2, "Control current overload 110%")//
	// .label(4, "Control current overload 150%")//
	// .label(8, "Control current overload 200%")//
	// .label(16, "Control current overload 120%")//
	// .label(32, "Control current overload 300%")//
	// .label(64, "Control transient load 300%")//
	// .label(128, "Grid over current")//
	// .label(256, "Locking waveform too many times")//
	// .label(512, "Inverter voltage zero drift error")//
	// .label(1024, "Grid voltage zero drift error")//
	// .label(2048, "Control current zero drift error")//
	// .label(4096, "Inverter current zero drift error")//
	// .label(8192, "Grid current zero drift error")//
	// .label(16384, "PDP protection")//
	// .label(32768, "Hardware control current protection")//
	// .build();
	// @IsChannel(id = "PcsFault2PhaseC")
	// public final ModbusChannel _pcsFault2PhaseC = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Hardware AC volt. protection")//
	// .label(2, "Hardware DC curr. protection")//
	// .label(4, "Hardware temperature protection")//
	// .label(8, "No capturing signal")//
	// .label(16, "DC overvoltage")//
	// .label(32, "DC disconnected")//
	// .label(64, "Inverter undervoltage")//
	// .label(128, "Inverter overvoltage")//
	// .label(256, "Current sensor fail")//
	// .label(512, "Voltage sensor fail")//
	// .label(1024, "Power uncontrollable")//
	// .label(2048, "Current uncontrollable")//
	// .label(4096, "Fan error")//
	// .label(8192, "Phase lack")//
	// .label(16384, "Inverter relay fault")//
	// .label(32768, "Grid relay fault")//
	// .build();
	// @IsChannel(id = "PcsFault3PhaseC")
	// public final ModbusChannel _pcsFault3PhaseC = new ModbusChannelBuilder().nature(this) //
	// .label(1, "Control panel overtemp")//
	// .label(2, "Power panel overtemp")//
	// .label(4, "DC input overcurrent")//
	// .label(8, "Capacitor overtemp")//
	// .label(16, "Radiator overtemp")//
	// .label(32, "Transformer overtemp")//
	// .label(64, "Combination comm error")//
	// .label(128, "EEPROM error")//
	// .label(256, "Load current zero drift error")//
	// .label(512, "Current limit-R error")//
	// .label(1024, "Phase sync error")//
	// .label(2048, "External PV current zero drift error")//
	// .label(4096, "External grid current zero drift error")//
	// .build();

	// private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").build();
	// private final NumericChannel _activePower = new NumericChannelBuilder<>().nature(this).unit("W")
	// .channel(_activePowerPhaseA, _activePowerPhaseB, _activePowerPhaseC).aggregate(Aggregation.SUM).build();
	// private final NumericChannel _reactivePower = new NumericChannelBuilder<>().nature(this).unit("W")
	// .channel(_reactivePowerPhaseA, _reactivePowerPhaseB, _reactivePowerPhaseC).aggregate(Aggregation.SUM)
	// .build();

	// private final WriteableModbusChannel _setWorkState = new WriteableModbusChannelBuilder().nature(this) //
	// .label(0, "Local control") //
	// .label(1, START) // "Remote control on grid starting"
	// .label(2, "Remote control off grid starting") //
	// .label(3, STOP)//
	// .label(4, "Emergency Stop").build();
	// public final WriteableModbusChannel _setActivePowerPhaseA = new WriteableModbusChannelBuilder().nature(this)
	// .unit("W").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	// public final WriteableModbusChannel _setActivePowerPhaseB = new WriteableModbusChannelBuilder().nature(this)
	// .unit("W").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	// public final WriteableModbusChannel _setActivePowerPhaseC = new WriteableModbusChannelBuilder().nature(this)
	// .unit("W").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	// public final WriteableModbusChannel _setReactivePowerPhaseA = new WriteableModbusChannelBuilder().nature(this)
	// .unit("var").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	// public final WriteableModbusChannel _setReactivePowerPhaseB = new WriteableModbusChannelBuilder().nature(this)
	// .unit("var").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	// public final WriteableModbusChannel _setReactivePowerPhaseC = new WriteableModbusChannelBuilder().nature(this)
	// .unit("var").minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	// private final ConfigChannel _minSoc = new ConfigChannelBuilder().nature(this).defaultValue(DEFAULT_MINSOC)
	// .percentType().build();
	//
	// public FeneconProEss(String thingId) {
	// super(thingId);
	// }
	//
	// @Override
	// public NumericChannel activePower() {
	// return _activePower;
	// }
	//
	// @IsChannel(id = "ActivePowerPhaseA")
	// public NumericChannel activePowerPhaseA() {
	// return _activePowerPhaseA;
	// }
	//
	// @IsChannel(id = "ActivePowerPhaseB")
	// public NumericChannel activePowerPhaseB() {
	// return _activePowerPhaseB;
	// }
	//
	// @IsChannel(id = "ActivePowerPhaseC")
	// public NumericChannel activePowerPhaseC() {
	// return _activePowerPhaseC;
	// }

	// @Override
	// public NumericChannel allowedDischarge() {
	// return _allowedDischarge;
	// }
	//
	// @Override
	// public NumericChannel apparentPower() {
	// return _apparentPower;
	// }
	//
	// @Override
	// public NumericChannel gridMode() {
	// return _pcsOperationState;
	// }
	//
	// @Override
	// public NumericChannel minSoc() {
	// return _minSoc;
	// }
	//
	// @Override
	// public NumericChannel reactivePower() {
	// return _reactivePower;
	// }
	//
	// @IsChannel(id = "RectivePowerPhaseA")
	// public NumericChannel reactivePowerPhaseA() {
	// return _reactivePowerPhaseA;
	// }
	//
	// @IsChannel(id = "RectivePowerPhaseB")
	// public NumericChannel reactivePowerPhaseB() {
	// return _reactivePowerPhaseB;
	// }
	//
	// @IsChannel(id = "RectivePowerPhaseC")
	// public NumericChannel reactivePowerPhaseC() {
	// return _reactivePowerPhaseC;
	// }
	//
	// @Override
	// public WriteableNumericChannel setActivePower() {
	// return null;
	// }
	//
	// @Override
	// public WriteableNumericChannel setReactivePower() {
	// return null;
	// }
	//
	// @IsChannel(id = "SetReactivePowerPhaseC")
	// public WriteableNumericChannel setReactivePowerPhaseC() {
	// return _setReactivePowerPhaseC;
	// }
	//
	// @IsChannel(id = "SetReactivePowerPhaseB")
	// public WriteableNumericChannel setReactivePowerPhaseB() {
	// return _setReactivePowerPhaseB;
	// }
	//
	// @IsChannel(id = "SetReactivePowerPhaseA")
	// public WriteableNumericChannel setReactivePowerPhaseA() {
	// return _setReactivePowerPhaseA;
	// }
	//
	// @IsChannel(id = "SetActivePowerPhaseC")
	// public WriteableNumericChannel setActivePowerPhaseC() {
	// return _setActivePowerPhaseC;
	// }
	//
	// @IsChannel(id = "SetActivePowerPhaseB")
	// public WriteableNumericChannel setActivePowerPhaseB() {
	// return _setActivePowerPhaseB;
	// }
	//
	// @IsChannel(id = "SetActivePowerPhaseA")
	// public WriteableNumericChannel setactivePowerPhaseA() {
	// return _setActivePowerPhaseA;
	// }
	//
	// @Override
	// public void setMinSoc(Integer minSoc) {
	// this._minSoc.updateValue(Long.valueOf(minSoc));
	// }
	//
	// @Override
	// public WriteableNumericChannel setWorkState() {
	// return _setWorkState;
	// }
	//
	// @Override
	// public NumericChannel soc() {
	// return _soc;
	// }
	//
	// @Override
	// public NumericChannel systemState() {
	// return _systemState;
	// }
	//
	// @Override
	// protected ModbusProtocol defineModbusProtocol() throws ConfigException {
	// return new ModbusProtocol( //

	// new WritableModbusRange(200, //
	// new ElementBuilder().address(200).channel(_setWorkState).build(),
	// new ElementBuilder().address(201).dummy().build(),
	// new ElementBuilder().address(202).dummy().build(),
	// new ElementBuilder().address(203).dummy().build(),
	// new ElementBuilder().address(204).dummy().build(),
	// new ElementBuilder().address(205).dummy().build(),
	// new ElementBuilder().address(206).channel(_setActivePowerPhaseA).build(),
	// new ElementBuilder().address(207).channel(_setReactivePowerPhaseA).build(),
	// new ElementBuilder().address(208).channel(_setActivePowerPhaseB).build(),
	// new ElementBuilder().address(209).channel(_setReactivePowerPhaseB).build(),
	// new ElementBuilder().address(210).channel(_setActivePowerPhaseC).build(),
	// new ElementBuilder().address(211).channel(_setReactivePowerPhaseC).build()));
	// }
	//

}
