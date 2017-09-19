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
package io.openems.impl.device.byd;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "BYD BEM 125 KTLA01 ESS")
public class Bem125ktla01Ess extends ModbusDeviceNature implements SymmetricEssNature {

	/*
	 * Constructors
	 */
	public Bem125ktla01Ess(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
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
	private ModbusReadChannel<Long> soc;
	private StaticValueChannel<Long> allowedCharge = new StaticValueChannel<Long>("AllowedCharge", this, 0L);
	private StaticValueChannel<Long> allowedDischarge = new StaticValueChannel<Long>("AllowedDischarge", this, 0L);
	private StaticValueChannel<Long> allowedApparent = new StaticValueChannel<>("allowedApparent", this, 0L).unit("VA")
			.unit("VA");
	private ModbusReadChannel<Long> apparentPower;
	private StaticValueChannel<Long> gridMode = new StaticValueChannel<Long>("GridMode", this, 0L);
	private StaticValueChannel<Long> activePower = new StaticValueChannel<Long>("ActivePower", this, 0L);
	private StaticValueChannel<Long> reactivePower = new StaticValueChannel<Long>("ActivePower", this, 0L);
	private StaticValueChannel<Long> systemState = new StaticValueChannel<Long>("SystemState", this, 0L);;
	private ModbusWriteChannel<Long> setActivePower;
	private ModbusWriteChannel<Long> setReactivePower;
	private ModbusWriteChannel<Long> setWorkState;
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 0L);
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 170000L).unit("Wh");
	public StatusBitChannels warning;

	public ModbusReadChannel<Long> sysAlarmInfo;
	public StatusBitChannel sysWorkStatus;
	public StatusBitChannel sysControlMode;
	public StatusBitChannel sysAlarmInfo2;
	public ModbusReadChannel<Long> batteryStackVoltage;
	public ModbusReadChannel<Long> batteryStackCurrent;
	public ModbusReadChannel<Long> batteryStackPower;
	public ModbusReadChannel<Long> batteryStackSoc;
	public ModbusReadChannel<Long> batteryStackSoh;
	public ModbusReadChannel<Long> batteryStackMaxChargeCurrent;
	public ModbusReadChannel<Long> batteryStackMaxDischargeCurrent;
	public ModbusReadChannel<Long> batteryStackMaxChargePower;
	public ModbusReadChannel<Long> batteryStackMaxDischargePower;
	public ModbusReadChannel<Long> batteryStackTotalCapacity;
	public ModbusReadChannel<Long> batteryStackTotalCharge;
	public ModbusReadChannel<Long> batteryStackTotalDischarge;

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

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		warning = new StatusBitChannels("Warning", this);
		return new ModbusProtocol( //
				new ModbusRegisterRange(0x0100, //
						new UnsignedWordElement(0x100, //
								sysAlarmInfo = new ModbusReadLongChannel("SysAlarmInfo", this)//
										.label(0, "Warning State")//
										.label(1, "Protection State")//
										.label(2, "Derating State")//
										.label(4, "Charge Forbidden").label(16, "Discharge Forbidden")),
						new UnsignedWordElement(0x101, //
								sysWorkStatus = new StatusBitChannel("SysWorkStatus", this)//
										.label(0, "Initial") //
										.label(1, "Fault") //
										.label(2, "Stop") //
										.label(4, "Hot Standby") //
										.label(8, "Monitoring") //
										.label(16, "Standby") //
										.label(32, "Operation") //
										.label(64, "Debug")), //
						new UnsignedWordElement(0x102, //
								sysControlMode = new StatusBitChannel("SysControlMode", this)//
										.label(0, "Remote") //
										.label(1, "Local")), //
						new DummyElement(0x103)),
				new ModbusRegisterRange(0x0110, //
						new UnsignedWordElement(0x110, //
								sysAlarmInfo = new StatusBitChannel("SysAlarmInfo", this)//
										.label(0, "Status abnormal of AC surge protector") //
										.label(1, "Close of control switch") //
										.label(2, "Emergency stop") //
										.label(4, "Status abnormal of frog detector") //
										.label(8, "Serious leakage") //
										.label(16, "Normal_leakage")), //
						new UnsignedWordElement(0x111, //
								sysAlarmInfo2 = new StatusBitChannel("SysAlarmInfo2", this)//
										.label(0, "Failure of temperature sensor in control cabinet") //
										.label(1, "Close of control switch") //
						/*
						 * TODO new OnOffBitItem(9, "Failure_of_humidity_sensor_in_control_cabinet"), //
						 * new OnOffBitItem(12, "Failure_of_storage_device"), //
						 * new OnOffBitItem(13, "Exceeding_of_humidity_in_control_cabinet"))));
						 */
						)), new ModbusRegisterRange(0x1300, new UnsignedWordElement(0x1300, //
								batteryStackVoltage = new ModbusReadLongChannel("BatteryStackVoltage", this)
										.multiplier(2).unit("mV")),
								new UnsignedWordElement(0x1301, //
										batteryStackCurrent = new ModbusReadLongChannel("BatteryStackCurrent", this)
												.multiplier(2).unit("mA")),
								new UnsignedWordElement(0x1302, //
										batteryStackPower = new ModbusReadLongChannel("BatteryStackPower", this)
												.multiplier(2).unit("W")),
								new UnsignedWordElement(0x1303, //
										batteryStackSoc = soc = new ModbusReadLongChannel("BatteryStackSoc", this)
												.unit("%")),
								new UnsignedWordElement(0x1304, //
										batteryStackSoh = new ModbusReadLongChannel("BatteryStackSoh", this).unit("%")),
								new UnsignedWordElement(0x1305, //
										batteryStackMaxChargeCurrent = new ModbusReadLongChannel(
												"BatteryStackMaxChargeCurrent", this).multiplier(2).unit("mA")),
								new UnsignedWordElement(0x1306, //
										batteryStackMaxDischargeCurrent = new ModbusReadLongChannel(
												"BatteryStackMaxDischargeCurrent", this).multiplier(2).unit("mA")),
								new UnsignedWordElement(0x1307, //
										batteryStackMaxChargePower = new ModbusReadLongChannel(
												"BatteryStackMaxChargePower", this).multiplier(2).unit("W")),
								new UnsignedWordElement(0x1308, //
										batteryStackMaxDischargePower = new ModbusReadLongChannel(
												"BatteryStackMaxDischargePower", this).multiplier(2).unit("W")),
								new UnsignedWordElement(0x1309, //
										batteryStackTotalCapacity = new ModbusReadLongChannel(
												"BatteryStackTotalCapacity", this).unit("Wh")),
								new UnsignedDoublewordElement(0x130A, //
										batteryStackTotalCharge = new ModbusReadLongChannel("BatteryStackTotalCharge",
												this).unit("kWh")),
								new UnsignedDoublewordElement(0x130C, //
										batteryStackTotalDischarge = new ModbusReadLongChannel(
												"BatteryStackTotalDischarge", this).unit("kWh"))));
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}
}
