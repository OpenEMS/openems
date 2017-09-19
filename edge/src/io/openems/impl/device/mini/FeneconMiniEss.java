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
package io.openems.impl.device.mini;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.device.nature.realtimeclock.RealTimeClockNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
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

@ThingInfo(title = "FENECON Mini ESS")
public class FeneconMiniEss extends ModbusDeviceNature implements SymmetricEssNature, RealTimeClockNature {

	/*
	 * Constructors
	 */
	public FeneconMiniEss(String thingId, Device parent) throws ConfigException {
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
	private ModbusReadLongChannel activePower;
	private ModbusReadLongChannel reactivePower;
	private ModbusWriteLongChannel setWorkState;
	private ModbusWriteLongChannel setActivePower;
	private ModbusWriteLongChannel setReactivePower;
	private ReadChannel<Long> apparentPower;
	// RealTimeClock
	private ModbusWriteLongChannel rtcYear;
	private ModbusWriteLongChannel rtcMonth;
	private ModbusWriteLongChannel rtcDay;
	private ModbusWriteLongChannel rtcHour;
	private ModbusWriteLongChannel rtcMinute;
	private ModbusWriteLongChannel rtcSecond;
	private StaticValueChannel<Long> nominalPower = new StaticValueChannel<Long>("maxNominalPower", this, 3000l)
			.unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 3000L).unit("Wh");

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
	public ReadChannel<Long> activePower() {
		return activePower;
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
	public StatusBitChannels warning() {
		return warning;
	}

	@Override
	public ReadChannel<Long> allowedApparent() {
		return phaseAllowedApparent;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
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
	public ModbusReadLongChannel frequency;
	public ModbusReadLongChannel current;
	public ModbusReadLongChannel voltage;
	public ModbusReadLongChannel pcsOperationState;
	public ModbusReadLongChannel batteryPower;
	public ModbusReadLongChannel batteryGroupAlarm;
	public ModbusReadLongChannel batteryCurrent;
	public ModbusReadLongChannel batteryVoltage;
	public ModbusReadLongChannel batteryGroupState;
	public ModbusReadLongChannel totalBatteryDischargeEnergy;
	public ModbusReadLongChannel totalBatteryChargeEnergy;
	public ModbusReadLongChannel controlMode;
	public ModbusWriteLongChannel setPcsMode;
	public ModbusWriteLongChannel setSetupMode;
	public ModbusReadLongChannel setupMode;
	public ModbusReadLongChannel pcsMode;
	public StatusBitChannel pcsAlarm1;
	public StatusBitChannel pcsAlarm2;
	public StatusBitChannel pcsFault1;
	public StatusBitChannel pcsFault2;
	public StatusBitChannel pcsFault3;

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
				new DummyElement(102, 103), //
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
						current = new ModbusReadLongChannel("Current", this).unit("mA").multiplier(2)),
				new DummyElement(119, 120), new UnsignedWordElement(121, //
						voltage = new ModbusReadLongChannel("Voltage", this).unit("mV").multiplier(2)),
				new DummyElement(122, 123), new SignedWordElement(124, //
						activePower = new ModbusReadLongChannel("ActivePower", this).unit("W")),
				new DummyElement(125, 126), new SignedWordElement(127, //
						reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("var")),
				new DummyElement(128, 130), new UnsignedWordElement(131, //
						frequency = new ModbusReadLongChannel("Frequency", this).unit("mHz").multiplier(1)),
				new DummyElement(132, 133), new UnsignedWordElement(134, //
						phaseAllowedApparent = new ModbusReadLongChannel("PhaseAllowedApparentPower", this).unit("VA")),
				new DummyElement(135, 140), new UnsignedWordElement(141, //
						allowedCharge = new ModbusReadLongChannel("AllowedCharge", this).unit("W").negate()),
				new UnsignedWordElement(142, //
						allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this).unit("W")),
				new DummyElement(143, 149),
				new UnsignedWordElement(150, pcsAlarm1 = warning.channel(new StatusBitChannel("PcsAlarm1", this)//
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
				)), new UnsignedWordElement(151, pcsAlarm2 = warning.channel(new StatusBitChannel("PcsAlarm2", this)//
				)), new UnsignedWordElement(152, warning.channel(pcsFault1 = new StatusBitChannel("PcsFault1", this)//
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
				)), new UnsignedWordElement(153, warning.channel(pcsFault2 = new StatusBitChannel("PcsFault2", this)//
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
				)), new UnsignedWordElement(154, warning.channel(pcsFault3 = new StatusBitChannel("PcsFault3", this)//
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
				new WriteableModbusRegisterRange(201, //
						new SignedWordElement(201,
								setActivePower = new ModbusWriteLongChannel("SetActivePower", this).unit("W")), //
						new SignedWordElement(202,
								setReactivePower = new ModbusWriteLongChannel("SetReactivePower", this).unit("Var"))), //
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
		apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this, (channels) -> {
			ReadChannel<Long> activePower = channels[0];
			ReadChannel<Long> reactivePower = channels[1];
			try {
				return ControllerUtils.calculateApparentPower(activePower.value(), reactivePower.value());
			} catch (InvalidValueException e) {
				log.error("failed to calculate apparentPower. some value is missing.", e);
			}
			return 0l;
		}, activePower, reactivePower);

		return protokol;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return nominalPower;
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

}
