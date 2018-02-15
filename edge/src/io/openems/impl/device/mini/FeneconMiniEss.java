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
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.device.nature.realtimeclock.RealTimeClockNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
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

@ThingInfo(title = "FENECON Mini ESS")
public class FeneconMiniEss extends ModbusDeviceNature implements SymmetricEssNature, RealTimeClockNature {

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
						new ModbusBitWrappingChannel("BatteryGroupAlarm" , this, this.thingState)//
						.warningBit(0, WarningEss.FailTheSystemShouldBeStopped )//
						.warningBit(1, WarningEss.CommonLowVoltageAlarm)//
						.warningBit(2, WarningEss.CommonHighVoltageAlarm)//
						.warningBit(3, WarningEss.ChargingOverCurrentAlarm)//
						.warningBit(4, WarningEss.DischargingOverCurrentAlarm)//
						.warningBit(5, WarningEss.OverTemperatureAlarm)//
						.warningBit(6, WarningEss.InteralCommunicationAbnormal)//
						),//

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
				new UnsignedWordElement(150,//
						new ModbusBitWrappingChannel("PcsAlarm1" , this, this.thingState)//
						.warningBit(0, WarningEss.GridUndervoltage)//
						.warningBit(1, WarningEss.GridOvervoltage)//
						.warningBit(2, WarningEss.GridUnderFrequency)//
						.warningBit(3, WarningEss.GridOverFrequency)//
						.warningBit(4, WarningEss.GridPowerSupplyOff)//
						.warningBit(5, WarningEss.GridConditionUnmeet)//
						.warningBit(6, WarningEss.DCUnderVoltage)//
						.warningBit(7, WarningEss.InputOverResistance)//
						.warningBit(8, WarningEss.CombinationError)//
						.warningBit(9, WarningEss.CommWithInverterError)//
						.warningBit(10,WarningEss.TmeError)//
						),//

				new UnsignedWordElement(151,
						new ModbusBitWrappingChannel("PcsAlarm2", this, this.thingState)//
						), //

				new UnsignedWordElement(152,
						new ModbusBitWrappingChannel("PcsFault1", this, this.thingState)//
						.faultBit(0, FaultEss.ControlCurrentOverload100Percent)//
						.faultBit(1, FaultEss.ControlCurrentOverload110Percent)//
						.faultBit(2, FaultEss.ControlCurrentOverload150Percent)//
						.faultBit(3, FaultEss.ControlCurrentOverload200Percent)//
						.faultBit(4, FaultEss.ControlCurrentOverload120Percent)//
						.faultBit(5, FaultEss.ControlCurrentOverload300Percent)//
						.faultBit(6, FaultEss.ControlTransientLoad300Percent)//
						.faultBit(7, FaultEss.GridOverCurrent)//
						.faultBit(8, FaultEss.LockingWaveformTooManyTimes)//
						.faultBit(9, FaultEss.InverterVoltageZeroDriftError)//
						.faultBit(10,FaultEss.GridVoltageZeroDriftError)//
						.faultBit(11,FaultEss.ControlCurrentZeroDriftError)//
						.faultBit(12,FaultEss.InverterCurrentZeroDriftError)//
						.faultBit(13,FaultEss.GridCurrentZeroDriftError)//
						.faultBit(14, FaultEss.PDPProtection)//
						.faultBit(15, FaultEss.HardwareControlCurrentProtection)//
						),//

				new UnsignedWordElement(153, //
						new ModbusBitWrappingChannel("PcsFault2" , this, this.thingState)//
						.faultBit(0, FaultEss.HardwareACVoltProtection)//
						.faultBit(1, FaultEss.HardwareDCCurrentProtection)//
						.faultBit(2, FaultEss.HardwareTemperatureProtection)//
						.faultBit(3, FaultEss.NoCapturingSignal)//
						.faultBit(4, FaultEss.DCOvervoltage)//
						.faultBit(5, FaultEss.DCDisconnected)//
						.faultBit(6, FaultEss.InverterUndervoltage)//
						.faultBit(7, FaultEss.InverterOvervoltage)//
						.faultBit(8, FaultEss.CurrentSensorFail)//
						.faultBit(9, FaultEss.VoltageSensorFail)//
						.faultBit(10,FaultEss.PowerUncontrollable)//
						.faultBit(11,FaultEss.CurrentUncontrollable)//
						.faultBit(12,FaultEss.FanError)//
						.faultBit(13,FaultEss.PhaseLack)//
						.faultBit(14,FaultEss.InverterRelayFault)//
						.faultBit(15,FaultEss.GridRelayFault)//
						),//

				new UnsignedWordElement(154, //
						new ModbusBitWrappingChannel("PcsFault3", this, this.thingState)//
						.faultBit(0, FaultEss.ControlPanelOvertemp)//
						.faultBit(1, FaultEss.PowerPanelOvertemp)//
						.faultBit(2, FaultEss.DCInputOvercurrent)//
						.faultBit(3, FaultEss.CapacitorOvertemp)//
						.faultBit(4, FaultEss.RadiatorOvertemp)//
						.faultBit(5, FaultEss.TransformerOvertemp)//
						.faultBit(6, FaultEss.CombinationCommError)//
						.faultBit(7, FaultEss.EEPROMError)//
						.faultBit(8, FaultEss.LoadCurrentZeroDriftError)//
						.faultBit(9, FaultEss.CurrentLimitRError)//
						.faultBit(10,FaultEss.PhaseSyncError)//
						.faultBit(11,FaultEss.ExternalPVCurrentZeroDriftError)//
						.faultBit(12,FaultEss.ExternalGridCurrentZeroDriftError)//
						)),//

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

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
