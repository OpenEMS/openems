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
package io.openems.impl.device.minireadonly;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusBitWrappingChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Mini ESS")
public class FeneconMiniEss extends ModbusDeviceNature implements AsymmetricEssNature {

	private ThingStateChannels thingState;

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
		this.thingState = new ThingStateChannels(this);
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this).defaultValue(0);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this).defaultValue(0);

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
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel systemState;
	// Dummies
	private StaticValueChannel<Long> allowedCharge = new StaticValueChannel<Long>("AllowedCharge", this, 0l);
	private StaticValueChannel<Long> allowedDischarge = new StaticValueChannel<Long>("AllowedDischarge", this, 0l);
	private StaticValueChannel<Long> allowedApparent = new StaticValueChannel<Long>("AllowedApparent", this, 0l);
	private StaticValueChannel<Long> gridMode = new StaticValueChannel<Long>("GridMode", this, 0l);
	private StaticValueChannel<Long> reactivePowerL1 = new StaticValueChannel<Long>("ReactivePowerL1", this, 0l);
	private StaticValueChannel<Long> reactivePowerL2 = new StaticValueChannel<Long>("ReactivePowerL2", this, 0l);
	private StaticValueChannel<Long> reactivePowerL3 = new StaticValueChannel<Long>("ReactivePowerL3", this, 0l);
	/*
	 * This channels
	 */
	public ModbusReadLongChannel operatingMode;
	public ModbusReadLongChannel controlMode;
	public ModbusReadLongChannel allowedChargeEnergy;
	public ModbusReadLongChannel dischargedEnergy;
	public ModbusReadLongChannel batteryGroupStatus;
	public ModbusReadLongChannel becu1ChargeCurr;
	public ModbusReadLongChannel becu1DischargeCurr;
	public ModbusReadLongChannel becu1Volt;
	public ModbusReadLongChannel becu1Curr;
	public ModbusReadLongChannel becu1Soc;
	public ModbusReadLongChannel becu1Alarm1;
	public ModbusReadLongChannel becu1Alarm2;
	public ModbusReadLongChannel becu1Fault1;
	public ModbusReadLongChannel becu1Fault2;
	public ModbusReadLongChannel becu1Version;
	public ModbusReadLongChannel becu1MinVoltNo;
	public ModbusReadLongChannel becu1MinVolt;
	public ModbusReadLongChannel becu1MaxVoltNo;
	public ModbusReadLongChannel becu1MaxVolt;
	public ModbusReadLongChannel becu1MinTempNo;
	public ModbusReadLongChannel becu1MinTemp;
	public ModbusReadLongChannel becu1MaxTempNo;
	public ModbusReadLongChannel becu1MaxTemp;
	public ModbusReadLongChannel becu2ChargeCurr;
	public ModbusReadLongChannel becu2DischargeCurr;
	public ModbusReadLongChannel becu2Volt;
	public ModbusReadLongChannel becu2Curr;
	public ModbusReadLongChannel becu2Soc;
	public ModbusReadLongChannel becu2Alarm1;
	public ModbusReadLongChannel becu2Alarm2;
	public ModbusReadLongChannel becu2Fault1;
	public ModbusReadLongChannel becu2Fault2;
	public ModbusReadLongChannel becu2Version;
	public ModbusReadLongChannel becu2MinVoltNo;
	public ModbusReadLongChannel becu2MinVolt;
	public ModbusReadLongChannel becu2MaxVoltNo;
	public ModbusReadLongChannel becu2MaxVolt;
	public ModbusReadLongChannel becu2MinTempNo;
	public ModbusReadLongChannel becu2MinTemp;
	public ModbusReadLongChannel becu2MaxTempNo;
	public ModbusReadLongChannel becu2MaxTemp;
	public ModbusReadLongChannel systemWorkState;
	public ModbusReadLongChannel systemWorkModeState;
	public ModbusReadLongChannel becuNum;
	public ModbusReadLongChannel becuWorkState;
	public ModbusReadLongChannel becuChargeCurr;
	public ModbusReadLongChannel becuDischargeCurr;
	public ModbusReadLongChannel becuVolt;
	public ModbusReadLongChannel becuCurr;
	public ModbusReadLongChannel becuFault1;
	public ModbusReadLongChannel becuFault2;
	public ModbusReadLongChannel becuAlarm1;
	public ModbusReadLongChannel becuAlarm2;
	public ModbusReadLongChannel batteryGroupSoc;
	public ModbusReadLongChannel batteryGroupVoltage;
	public ModbusReadLongChannel batteryGroupCurr;
	public ModbusReadLongChannel batteryGroupPower;

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
	public ReadChannel<Long> allowedApparent() {
		return this.allowedApparent;
	}

	@Override
	public ReadChannel<Long> capacity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> reactivePowerL1() {
		return this.reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return this.reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return this.reactivePowerL3;
	}

	@Override
	public WriteChannel<Long> setActivePowerL1() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setActivePowerL2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setActivePowerL3() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL1() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setReactivePowerL3() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(100, //
						new UnsignedWordElement(100, //
								this.systemState = new ModbusReadLongChannel("SystemState", this)),
						new UnsignedWordElement(101, //
								this.controlMode = new ModbusReadLongChannel("ControlMode", this)),
						new DummyElement(102, 103), new UnsignedDoublewordElement(104, //
								this.allowedChargeEnergy = new ModbusReadLongChannel("BatteryAllowedCharging", this)),
						new UnsignedDoublewordElement(106, //
								this.dischargedEnergy = new ModbusReadLongChannel("DischargedEnergy", this)),
						new UnsignedWordElement(108, //
								this.batteryGroupStatus = new ModbusReadLongChannel("BatteryGroupStatus", this)),
						new UnsignedWordElement(109, //
								this.batteryGroupSoc = new ModbusReadLongChannel("BatteryGroupSoc", this)),
						new UnsignedWordElement(110, //
								this.batteryGroupVoltage = new ModbusReadLongChannel("BatteryGroupVoltage", this)),
						new UnsignedWordElement(111, //
								this.batteryGroupCurr = new ModbusReadLongChannel("BatteryGroupCurr", this)),
						new UnsignedWordElement(112, //
								this.batteryGroupPower = new ModbusReadLongChannel("BatteryGroupPower", this))),
				new ModbusRegisterRange(2007, //
						new UnsignedWordElement(2007, //
								this.activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
								.ignore(0l)
								.delta(10000l))),
				new ModbusRegisterRange(2107, //
						new UnsignedWordElement(2107, //
								this.activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
								.ignore(0l)
								.delta(10000l))),
				new ModbusRegisterRange(2207, //
						new UnsignedWordElement(2207, //
								this.activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
								.ignore(0l)
								.delta(10000l))),
				new ModbusRegisterRange(3000, //
						new UnsignedWordElement(3000, //
								this.becu1ChargeCurr = new ModbusReadLongChannel("Becu1ChargeCurr", this)),
						new UnsignedWordElement(3001, //
								this.becu1DischargeCurr = new ModbusReadLongChannel("Becu1DischargeCurr", this)),
						new UnsignedWordElement(3002, //
								this.becu1Volt = new ModbusReadLongChannel("Becu1Volt", this)),
						new UnsignedWordElement(3003, //
								this.becu1Curr = new ModbusReadLongChannel("Becu1Curr", this)),
						new UnsignedWordElement(3004, //
								this.becu1Soc = new ModbusReadLongChannel("Becu1Soc", this)),
						new UnsignedWordElement(3005, //
								new ModbusBitWrappingChannel("BecuAlarm1" , this, this.thingState)//
								.warningBit(0, WarningEss.BECU1GeneralChargeOverCurrentAlarm )//
								.warningBit(1, WarningEss.BECU1GeneralDischargeOverCurrentAlarm )//
								.warningBit(2, WarningEss.BECU1ChargeCurrentLimitAlarm)//
								.warningBit(3, WarningEss.BECU1DischargeCurrentLimitAlarm)//
								.warningBit(4, WarningEss.BECU1GeneralHighVoltageAlarm)//
								.warningBit(5, WarningEss.BECU1GeneralLowVoltageAlarm)//
								.warningBit(6, WarningEss.BECU1AbnormalVoltageChangeAlarm)//
								.warningBit(7, WarningEss.BECU1GeneralHighTemperatureAlarm )//
								.warningBit(8, WarningEss.BECU1GeneralLowTemperatureAlarm )//
								.warningBit(9, WarningEss.BECU1AbnormalTemperatureChangeAlarm)//
								.warningBit(10,WarningEss.BECU1SevereHighVoltageAlarm)//
								.warningBit(11,WarningEss.BECU1SevereLowVoltageAlarm)//
								.warningBit(12,WarningEss.BECU1SevereLowTemperatureAlarm)//
								.warningBit(13,WarningEss.BECU1SeverveChargeOverCurrentAlarm )//
								.warningBit(14,WarningEss.BECU1SeverveDischargeOverCurrentAlarm )//
								.warningBit(15,WarningEss.BECU1AbnormalCellCapacityAlarm)//
								),//

						new UnsignedWordElement(3006, //
								new ModbusBitWrappingChannel("BecuAlarm2" , this, this.thingState)//
								.warningBit(0, WarningEss.BECU1BalancedSamplingAlarm)//
								.warningBit(1, WarningEss.BECU1BalancedControlAlarm)//
								.warningBit(2, WarningEss.BECU1HallSensorDoesNotWorkAccurately)//
								.warningBit(4, WarningEss.BECU1Generalleakage)//
								.warningBit(5, WarningEss.BECU1Severeleakage)//
								.warningBit(6, WarningEss.BECU1Contactor1TurnOnAbnormity)//
								.warningBit(7, WarningEss.BECU1Contactor1TurnOffAbnormity)//
								.warningBit(8, WarningEss.BECU1Contactor2TurnOnAbnormity)//
								.warningBit(9, WarningEss.BECU1Contactor2TurnOffAbnormity )//
								.warningBit(10,WarningEss.BECU1Contactor4CheckAbnormity )//
								.warningBit(11,WarningEss.BECU1ContactorCurrentUnsafe)//
								.warningBit(12,WarningEss.BECU1Contactor5CheckAbnormity)//
								.warningBit(13,WarningEss.BECU1HighVoltageOffset )//
								.warningBit(14,WarningEss.BECU1LowVoltageOffset )//
								.warningBit(15,WarningEss.BECU1HighTemperatureOffset )//
								),//

						new UnsignedWordElement(3007, //
								new ModbusBitWrappingChannel("BecuFault1" , this, this.thingState)//
								.faultBit(0, FaultEss.BECU1DischargeSevereOvercurrent)//
								.faultBit(1, FaultEss.BECU1ChargeSevereOvercurrent)//
								.faultBit(2, FaultEss.BECU1GeneralUndervoltage)//
								.faultBit(3, FaultEss.BECU1SevereOvervoltage)//
								.faultBit(4, FaultEss.BECU1GeneralOvervoltage)//
								.faultBit(5, FaultEss.BECU1SevereUndervoltage)//
								.faultBit(6, FaultEss.BECU1InsideCANBroken)//
								.faultBit(7, FaultEss.BECU1GeneralUndervoltageHighCurrentDischarge)//
								.faultBit(8, FaultEss.BECU1BMUError)//
								.faultBit(9, FaultEss.BECU1CurrentSamplingInvalidation)//
								.faultBit(10,FaultEss.BECU1BatteryFail)//
								.faultBit(13,FaultEss.BECU1TemperatureSamplingBroken)//
								.faultBit(14,FaultEss.BECU1Contactor1TestBackIsAbnormalTurnOnAbnormity)//
								.faultBit(15,FaultEss.BECU1Contactor1TestBackIsAbnormalTurnOffAbnormity)//
								),//
						new UnsignedWordElement(3008, //
								new ModbusBitWrappingChannel("BecuFault2" , this, this.thingState)//
								.faultBit(0, FaultEss.BECU1Contactor2TestBackIsAbnormalTurnOnAbnormity)//
								.faultBit(1, FaultEss.BECU1Contactor2TestBackIsAbnormalTurnOffAbnormity)//
								.faultBit(2, FaultEss.BECU1SevereHighTemperatureFault)//
								.faultBit(9, FaultEss.BECU1HallInvalidation)//
								.faultBit(10,FaultEss.BECU1ContactorInvalidation)//
								.faultBit(12,FaultEss.BECU1OutsideCANBroken)//
								.faultBit(13,FaultEss.BECU1CathodeContactorBroken)//
								),//
						new UnsignedWordElement(3009, //
								this.becu1Version = new ModbusReadLongChannel("Becu1Version", this)),
						new DummyElement(3010, 3011), //
						new UnsignedWordElement(3012, //
								this.becu1MinVoltNo = new ModbusReadLongChannel("Becu1MinVoltNo", this)),
						new UnsignedWordElement(3013, //
								this.becu1MinVolt = new ModbusReadLongChannel("Becu1MinVolt", this)),
						new UnsignedWordElement(3014, //
								this.becu1MaxVoltNo = new ModbusReadLongChannel("Becu1MaxVoltNo", this)),
						new UnsignedWordElement(3015, //
								this.becu1MaxVolt = new ModbusReadLongChannel("Becu1MaxVolt", this)),
						new UnsignedWordElement(3016, //
								this.becu1MinTempNo = new ModbusReadLongChannel("Becu1MinTempNo", this)),
						new UnsignedWordElement(3017, //
								this.becu1MinTemp = new ModbusReadLongChannel("Becu1MinTemp", this)),
						new UnsignedWordElement(3018, //
								this.becu1MaxTempNo = new ModbusReadLongChannel("Becu1MaxTempNo", this)),
						new UnsignedWordElement(3019, //
								this.becu1MaxTemp = new ModbusReadLongChannel("Becu1MaxTemp", this))),
				new ModbusRegisterRange(3200, //
						new UnsignedWordElement(3200, //
								this.becu2ChargeCurr = new ModbusReadLongChannel("Becu2ChargeCurr", this)),
						new UnsignedWordElement(3201, //
								this.becu2DischargeCurr = new ModbusReadLongChannel("Becu2DischargeCurr", this)),
						new UnsignedWordElement(3202, //
								this.becu2Volt = new ModbusReadLongChannel("Becu2Volt", this)),
						new UnsignedWordElement(3203, //
								this.becu2Curr = new ModbusReadLongChannel("Becu2Curr", this)),
						new UnsignedWordElement(3204, //
								this.becu2Soc = new ModbusReadLongChannel("Becu2Soc", this)),
						new UnsignedWordElement(3205, //
								new ModbusBitWrappingChannel("Becu2Alarm1" , this, this.thingState)//
								.warningBit(0, WarningEss.BECU2GeneralChargeOverCurrentAlarm )//
								.warningBit(1, WarningEss.BECU2GeneralDischargeOverCurrentAlarm )//
								.warningBit(2, WarningEss.BECU2ChargeCurrentLimitAlarm)//
								.warningBit(3, WarningEss.BECU2DischargeCurrentLimitAlarm)//
								.warningBit(4, WarningEss.BECU2GeneralHighVoltageAlarm)//
								.warningBit(5, WarningEss.BECU2GeneralLowVoltageAlarm)//
								.warningBit(6, WarningEss.BECU2AbnormalVoltageChangeAlarm)//
								.warningBit(7, WarningEss.BECU2GeneralHighTemperatureAlarm )//
								.warningBit(8, WarningEss.BECU2GeneralLowTemperatureAlarm )//
								.warningBit(9, WarningEss.BECU2AbnormalTemperatureChangeAlarm)//
								.warningBit(10,WarningEss.BECU2SevereHighVoltageAlarm)//
								.warningBit(11,WarningEss.BECU2SevereLowVoltageAlarm)//
								.warningBit(12,WarningEss.BECU2SevereLowTemperatureAlarm)//
								.warningBit(13,WarningEss.BECU2SeverveChargeOverCurrentAlarm )//
								.warningBit(14,WarningEss.BECU2SeverveDischargeOverCurrentAlarm )//
								.warningBit(15,WarningEss.BECU2AbnormalCellCapacityAlarm)//
								),//
						new UnsignedWordElement(3206, //
								new ModbusBitWrappingChannel("Becu2Alarm2" , this, this.thingState)//
								.warningBit(0, WarningEss.BECU2BalancedSamplingAlarm)//
								.warningBit(1, WarningEss.BECU2BalancedControlAlarm)//
								.warningBit(2, WarningEss.BECU2HallSensorDoesNotWorkAccurately)//
								.warningBit(4, WarningEss.BECU2Generalleakage)//
								.warningBit(5, WarningEss.BECU2Severeleakage)//
								.warningBit(6, WarningEss.BECU2Contactor1TurnOnAbnormity)//
								.warningBit(7, WarningEss.BECU2Contactor1TurnOffAbnormity)//
								.warningBit(8, WarningEss.BECU2Contactor2TurnOnAbnormity)//
								.warningBit(9, WarningEss.BECU2Contactor2TurnOffAbnormity )//
								.warningBit(10,WarningEss.BECU2Contactor4CheckAbnormity )//
								.warningBit(11,WarningEss.BECU2ContactorCurrentUnsafe)//
								.warningBit(12,WarningEss.BECU2Contactor5CheckAbnormity)//
								.warningBit(13,WarningEss.BECU2HighVoltageOffset )//
								.warningBit(14,WarningEss.BECU2LowVoltageOffset )//
								.warningBit(15,WarningEss.BECU2HighTemperatureOffset )//
								),//
						new UnsignedWordElement(3207, //
								new ModbusBitWrappingChannel("Becu2Fault1" , this, this.thingState)//
								.faultBit(0, FaultEss.BECU2DischargeSevereOvercurrent)//
								.faultBit(1, FaultEss.BECU2ChargeSevereOvercurrent)//
								.faultBit(2, FaultEss.BECU2GeneralUndervoltage)//
								.faultBit(3, FaultEss.BECU2SevereOvervoltage)//
								.faultBit(4, FaultEss.BECU2GeneralOvervoltage)//
								.faultBit(5, FaultEss.BECU2SevereUndervoltage)//
								.faultBit(6, FaultEss.BECU2InsideCANBroken)//
								.faultBit(7, FaultEss.BECU2GeneralUndervoltageHighCurrentDischarge)//
								.faultBit(8, FaultEss.BECU2BMUError)//
								.faultBit(9, FaultEss.BECU2CurrentSamplingInvalidation)//
								.faultBit(10,FaultEss.BECU2BatteryFail)//
								.faultBit(13,FaultEss.BECU2TemperatureSamplingBroken)//
								.faultBit(14,FaultEss.BECU2Contactor1TestBackIsAbnormalTurnOnAbnormity)//
								.faultBit(15,FaultEss.BECU2Contactor1TestBackIsAbnormalTurnOffAbnormity)//
								),//
						new UnsignedWordElement(3208, //
								new ModbusBitWrappingChannel("Becu2Fault2" , this, this.thingState)//
								.faultBit(0, FaultEss.BECU2Contactor2TestBackIsAbnormalTurnOnAbnormity)//
								.faultBit(1, FaultEss.BECU2Contactor2TestBackIsAbnormalTurnOffAbnormity)//
								.faultBit(2, FaultEss.BECU2SevereHighTemperatureFault)//
								.faultBit(9, FaultEss.BECU2HallInvalidation)//
								.faultBit(10,FaultEss.BECU2ContactorInvalidation)//
								.faultBit(12,FaultEss.BECU2OutsideCANBroken)//
								.faultBit(13,FaultEss.BECU2CathodeContactorBroken)//
								),//
						new UnsignedWordElement(3209, //
								this.becu2Version = new ModbusReadLongChannel("Becu2Version", this)),
						new DummyElement(3210, 3211), //
						new UnsignedWordElement(3212, //
								this.becu2MinVoltNo = new ModbusReadLongChannel("Becu2MinVoltNo", this)),
						new UnsignedWordElement(3213, //
								this.becu2MinVolt = new ModbusReadLongChannel("Becu2MinVolt", this)),
						new UnsignedWordElement(3214, //
								this.becu2MaxVoltNo = new ModbusReadLongChannel("Becu2MaxVoltNo", this)),
						new UnsignedWordElement(3215, //
								this.becu2MaxVolt = new ModbusReadLongChannel("Becu2MaxVolt", this)),
						new UnsignedWordElement(3216, //
								this.becu2MinTempNo = new ModbusReadLongChannel("Becu2MinTempNo", this)),
						new UnsignedWordElement(3217, //
								this.becu2MinTemp = new ModbusReadLongChannel("Becu2MinTemp", this)),
						new UnsignedWordElement(3218, //
								this.becu2MaxTempNo = new ModbusReadLongChannel("Becu2MaxTempNo", this)),
						new UnsignedWordElement(3219, //
								this.becu2MaxTemp = new ModbusReadLongChannel("Becu2MaxTemp", this))),
				new ModbusRegisterRange(4000, //
						new UnsignedWordElement(4000, //
								this.systemWorkState = new ModbusReadLongChannel("SystemWorkState", this)),
						new UnsignedWordElement(4001, //
								this.systemWorkModeState = new ModbusReadLongChannel("SystemWorkModeState", this))),
				new ModbusRegisterRange(4800, //
						new UnsignedWordElement(4800, //
								this.becuNum = new ModbusReadLongChannel("BecuNum", this)),
						new UnsignedWordElement(4801, //
								this.becuWorkState = new ModbusReadLongChannel("BecuWorkState", this)),
						new DummyElement(4802, 4802), new UnsignedWordElement(4803, //
								this.becuChargeCurr = new ModbusReadLongChannel("BecuChargeCurr", this)),
						new UnsignedWordElement(4804, //
								this.becuDischargeCurr = new ModbusReadLongChannel("BecuDischargeCurr", this)),
						new UnsignedWordElement(4805, //
								this.becuVolt = new ModbusReadLongChannel("BecuVolt", this)),
						new UnsignedWordElement(4806, //
								this.becuCurr = new ModbusReadLongChannel("BecuCurr", this)),
						new UnsignedWordElement(4807, //
								this.becuWorkState = new ModbusReadLongChannel("BecuWorkState", this)),
						new UnsignedWordElement(4808, //
								new ModbusBitWrappingChannel("BecuFault1", this, this.thingState)//
								.faultBit(0, FaultEss.NoAvailableBatteryGroup)//
								.faultBit(1, FaultEss.StackGeneralLeakage)//
								.faultBit(2, FaultEss.StackSevereLeakage)//
								.faultBit(3, FaultEss.StackStartingFail)//
								.faultBit(4, FaultEss.StackStoppingFail)//
								.faultBit(9, FaultEss.BatteryProtection)//
								),//
						new UnsignedWordElement(4809, //
								new ModbusBitWrappingChannel("BecuFault2" , this, this.thingState)//
								.faultBit(0, FaultEss.StackAndGroup1CANCommunicationInterrupt)//
								.faultBit(1, FaultEss.StackAndGroup2CANCommunicationInterrupt)//
								),//
						new UnsignedWordElement(4810, //
								new ModbusBitWrappingChannel("BecuAlarm2" , this, this.thingState)//
								.warningBit(0, WarningEss.GeneralOvercurrentAlarmAtCellStackCharge)//
								.warningBit(1, WarningEss.GeneralOvercurrentAlarmAtCellStackDischarge)//
								.warningBit(2, WarningEss.CurrentLimitAlarmAtCellStackCharge)//
								.warningBit(3, WarningEss.CurrentLimitAlarmAtCellStackDischarge)//
								.warningBit(4, WarningEss.GeneralCellStackHighVoltageAlarm)//
								.warningBit(5, WarningEss.GeneralCellStackLowVoltageAlarm)//
								.warningBit(6, WarningEss.AbnormalCellStackVoltageChangeAlarm)//
								.warningBit(7, WarningEss.GeneralCellStackHighTemperatureAlarm)//
								.warningBit(8, WarningEss.GeneralCellStackLowTemperatureAlarm)//
								.warningBit(9, WarningEss.AbnormalCellStackTemperatureChangeAlarm)//
								.warningBit(10,WarningEss.SevereCellStackHighVoltageAlarm)//
								.warningBit(11,WarningEss.SevereCellStackLowVoltageAlarm)//
								.warningBit(12,WarningEss.SevereCellStackLowTemperatureAlarm)//
								.warningBit(13,WarningEss.SeverveOverCurrentAlarmAtCellStackDharge)//
								.warningBit(14,WarningEss.SeverveOverCurrentAlarmAtCellStackDischarge)//
								.warningBit(15,WarningEss.AbnormalCellStackCapacityAlarm)//
								),//
						new UnsignedWordElement(4811, //
								new ModbusBitWrappingChannel("BecuAlarm2" , this, this.thingState)//
								.warningBit(0, WarningEss.TheParameterOfEEPROMInCellStackLoseEffectiveness)//
								.warningBit(1, WarningEss.IsolatingSwitchInConfluenceArkBreak)//
								.warningBit(2, WarningEss.TheCommunicationBetweenCellStackAndTemperatureOfCollectorBreak)//
								.warningBit(3, WarningEss.TheTemperatureOfCollectorFail)//
								.warningBit(4, WarningEss.HallSensorDoNotWorkAccurately)//
								.warningBit(5, WarningEss.TheCommunicationOfPCSBreak)//
								.warningBit(6, WarningEss.AdvancedChargingOrMainContactorCloseAbnormally)//
								.warningBit(7, WarningEss.AbnormalSampledVoltage)//
								.warningBit(8, WarningEss.AbnormalAdvancedContactorOrAbnormalRS485GalleryOfPCS)//
								.warningBit(9, WarningEss.AbnormalMainContactor)//
								.warningBit(10,WarningEss.GeneralCellStackLeakage)//
								.warningBit(11,WarningEss.SevereCellStackLeakage)//
								.warningBit(12,WarningEss.SmokeAlarm)//
								.warningBit(13,WarningEss.TheCommunicationWireToAmmeterBreak)//
								.warningBit(14,WarningEss.TheCommunicationWireToDredBreak)//
								),//
						new UnsignedWordElement(4812, //
								this.soc = new ModbusReadLongChannel("Soc", this).unit("%"))));
		return protocol;
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
