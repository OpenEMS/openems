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
package io.openems.impl.device.commercial;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusBitWrappingChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Commercial DC-Charger")
public class FeneconCommercialCharger extends ModbusDeviceNature implements ChargerNature {

	/*
	 * Constructors
	 */
	public FeneconCommercialCharger(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		this.thingState = new ThingStateChannels(this);

	}

	/*
	 * Inherited Channels
	 */
	private ThingStateChannels thingState;

	public ModbusWriteChannel<Long> pvPowerLimitCommand;

	public ReadChannel<Long> actualPower;

	public ReadChannel<Long> inputVoltage;

	private final ConfigChannel<Long> maxActualPower = new ConfigChannel<Long>("maxActualPower", this);

	@Override
	public ConfigChannel<Long> maxActualPower() {
		return maxActualPower;
	}

	/*
	 * BMS DCDC
	 */

	public ModbusReadChannel<Long> bmsDCDCWorkState;
	public ModbusReadChannel<Long> bmsDCDCWorkMode;
	public ModbusReadChannel<Long> bmsDCDCSuggestiveInformation1;
	public ModbusReadChannel<Long> bmsDCDCSuggestiveInformation2;
	public ModbusReadChannel<Long> bmsDCDCSuggestiveInformation3;
	public ModbusReadChannel<Long> bmsDCDCSuggestiveInformation4;
	public ModbusReadChannel<Long> bmsDCDCSuggestiveInformation5;
	public ModbusReadChannel<Long> bmsDCDCAbnormity1;
	public ModbusReadChannel<Long> bmsDCDCAbnormity2;
	public ModbusReadChannel<Long> bmsDCDCAbnormity3;
	public ModbusReadChannel<Long> bmsDCDCAbnormity4;
	public ModbusReadChannel<Long> bmsDCDCAbnormity5;
	public ModbusReadChannel<Long> bmsDCDCAbnormity6;
	public ModbusReadChannel<Long> bmsDCDCAbnormity7;
	public ModbusReadChannel<Long> bmsDCDCSwitchState;
	public ModbusReadChannel<Long> bmsDCDCOutputVoltage;
	public ModbusReadChannel<Long> bmsDCDCOutputCurrent;
	public ModbusReadChannel<Long> bmsDCDCOutputPower;
	public ModbusReadChannel<Long> bmsDCDCInputVoltage;
	public ModbusReadChannel<Long> bmsDCDCInputCurrent;
	public ModbusReadChannel<Long> bmsDCDCInputPower;
	public ModbusReadChannel<Long> bmsDCDCInputEnergy;
	public ModbusReadChannel<Long> bmsDCDCOutputEnergy;
	public ModbusReadChannel<Long> bmsDCDCReactorTemperature;
	public ModbusReadChannel<Long> bmsDCDCIgbtTemperature;
	public ModbusReadChannel<Long> bmsDCDCInputTotalChargeEnergy;
	public ModbusReadChannel<Long> bmsDCDCInputTotalDischargeEnergy;
	public ModbusReadChannel<Long> bmsDCDCOutputTotalChargeEnergy;
	public ModbusReadChannel<Long> bmsDCDCOutputTotalDischargeEnergy;

	/*
	 * BMS DCDC 1
	 */

	public ModbusReadChannel<Long> bmsDCDC1WorkState;
	public ModbusReadChannel<Long> bmsDCDC1WorkMode;
	public ModbusReadChannel<Long> bmsDCDC1SuggestiveInformation1;
	public ModbusReadChannel<Long> bmsDCDC1SuggestiveInformation2;
	public ModbusReadChannel<Long> bmsDCDC1SuggestiveInformation3;
	public ModbusReadChannel<Long> bmsDCDC1SuggestiveInformation4;
	public ModbusReadChannel<Long> bmsDCDC1SuggestiveInformation5;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity1;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity2;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity3;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity4;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity5;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity6;
	public ModbusReadChannel<Long> bmsDCDC1Abnormity7;
	public ModbusReadChannel<Long> bmsDCDC1SwitchState;
	public ModbusReadChannel<Long> bmsDCDC1OutputVoltage;
	public ModbusReadChannel<Long> bmsDCDC1OutputCurrent;
	public ModbusReadChannel<Long> bmsDCDC1OutputPower;
	public ModbusReadChannel<Long> bmsDCDC1InputVoltage;
	public ModbusReadChannel<Long> bmsDCDC1InputCurrent;
	public ModbusReadChannel<Long> bmsDCDC1InputPower;
	public ModbusReadChannel<Long> bmsDCDC1InputEnergy;
	public ModbusReadChannel<Long> bmsDCDC1OutputEnergy;
	public ModbusReadChannel<Long> bmsDCDC1ReactorTemperature;
	public ModbusReadChannel<Long> bmsDCDC1IgbtTemperature;
	public ModbusReadChannel<Long> bmsDCDC1InputTotalChargeEnergy;
	public ModbusReadChannel<Long> bmsDCDC1InputTotalDischargeEnergy;
	public ModbusReadChannel<Long> bmsDCDC1OutputTotalChargeEnergy;
	public ModbusReadChannel<Long> bmsDCDC1OutputTotalDischargeEnergy;
	/*
	 * PV DCDC
	 */

	public ModbusReadChannel<Long> pvDCDCWorkState;
	public ModbusReadChannel<Long> pvDCDCWorkMode;
	public ModbusReadChannel<Long> pvDCDCSuggestiveInformation1;
	public ModbusReadChannel<Long> pvDCDCSuggestiveInformation2;
	public ModbusReadChannel<Long> pvDCDCSuggestiveInformation3;
	public ModbusReadChannel<Long> pvDCDCSuggestiveInformation4;
	public ModbusReadChannel<Long> pvDCDCSuggestiveInformation5;
	public ModbusReadChannel<Long> pvDCDCAbnormity1;
	public ModbusReadChannel<Long> pvDCDCAbnormity2;
	public ModbusReadChannel<Long> pvDCDCAbnormity3;
	public ModbusReadChannel<Long> pvDCDCAbnormity4;
	public ModbusReadChannel<Long> pvDCDCAbnormity5;
	public ModbusReadChannel<Long> pvDCDCAbnormity6;
	public ModbusReadChannel<Long> pvDCDCAbnormity7;
	public ModbusReadChannel<Long> pvDCDCSwitchState;
	public ModbusReadChannel<Long> pvDCDCOutputVoltage;
	public ModbusReadChannel<Long> pvDCDCOutputCurrent;
	public ModbusReadChannel<Long> pvDCDCOutputPower;
	public ModbusReadChannel<Long> pvDCDCInputVoltage;
	public ModbusReadChannel<Long> pvDCDCInputCurrent;
	public ModbusReadChannel<Long> pvDCDCInputPower;
	public ModbusReadChannel<Long> pvDCDCInputEnergy;
	public ModbusReadChannel<Long> pvDCDCOutputEnergy;
	public ModbusReadChannel<Long> pvDCDCReactorTemperature;
	public ModbusReadChannel<Long> pvDCDCIgbtTemperature;
	public ModbusReadChannel<Long> pvDCDCInputTotalChargeEnergy;
	public ModbusReadChannel<Long> pvDCDCInputTotalDischargeEnergy;
	public ModbusReadChannel<Long> pvDCDCOutputTotalChargeEnergy;
	public ModbusReadChannel<Long> pvDCDCOutputTotalDischargeEnergy;

	/*
	 * PV DCDC 1
	 */

	public ModbusReadChannel<Long> pvDCDC1WorkState;
	public ModbusReadChannel<Long> pvDCDC1WorkMode;
	public ModbusReadChannel<Long> pvDCDC1SuggestiveInformation1;
	public ModbusReadChannel<Long> pvDCDC1SuggestiveInformation2;
	public ModbusReadChannel<Long> pvDCDC1SuggestiveInformation3;
	public ModbusReadChannel<Long> pvDCDC1SuggestiveInformation4;
	public ModbusReadChannel<Long> pvDCDC1SuggestiveInformation5;
	public ModbusReadChannel<Long> pvDCDC1Abnormity1;
	public ModbusReadChannel<Long> pvDCDC1Abnormity2;
	public ModbusReadChannel<Long> pvDCDC1Abnormity3;
	public ModbusReadChannel<Long> pvDCDC1Abnormity4;
	public ModbusReadChannel<Long> pvDCDC1Abnormity5;
	public ModbusReadChannel<Long> pvDCDC1Abnormity6;
	public ModbusReadChannel<Long> pvDCDC1Abnormity7;
	public ModbusReadChannel<Long> pvDCDC1SwitchState;
	public ModbusReadChannel<Long> pvDCDC1OutputVoltage;
	public ModbusReadChannel<Long> pvDCDC1OutputCurrent;
	public ModbusReadChannel<Long> pvDCDC1OutputPower;
	public ModbusReadChannel<Long> pvDCDC1InputVoltage;
	public ModbusReadChannel<Long> pvDCDC1InputCurrent;
	public ModbusReadChannel<Long> pvDCDC1InputPower;
	public ModbusReadChannel<Long> pvDCDC1InputEnergy;
	public ModbusReadChannel<Long> pvDCDC1OutputEnergy;
	public ModbusReadChannel<Long> pvDCDC1ReactorTemperature;
	public ModbusReadChannel<Long> pvDCDC1IgbtTemperature;
	public ModbusReadChannel<Long> pvDCDC1InputTotalChargeEnergy;
	public ModbusReadChannel<Long> pvDCDC1InputTotalDischargeEnergy;
	public ModbusReadChannel<Long> pvDCDC1OutputTotalChargeEnergy;
	public ModbusReadChannel<Long> pvDCDC1OutputTotalDischargeEnergy;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol(//
				//				new WriteableModbusRegisterRange(0x0503, new UnsignedWordElement(0x0503,
				//						pvPowerLimitCommand = new ModbusWriteLongChannel("PvPowerLimitCommand", this).multiplier(2)
				//						.unit("W"))),
				//				new ModbusRegisterRange(0xA000, //
				//						new UnsignedWordElement(0xA000,
				//								bmsDCDCWorkState = new ModbusReadLongChannel("BmsDCDCWorkState", this)//
				//								.label(2, "Initial")//
				//								.label(4, "Stop")//
				//								.label(8, "Ready")//
				//								.label(16, "Running")//
				//								.label(32, "Fault")//
				//								.label(64, "Debug")//
				//								.label(128, "Locked")),
				//						new UnsignedWordElement(0xA001,
				//								bmsDCDCWorkMode = new ModbusReadLongChannel("BmsDCDCWorkMode", this)//
				//								.label(128, "Constant Current")//
				//								.label(256, "Constant Voltage")//
				//								.label(512, "Boost MPPT"))),
				//				new ModbusRegisterRange(0xA100, //
				//						new UnsignedWordElement(0xA100,//
				//								new ModbusBitWrappingChannel("BmsDCDCSuggestiveInformation1" , this, this.thingState)//
				//								.warningBit(0, WarningCharger.CurrentSamplingChannelAbnormityOnHighVoltageSide)//
				//								.warningBit(1, WarningCharger.CurrentSamplingChannelAbnormityOnLowVoltageSide)//
				//								.warningBit(6, WarningCharger.EEPROMParametersOverRange)//
				//								.warningBit(7, WarningCharger.UpdateEEPROMFailed)//
				//								.warningBit(8, WarningCharger.ReadEEPROMFailed)//
				//								.warningBit(9, WarningCharger.CurrentSamplingChannelAbnormityBeforeInductance)//
				//								),//

				//						new UnsignedWordElement(0xA101, //
				//								new ModbusBitWrappingChannel("BmsDCDCSuggestiveInformation2", this, this.thingState)//
				//								.warningBit(0, WarningCharger.ReactorPowerDecreaseCausedByOvertemperature)//
				//								.warningBit(1, WarningCharger.IGBTPowerDecreaseCausedByOvertemperature)//
				//								.warningBit(2, WarningCharger.TemperatureChanel3PowerDecreaseCausedByOvertemperature)//
				//								.warningBit(3, WarningCharger.TemperatureChanel4PowerDecreaseCausedByOvertemperature)//
				//								.warningBit(4, WarningCharger.TemperatureChanel5PowerDecreaseCausedByOvertemperature)//
				//								.warningBit(5, WarningCharger.TemperatureChanel6PowerDecreaseCausedByOvertemperature)//
				//								.warningBit(6, WarningCharger.TemperatureChanel7PowerDecreaseCausedByOvertemperature)//
				//								.warningBit(7, WarningCharger.TemperatureChanel8PowerDecreaseCausedByOvertemperature)//
				//								.warningBit(8, WarningCharger.Fan1StopFailed)//
				//								.warningBit(9, WarningCharger.Fan2StopFailed)//
				//								.warningBit(10,WarningCharger.Fan3StopFailed)//
				//								.warningBit(11,WarningCharger.Fan4StopFailed)//
				//								.warningBit(12,WarningCharger.Fan1StartupFailed)//
				//								.warningBit(13,WarningCharger.Fan2StartupFailed)//
				//								.warningBit(14,WarningCharger.Fan3StartupFailed)//
				//								.warningBit(15,WarningCharger.Fan4StartupFailed)//
				//								),//

				//						new UnsignedWordElement(0xA102,//
				//								new ModbusBitWrappingChannel("BmsDCDCSuggestiveInformation3", this, this.thingState)//
				//								.warningBit(0, WarningCharger.HighVoltageSideOvervoltage)//
				//								.warningBit(1, WarningCharger.HighVoltageSideUndervoltage)//
				//								.warningBit(2, WarningCharger.HighVoltageSideVoltageChangeUnconventionally)//
				//								),//

				new UnsignedWordElement(0xA103, //
						new ModbusBitWrappingChannel("BmsDCDCSuggestiveInformation4", this, this.thingState)//
						.warningBit(0, WarningCharger.CurrentAbnormityBeforeDCConverterWorkOnHighVoltageSide)
						.warningBit(1, WarningCharger.CurrentAbnormityBeforeDCConverterWorkOnLowVoltageSXide)
						.warningBit(2, WarningCharger.InitialDutyRatioAbnormityBeforeDCConverterWork)
						.warningBit(3, WarningCharger.VoltageAbnormityBeforeDCConverterWorkOnHighVoltageSide)
						.warningBit(4, WarningCharger.VoltageAbnormityBeforeDCConverterWorkOnLowVoltageSide)
						),//

				new UnsignedWordElement(0xA104,
						new ModbusBitWrappingChannel("BmsDCDCSuggestiveInformation5", this, this.thingState)//
						.warningBit(0, WarningCharger.HighVoltageBreakerInspectionAbnormity)//
						.warningBit(1, WarningCharger.LowVoltageBreakerInspectionAbnormity)//
						.warningBit(2, WarningCharger.BsmDCDC5DCPrechargeContactorInspectionAbnormity)//
						.warningBit(3, WarningCharger.DCPrechargeContactorOpenUnsuccessfully)//
						.warningBit(4, WarningCharger.DCMainContactorInspectionAbnormity)//
						.warningBit(5, WarningCharger.DCMainContactorOpenUnsuccessfully)//
						.warningBit(6, WarningCharger.OutputContactorCloseUnsuccessfully)//
						.warningBit(7, WarningCharger.OutputContactorOpenUnsuccessfully)//
						.warningBit(8, WarningCharger.ACMainContactorCloseUnsuccessfully)//
						.warningBit(9, WarningCharger.ACMainContactorOpenUnsuccessfully)//
						.warningBit(10,WarningCharger.NegContactorOpenUnsuccessfully)//
						.warningBit(11,WarningCharger.NegContactorCloseUnsuccessfully)//
						.warningBit(12,WarningCharger.NegContactorStateAbnormal)//
						),//

				new DummyElement(0xA105, 0xA10F),
				new UnsignedWordElement(0xA110,//
						new ModbusBitWrappingChannel("BmsDCDCAbnormity1", this, this.thingState)//
						.faultBit(0, FaultCharger.HighVoltageSideOfDCConverterUndervoltage)//
						.faultBit(1, FaultCharger.HighVoltageSideOfDCConverterOvervoltage)//
						.faultBit(2, FaultCharger.LowVoltageSideOfDCConverterUndervoltage)//
						.faultBit(3, FaultCharger.LowVoltageSideOfDCConverterOvervoltage)//
						.faultBit(4, FaultCharger.HighVoltageSideOfDCConverterOvercurrentFault)//
						.faultBit(5, FaultCharger.LowVoltageSideOfDCConverterOvercurrentFault)//
						.faultBit(6, FaultCharger.DCConverterIGBTFault)//
						.faultBit(7, FaultCharger.DCConverterPrechargeUnmet)//
						),//

				new UnsignedWordElement(0xA111,
						new ModbusBitWrappingChannel("BmsDCDCAbnormity2", this, this.thingState)//
						.faultBit(0, FaultCharger.BECUCommunicationDisconnected)//
						.faultBit(1, FaultCharger.DCConverterCommunicationDisconnected)//
						.faultBit(2, FaultCharger.CurrentConfigurationOverRange)//
						.faultBit(3, FaultCharger.TheBatteryRequestStop)//
						.faultBit(5, FaultCharger.OvercurrentRelayFault)//
						.faultBit(6, FaultCharger.LightningProtectionDeviceFault)//
						.faultBit(7, FaultCharger.DCConverterPriamaryContactorDisconnectedAbnormally)//
						.faultBit(9, FaultCharger.DCDisconnectedAbnormallyOnLowVoltageSideOfDCConvetor)//
						.faultBit(12,FaultCharger.DCConvetorEEPROMAbnormity1)//
						.faultBit(13,FaultCharger.DCConvetorEEPROMAbnormity1Second)//
						.faultBit(14,FaultCharger.EDCConvetorEEPROMAbnormity1)//
						),//

				new UnsignedWordElement(0xA112,//
						new ModbusBitWrappingChannel("BmsDCDCAbnormity3", this, this.thingState)//
						.faultBit(0, FaultCharger.DCConvertorGeneralOverload)//
						.faultBit(1, FaultCharger.DCShortCircuit)//
						.faultBit(2, FaultCharger.PeakPulseCurrentProtection)//
						.faultBit(3, FaultCharger.DCDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
						.faultBit(4, FaultCharger.EffectivePulseValueOverhigh)//
						.faultBit(5, FaultCharger.DCConverteSevereOverload)//
						.faultBit(6, FaultCharger.DCBreakerDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
						.faultBit(7, FaultCharger.DCBreakerDisconnectAbnormallyOnLowVoltageSideOfDCConvetor)//
						.faultBit(8, FaultCharger.DCConvetorPrechargeContactorCloseFailed)//
						.faultBit(9, FaultCharger.DCConvetorMainContactorCloseFailed)//
						.faultBit(10,FaultCharger.ACContactorStateAbnormityOfDCConvetor)//
						.faultBit(11,FaultCharger.DCConvetorEmergencyStop)//
						.faultBit(12,FaultCharger.DCConverterChargingGunDisconnected)//
						.faultBit(13,FaultCharger.DCCurrentAbnormityBeforeDCConvetorWork)//
						.faultBit(14,FaultCharger.FuSeDisconnected)//
						.faultBit(15,FaultCharger.DCConverterHardwareCurrentOrVoltageFault)//
						),//

				new UnsignedWordElement(0xA113,//
						new ModbusBitWrappingChannel("BmsDCDCAbnormity4", this, this.thingState)//
						.faultBit(0, FaultCharger.DCConverterCrystalOscillatorCircuitInvalidation)//
						.faultBit(1, FaultCharger.DCConverterResetCircuitInvalidation)//
						.faultBit(2, FaultCharger.DCConverterSamplingCircuitInvalidation)//
						.faultBit(3, FaultCharger.DCConverterDigitalIOCircuitInvalidation)//
						.faultBit(4, FaultCharger.DCConverterPWMCircuitInvalidation)//
						.faultBit(5, FaultCharger.DCConverterX5045CircuitInvalidation)//
						.faultBit(6, FaultCharger.DCConverterCANCircuitInvalidation)//
						.faultBit(7, FaultCharger.DCConverterSoftwareANDHardwareProtectionCircuitInvalidation)//
						.faultBit(8, FaultCharger.DCConverterPowerCircuitInvalidation)//
						.faultBit(9, FaultCharger.DCConverterCPUInvalidation)//
						.faultBit(10,FaultCharger.DCConverterTINT0InterruptInvalidation)//
						.faultBit(11,FaultCharger.DCConverterADCInterruptInvalidation)//
						.faultBit(12,FaultCharger.DCConverterCAPITN4InterruptInvalidation)//
						.faultBit(13,FaultCharger.DCConverterCAPINT6InterruptInvalidation)//
						.faultBit(14,FaultCharger.DCConverterT3PINTinterruptInvalidation)//
						.faultBit(15,FaultCharger.DCConverterT4PINTinterruptInvalidation)//
						),//

				new UnsignedWordElement(0xA114,//
						new ModbusBitWrappingChannel("BmsDCDCAbnormity5", this, this.thingState)//
						.faultBit(0, FaultCharger.DCConverterPDPINTAInterruptInvalidation)//
						.faultBit(1, FaultCharger.DCConverterT1PINTInterruptInvalidation)//
						.faultBit(2, FaultCharger.DCConverterRESVInterruptInvalidation)//
						.faultBit(3, FaultCharger.DCConverter100usTaskInvalidation)//
						.faultBit(4, FaultCharger.DCConverterClockInvalidation)//
						.faultBit(5, FaultCharger.DCConverterEMSMemoryInvalidation)//
						.faultBit(6, FaultCharger.DCConverterExteriorCommunicationInvalidation)//
						.faultBit(7, FaultCharger.DCConverterIOInterfaceInvalidation)//
						.faultBit(8, FaultCharger.DCConverterInputVoltageBoundFault)//
						.faultBit(9, FaultCharger.DCConverterOutterVoltageBoundFault)//
						.faultBit(10,FaultCharger.DCConverterOutputVoltageBoundFault)//
						.faultBit(11,FaultCharger.DCConverterInductCurrentBoundFault)//
						.faultBit(12,FaultCharger.DCConverterInputCurrentBoundFault)//
						.faultBit(13,FaultCharger.DCConverterOutputCurrentBoundFault)//
						),//

				new UnsignedWordElement(0xA115,//
						new ModbusBitWrappingChannel("BmsDCDCAbnormity6", this, this.thingState)//
						.faultBit(0, FaultCharger.DCReactorOverTemperature)//
						.faultBit(1, FaultCharger.DCIGBTOverTemperature)//
						.faultBit(2, FaultCharger.DCConverterChanel3OverTemperature)//
						.faultBit(3, FaultCharger.DCConverterChanel4OverTemperature)//
						.faultBit(4, FaultCharger.DCConverterChanel5OverTemperature)//
						.faultBit(5, FaultCharger.DCConverterChanel6OverTemperature)//
						.faultBit(6, FaultCharger.DCConverterChanel7OverTemperature)//
						.faultBit(7, FaultCharger.DCConverterChanel8OverTemperature)//
						.faultBit(8, FaultCharger.DCReactorTemperatureSamplingInvalidation)//
						.faultBit(9, FaultCharger.DCIGBTTemperatureSamplingInvalidation)//
						.faultBit(10,FaultCharger.DCConverterChanel3TemperatureSamplingInvalidation)//
						.faultBit(11,FaultCharger.DCConverterChanel4TemperatureSamplingInvalidation)//
						.faultBit(12,FaultCharger.DCConverterChanel5TemperatureSamplingInvalidation)//
						.faultBit(13,FaultCharger.DCConverterChanel6TemperatureSamplingInvalidation)//
						.faultBit(14,FaultCharger.DCConverterChanel7TemperatureSamplingInvalidation)//
						.faultBit(15,FaultCharger.DCConverterChanel8TemperatureSamplingInvalidation)//
						),//

				new UnsignedWordElement(0xA116,
						new ModbusBitWrappingChannel("BmsDCDCAbnormity7", this, this.thingState)//
						.faultBit(4, FaultCharger.DCConverterInductanceCurrentSamplingInvalidation)//
						.faultBit(5, FaultCharger.CurrentSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
						.faultBit(6, FaultCharger.VoltageSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
						.faultBit(7, FaultCharger.InsulationInspectionFault)//
						.faultBit(8, FaultCharger.NegContactorCloseUnsuccessly)//
						.faultBit(9, FaultCharger.NegContactorCutWhenRunning)//
						),//

				new DummyElement(0xA117, 0xA11F),
				new UnsignedWordElement(0xA120,
						bmsDCDCSwitchState = new StatusBitChannel("BmsDCDCSwitchState", this)//
						.label(1, "DC precharge contactor")//
						.label(2, "DC main contactor")//
						.label(4, "Output contactor")//
						.label(8, "Output breaker")//
						.label(16, "Input breaker")//
						.label(32, "AC contactor")//
						.label(64, "Emergency stop button")//
						.label(128, "NegContactor"))),
				new ModbusRegisterRange(0xA130, //
						new SignedWordElement(0xA130,
								bmsDCDCOutputVoltage = new ModbusReadLongChannel("BmsDCDCOutputVoltage", this)
								.unit("mV").multiplier(2)),
						new SignedWordElement(0xA131,
								bmsDCDCOutputCurrent = new ModbusReadLongChannel("BmsDCDCOutputCurrent", this)
								.unit("mA").multiplier(2)),
						new SignedWordElement(0xA132,
								bmsDCDCOutputPower = new ModbusReadLongChannel("BmsDCDCOutputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xA133,
								bmsDCDCInputVoltage = new ModbusReadLongChannel("BmsDCDCInputVoltage", this).unit("mV")
								.multiplier(2)),
						new SignedWordElement(0xA134,
								bmsDCDCInputCurrent = new ModbusReadLongChannel("BmsDCDCInputCurrent", this).unit("mA")
								.multiplier(2)),
						new SignedWordElement(0xA135,
								bmsDCDCInputPower = new ModbusReadLongChannel("BmsDCDCInputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xA136,
								bmsDCDCInputEnergy = new ModbusReadLongChannel("BmsDCDCInputEnergy", this).unit("Wh")
								.multiplier(2)),
						new SignedWordElement(0xA137,
								bmsDCDCOutputEnergy = new ModbusReadLongChannel("BmsDCDCOutputEnergy", this).unit("Wh")
								.multiplier(2)),
						new DummyElement(0xA138, 0xA13F),
						new SignedWordElement(0xA140,
								bmsDCDCReactorTemperature = new ModbusReadLongChannel("BmsDCDCReactorTemperature", this)
								.unit("°C")),
						new SignedWordElement(0xA141,
								bmsDCDCIgbtTemperature = new ModbusReadLongChannel("BmsDCDCIgbtTemperature", this)
								.unit("°C")),
						new DummyElement(0xA142, 0xA14F),
						new UnsignedDoublewordElement(0xA150,
								bmsDCDCInputTotalChargeEnergy = new ModbusReadLongChannel(
										"BmsDCDCInputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA152,
								bmsDCDCInputTotalDischargeEnergy = new ModbusReadLongChannel(
										"BmsDCDCInputTotalDischargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA154,
								bmsDCDCOutputTotalChargeEnergy = new ModbusReadLongChannel(
										"BmsDCDCOutputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA156,
								bmsDCDCOutputTotalDischargeEnergy = new ModbusReadLongChannel(
										"BmsDCDCOutputTotalDischargeEnergy", this).unit("Wh")
								.multiplier(2)).wordOrder(WordOrder.LSWMSW)),
				new ModbusRegisterRange(0xA300, //
						new UnsignedWordElement(0xA300,
								bmsDCDC1WorkState = new ModbusReadLongChannel("BmsDCDC1WorkState", this)//
								.label(2, "Initial")//
								.label(4, "Stop")//
								.label(8, "Ready")//
								.label(16, "Running")//
								.label(32, "Fault")//
								.label(64, "Debug")//
								.label(128, "Locked")),
						new UnsignedWordElement(0xA301,
								bmsDCDC1WorkMode = new ModbusReadLongChannel("BmsDCDC1WorkMode", this)//
								.label(128, "Constant Current")//
								.label(256, "Constant Voltage")//
								.label(512, "Boost MPPT"))),
				new ModbusRegisterRange(0xA400, //
						new UnsignedWordElement(0xA400,//
								//used BsmDCDC1 as prefix on each one if it is used before
								new ModbusBitWrappingChannel("BmsDCDC1SuggestiveInformation1", this, this.thingState)//
								.warningBit(0, WarningCharger.BsmDCDC1CurrentSamplingChannelAbnormityOnHighVoltageSide)//
								.warningBit(1, WarningCharger.BsmDCDC1CurrentSamplingChannelAbnormityOnLowVoltageSide)//
								.warningBit(6, WarningCharger.BmsDCDC1EEPROMParametersOverRange)//
								.warningBit(7, WarningCharger.BsmDCDC1UpdateEEPROMFailed)//
								.warningBit(8, WarningCharger.BsmDCDC1ReadEEPROMFailed)//
								.warningBit(9, WarningCharger.BsmDCDC1CurrentSamplingChannelAbnormityBeforeInductance)//
								),//

						new UnsignedWordElement(0xA401, //
								//Prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1SuggestiveInformation2", this, this.thingState)//
								.warningBit(0, WarningCharger.BsmDCDC1ReactorPowerDecreaseCausedByOvertemperature)//
								.warningBit(1, WarningCharger.BsmDCDC1IGBTPowerDecreaseCausedByOvertemperature)//
								.warningBit(2, WarningCharger.BsmDCDC1TemperatureChanel3PowerDecreaseCausedByOvertemperature)//
								.warningBit(3, WarningCharger.BsmDCDC1TemperatureChanel4PowerDecreaseCausedByOvertemperature)//
								.warningBit(4, WarningCharger.BsmDCDC1TemperatureChanel5PowerDecreaseCausedByOvertemperature)//
								.warningBit(5, WarningCharger.BsmDCDC1TemperatureChanel6PowerDecreaseCausedByOvertemperature)//
								.warningBit(6, WarningCharger.BsmDCDC1TemperatureChanel7PowerDecreaseCausedByOvertemperature)//
								.warningBit(7, WarningCharger.BsmDCDC1TemperatureChanel8PowerDecreaseCausedByOvertemperature)//
								.warningBit(8, WarningCharger.BsmDCDC1Fan1StopFailed)//
								.warningBit(9, WarningCharger.BsmDCDC1Fan2StopFailed)//
								.warningBit(10,WarningCharger.BsmDCDC1Fan3StopFailed)//
								.warningBit(11,WarningCharger.BsmDCDC1Fan4StopFailed)//
								.warningBit(12,WarningCharger.BsmDCDC1Fan1StartupFailed)//
								.warningBit(13,WarningCharger.BsmDCDC1Fan2StartupFailed)//
								.warningBit(14,WarningCharger.BsmDCDC1Fan3StartupFailed)//
								.warningBit(15,WarningCharger.BsmDCDC1Fan4StartupFailed)//
								),//
						new UnsignedWordElement(0xA402,//
								//prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1SuggestiveInformation3", this, this.thingState)//
								.warningBit(0, WarningCharger.BsmDCDC1HighVoltageSideOvervoltage)//
								.warningBit(1, WarningCharger.BsmDCDC1HighVoltageSideUndervoltage)//
								.warningBit(2, WarningCharger.BsmDCDC1HighVoltageSideVoltageChangeUnconventionally)//
								),//

						new UnsignedWordElement(0xA403, //
								//prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1SuggestiveInformation4", this, this.thingState)//
								.warningBit(0, WarningCharger.BmsDCDC1CurrentAbnormityBeforeDCConverterWorkOnHighVoltageSide)
								.warningBit(1, WarningCharger.BmsDCDC1CurrentAbnormityBeforeDCConverterWorkOnLowVoltageSXide)
								.warningBit(2, WarningCharger.BmsDCDC1InitialDutyRatioAbnormityBeforeDCConverterWork)
								.warningBit(3, WarningCharger.BmsDCDC1VoltageAbnormityBeforeDCConverterWorkOnHighVoltageSide)
								.warningBit(4, WarningCharger.BmsDCDC1VoltageAbnormityBeforeDCConverterWorkOnLowVoltageSide)
								),//

						new UnsignedWordElement(0xA404,//
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1SuggestiveInformation5", this, this.thingState)//
								.warningBit(0, WarningCharger.BmsDCDC1HighVoltageBreakerInspectionAbnormity)//
								.warningBit(1, WarningCharger.BmsDCDC1LowVoltageBreakerInspectionAbnormity)//
								.warningBit(2, WarningCharger.BmsDCDC1BsmDCDC5DCPrechargeContactorInspectionAbnormity)//
								.warningBit(3, WarningCharger.BmsDCDC1DCPrechargeContactorOpenUnsuccessfully)//
								.warningBit(4, WarningCharger.BmsDCDC1DCMainContactorInspectionAbnormity)//
								.warningBit(5, WarningCharger.BmsDCDC1DCMainContactorOpenUnsuccessfully)//
								.warningBit(6, WarningCharger.BmsDCDC1OutputContactorCloseUnsuccessfully)//
								.warningBit(7, WarningCharger.BmsDCDC1OutputContactorOpenUnsuccessfully)//
								.warningBit(8, WarningCharger.BmsDCDC1ACMainContactorCloseUnsuccessfully)//
								.warningBit(9, WarningCharger.BmsDCDC1ACMainContactorOpenUnsuccessfully)//
								.warningBit(10,WarningCharger.BmsDCDC1NegContactorOpenUnsuccessfully)//
								.warningBit(11,WarningCharger.BmsDCDC1NegContactorCloseUnsuccessfully)//
								.warningBit(12,WarningCharger.BmsDCDC1NegContactorStateAbnormal)//
								),//

						new DummyElement(0xA405, 0xA40F),
						new UnsignedWordElement(0xA410,
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity1", this, this.thingState)//
								.faultBit(0, FaultCharger.BmsDCDC1HighVoltageSideOfDCConverterUndervoltage)//
								.faultBit(1, FaultCharger.BmsDCDC1HighVoltageSideOfDCConverterOvervoltage)//
								.faultBit(2, FaultCharger.BmsDCDC1LowVoltageSideOfDCConverterUndervoltage)//
								.faultBit(3, FaultCharger.BmsDCDC1LowVoltageSideOfDCConverterOvervoltage)//
								.faultBit(4, FaultCharger.BmsDCDC1HighVoltageSideOfDCConverterOvercurrentFault)//
								.faultBit(5, FaultCharger.BmsDCDC1LowVoltageSideOfDCConverterOvercurrentFault)//
								.faultBit(6, FaultCharger.BmsDCDC1DCConverterIGBTFault)//
								.faultBit(7, FaultCharger.BmsDCDC1DCConverterPrechargeUnmet)//
								),//

						new UnsignedWordElement(0xA411,
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity2", this, this.thingState)//
								.faultBit(0, FaultCharger.BmsDCDC1BECUCommunicationDisconnected)//
								.faultBit(1, FaultCharger.BmsDCDC1DCConverterCommunicationDisconnected)//
								.faultBit(2, FaultCharger.BmsDCDC1CurrentConfigurationOverRange)//
								.faultBit(3, FaultCharger.BmsDCDC1TheBatteryRequestStop)//
								.faultBit(5, FaultCharger.BmsDCDC1OvercurrentRelayFault)//
								.faultBit(6, FaultCharger.BmsDCDC1LightningProtectionDeviceFault)//
								.faultBit(7, FaultCharger.BmsDCDC1DCConverterPriamaryContactorDisconnectedAbnormally)//
								.faultBit(9, FaultCharger.BmsDCDC1DCDisconnectedAbnormallyOnLowVoltageSideOfDCConvetor)//
								.faultBit(12,FaultCharger.BmsDCDC1DCConvetorEEPROMAbnormity1)//
								.faultBit(13,FaultCharger.BmsDCDC1DCConvetorEEPROMAbnormity1Second)//
								.faultBit(14,FaultCharger.BmsDCDC1EDCConvetorEEPROMAbnormity1)//
								),//

						new UnsignedWordElement(0xA412,//
								//prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity3", this, this.thingState)//
								.faultBit(0, FaultCharger.BsmDCDC1DCConvertorGeneralOverload)//
								.faultBit(1, FaultCharger.BsmDCDC1DCShortCircuit)//
								.faultBit(2, FaultCharger.BsmDCDC1PeakPulseCurrentProtection)//
								.faultBit(3, FaultCharger.BsmDCDC1DCDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
								.faultBit(4, FaultCharger.BsmDCDC1EffectivePulseValueOverhigh)//
								.faultBit(5, FaultCharger.BsmDCDC1DCConverteSevereOverload)//
								.faultBit(6, FaultCharger.BsmDCDC1DCBreakerDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
								.faultBit(7, FaultCharger.BsmDCDC1DCBreakerDisconnectAbnormallyOnLowVoltageSideOfDCConvetor)//
								.faultBit(8, FaultCharger.BsmDCDC1DCConvetorPrechargeContactorCloseFailed)//
								.faultBit(9, FaultCharger.BsmDCDC1DCConvetorMainContactorCloseFailed)//
								.faultBit(10,FaultCharger.BsmDCDC1ACContactorStateAbnormityOfDCConvetor)//
								.faultBit(11,FaultCharger.BsmDCDC1DCConvetorEmergencyStop)//
								.faultBit(12,FaultCharger.BsmDCDC1DCConverterChargingGunDisconnected)//
								.faultBit(13,FaultCharger.BsmDCDC1DCCurrentAbnormityBeforeDCConvetorWork)//
								.faultBit(14,FaultCharger.BsmDCDC1FuSeDisconnected)//
								.faultBit(15,FaultCharger.BsmDCDC1DCConverterHardwareCurrentOrVoltageFault)//
								),//

						new UnsignedWordElement(0xA413,
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity4", this, this.thingState)//
								.faultBit(0, FaultCharger.BmsDCDC1DCConverterCrystalOscillatorCircuitInvalidation)//
								.faultBit(1, FaultCharger.BmsDCDC1DCConverterResetCircuitInvalidation)//
								.faultBit(2, FaultCharger.BmsDCDC1DCConverterSamplingCircuitInvalidation)//
								.faultBit(3, FaultCharger.BmsDCDC1DCConverterDigitalIOCircuitInvalidation)//
								.faultBit(4, FaultCharger.BmsDCDC1DCConverterPWMCircuitInvalidation)//
								.faultBit(5, FaultCharger.BmsDCDC1DCConverterX5045CircuitInvalidation)//
								.faultBit(6, FaultCharger.BmsDCDC1DCConverterCANCircuitInvalidation)//
								.faultBit(7, FaultCharger.BmsDCDC1DCConverterSoftwareANDHardwareProtectionCircuitInvalidation)//
								.faultBit(8, FaultCharger.BmsDCDC1DCConverterPowerCircuitInvalidation)//
								.faultBit(9, FaultCharger.BmsDCDC1DCConverterCPUInvalidation)//
								.faultBit(10,FaultCharger.BmsDCDC1DCConverterTINT0InterruptInvalidation)//
								.faultBit(11,FaultCharger.BmsDCDC1DCConverterADCInterruptInvalidation)//
								.faultBit(12,FaultCharger.BmsDCDC1DCConverterCAPITN4InterruptInvalidation)//
								.faultBit(13,FaultCharger.BmsDCDC1DCConverterCAPINT6InterruptInvalidation)//
								.faultBit(14,FaultCharger.BmsDCDC1DCConverterT3PINTinterruptInvalidation)//
								.faultBit(15,FaultCharger.BmsDCDC1DCConverterT4PINTinterruptInvalidation)//
								),//
						new UnsignedWordElement(0xA414,
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity5", this, this.thingState)//
								.faultBit(0, FaultCharger.BmsDCDC1DCConverterPDPINTAInterruptInvalidation)//
								.faultBit(1, FaultCharger.BmsDCDC1DCConverterT1PINTInterruptInvalidationSecond)//
								.faultBit(2, FaultCharger.BmsDCDC1DCConverterRESVInterruptInvalidation)//
								.faultBit(3, FaultCharger.BmsDCDC1DCConverter100usTaskInvalidation)//
								.faultBit(4, FaultCharger.BmsDCDC1DCConverterClockInvalidation)//
								.faultBit(5, FaultCharger.BmsDCDC1DCConverterEMSMemoryInvalidation)//
								.faultBit(6, FaultCharger.BmsDCDC1DCConverterExteriorCommunicationInvalidation)//
								.faultBit(7, FaultCharger.BmsDCDC1DCConverterIOInterfaceInvalidation)//
								.faultBit(8, FaultCharger.BmsDCDC1DCConverterInputVoltageBoundFault)//
								.faultBit(9, FaultCharger.BmsDCDC1DCConverterOutterVoltageBoundFault)//
								.faultBit(10,FaultCharger.BmsDCDC1DCConverterOutputVoltageBoundFault)//
								.faultBit(11,FaultCharger.BmsDCDC1DCConverterInductCurrentBoundFault)//
								.faultBit(12,FaultCharger.BmsDCDC1DCConverterInputCurrentBoundFault)//
								.faultBit(13,FaultCharger.BmsDCDC1DCConverterOutputCurrentBoundFault)//
								),//
						new UnsignedWordElement(0xA415,
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity6", this, this.thingState)//
								.faultBit(0, FaultCharger.BmsDCDC1DCReactorOverTemperature)//
								.faultBit(1, FaultCharger.BmsDCDC1DCIGBTOverTemperature)//
								.faultBit(2, FaultCharger.BmsDCDC1DCConverterChanel3OverTemperature)//
								.faultBit(3, FaultCharger.BmsDCDC1DCConverterChanel4OverTemperature)//
								.faultBit(4, FaultCharger.BmsDCDC1DCConverterChanel5OverTemperature)//
								.faultBit(5, FaultCharger.BmsDCDC1DCConverterChanel6OverTemperature)//
								.faultBit(6, FaultCharger.BmsDCDC1DCConverterChanel7OverTemperature)//
								.faultBit(7, FaultCharger.BmsDCDC1DCConverterChanel8OverTemperature)//
								.faultBit(8, FaultCharger.BmsDCDC1DCReactorTemperatureSamplingInvalidation)//
								.faultBit(9, FaultCharger.BmsDCDC1DCIGBTTemperatureSamplingInvalidation)//
								.faultBit(10,FaultCharger.BmsDCDC1DCConverterChanel3TemperatureSamplingInvalidation)//
								.faultBit(11,FaultCharger.BmsDCDC1DCConverterChanel4TemperatureSamplingInvalidation)//
								.faultBit(12,FaultCharger.BmsDCDC1DCConverterChanel5TemperatureSamplingInvalidation)//
								.faultBit(13,FaultCharger.BmsDCDC1DCConverterChanel6TemperatureSamplingInvalidation)//
								.faultBit(14,FaultCharger.BmsDCDC1DCConverterChanel7TemperatureSamplingInvalidation)//
								.faultBit(15,FaultCharger.BmsDCDC1DCConverterChanel8TemperatureSamplingInvalidation)//
								),//
						new UnsignedWordElement(0xA416,
								// prefix = "BsmDCDC1"
								new ModbusBitWrappingChannel("BmsDCDC1Abnormity7", this, this.thingState)//
								.faultBit(4, FaultCharger.BmsDCDC1DCConverterInductanceCurrentSamplingInvalidation)//
								.faultBit(5, FaultCharger.BmsDCDC1CurrentSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
								.faultBit(6, FaultCharger.BmsDCDC1VoltageSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
								.faultBit(7, FaultCharger.BmsDCDC1InsulationInspectionFault)//
								.faultBit(8, FaultCharger.BmsDCDC1NegContactorCloseUnsuccessly)//
								.faultBit(9, FaultCharger.BmsDCDC1NegContactorCutWhenRunning)//
								),//
						new DummyElement(0xA417, 0xA41F),
						new UnsignedWordElement(0xA420,
								bmsDCDC1SwitchState = new StatusBitChannel("BmsDCDC1SwitchState", this)//
								.label(1, "DC precharge contactor")//
								.label(2, "DC main contactor")//
								.label(4, "Output contactor")//
								.label(8, "Output breaker")//
								.label(16, "Input breaker")//
								.label(32, "AC contactor")//
								.label(64, "Emergency stop button")//
								.label(128, "NegContactor"))),
				new ModbusRegisterRange(0xA430, //
						new SignedWordElement(0xA430,
								bmsDCDC1OutputVoltage = new ModbusReadLongChannel("BmsDCDC1OutputVoltage", this)
								.unit("mV").multiplier(2)),
						new SignedWordElement(0xA431,
								bmsDCDC1OutputCurrent = new ModbusReadLongChannel("BmsDCDC1OutputCurrent", this)
								.unit("mA").multiplier(2)),
						new SignedWordElement(0xA432,
								bmsDCDC1OutputPower = new ModbusReadLongChannel("BmsDCDC1OutputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xA433,
								bmsDCDC1InputVoltage = new ModbusReadLongChannel("BmsDCDC1InputVoltage", this)
								.unit("mV").multiplier(2)),
						new SignedWordElement(0xA434,
								bmsDCDC1InputCurrent = new ModbusReadLongChannel("BmsDCDC1InputCurrent", this)
								.unit("mA").multiplier(2)),
						new SignedWordElement(0xA435,
								bmsDCDC1InputPower = new ModbusReadLongChannel("BmsDCDC1InputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xA436,
								bmsDCDC1InputEnergy = new ModbusReadLongChannel("BmsDCDC1InputEnergy", this).unit("Wh")
								.multiplier(2)),
						new SignedWordElement(0xA437,
								bmsDCDC1OutputEnergy = new ModbusReadLongChannel("BmsDCDC1OutputEnergy", this)
								.unit("Wh").multiplier(2)),
						new DummyElement(0xA438, 0xA43F),
						new SignedWordElement(0xA440,
								bmsDCDC1ReactorTemperature = new ModbusReadLongChannel("BmsDCDC1ReactorTemperature",
										this).unit("°C")),
						new SignedWordElement(0xA441,
								bmsDCDC1IgbtTemperature = new ModbusReadLongChannel("BmsDCDC1IgbtTemperature", this)
								.unit("°C")),
						new DummyElement(0xA442, 0xA44F),
						new UnsignedDoublewordElement(0xA450,
								bmsDCDC1InputTotalChargeEnergy = new ModbusReadLongChannel(
										"BmsDCDC1InputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA452,
								bmsDCDC1InputTotalDischargeEnergy = new ModbusReadLongChannel(
										"BmsDCDC1InputTotalDischargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA454,
								bmsDCDC1OutputTotalChargeEnergy = new ModbusReadLongChannel(
										"BmsDCDC1OutputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA456,
								bmsDCDC1OutputTotalDischargeEnergy = new ModbusReadLongChannel(
										"BmsDCDC1OutputTotalDischargeEnergy", this).unit("Wh")
								.multiplier(2)).wordOrder(WordOrder.LSWMSW)),
				new ModbusRegisterRange(0xA600, //
						new UnsignedWordElement(0xA600,
								pvDCDCWorkState = new ModbusReadLongChannel("PvDCDCWorkState", this)//
								.label(2, "Initial")//
								.label(4, "Stop")//
								.label(8, "Ready")//
								.label(16, "Running")//
								.label(32, "Fault")//
								.label(64, "Debug")//
								.label(128, "Locked")),
						new UnsignedWordElement(0xA601,
								pvDCDCWorkMode = new ModbusReadLongChannel("PvDCDCWorkMode", this)//
								.label(128, "Constant Current")//
								.label(256, "Constant Voltage")//
								.label(512, "Boost MPPT"))),
				new ModbusRegisterRange(0xA700, //
						new UnsignedWordElement(0xA700,//
								// prefix  = "PvDCDC "
								new ModbusBitWrappingChannel("PvDCDCSuggestiveInformation1" , this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDCCurrentSamplingChannelAbnormityOnHighVoltageSide)//
								.warningBit(1, WarningCharger.PvDCDCCurrentSamplingChannelAbnormityOnLowVoltageSide)//
								.warningBit(6, WarningCharger.PvDCDCEEPROMParametersOverRange)//
								.warningBit(7, WarningCharger.PvDCDCUpdateEEPROMFailed)//
								.warningBit(8, WarningCharger.PvDCDCReadEEPROMFailed)//
								.warningBit(9, WarningCharger.PvDCDCCurrentSamplingChannelAbnormityBeforeInductance)//
								),//

						new UnsignedWordElement(0xA701, //
								//prefix  = "PvDCDC "
								new ModbusBitWrappingChannel("PvDCDCSuggestiveInformation2", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDCReactorPowerDecreaseCausedByOvertemperature)//
								.warningBit(1, WarningCharger.PvDCDCIGBTPowerDecreaseCausedByOvertemperature)//
								.warningBit(2, WarningCharger.PvDCDCTemperatureChanel3PowerDecreaseCausedByOvertemperature)//
								.warningBit(3, WarningCharger.PvDCDCTemperatureChanel4PowerDecreaseCausedByOvertemperature)//
								.warningBit(4, WarningCharger.PvDCDCTemperatureChanel5PowerDecreaseCausedByOvertemperature)//
								.warningBit(5, WarningCharger.PvDCDCTemperatureChanel6PowerDecreaseCausedByOvertemperature)//
								.warningBit(6, WarningCharger.PvDCDCTemperatureChanel7PowerDecreaseCausedByOvertemperature)//
								.warningBit(7, WarningCharger.PvDCDCTemperatureChanel8PowerDecreaseCausedByOvertemperature)//
								.warningBit(8, WarningCharger.PvDCDCFan1StopFailed)//
								.warningBit(9, WarningCharger.PvDCDCFan2StopFailed)//
								.warningBit(10,WarningCharger.PvDCDCFan3StopFailed)//
								.warningBit(11,WarningCharger.PvDCDCFan4StopFailed)//
								.warningBit(12,WarningCharger.PvDCDCFan1StartupFailed)//
								.warningBit(13,WarningCharger.PvDCDCFan2StartupFailed)//
								.warningBit(14,WarningCharger.PvDCDCFan3StartupFailed)//
								.warningBit(15,WarningCharger.PvDCDCFan4StartupFailed)//
								),//

						new UnsignedWordElement(0xA702,//
								// prefix  = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCSuggestiveInformation3", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDCHighVoltageSideOvervoltage)//
								.warningBit(1, WarningCharger.PvDCDCHighVoltageSideUndervoltage)//
								.warningBit(2, WarningCharger.PvDCDCHighVoltageSideVoltageChangeUnconventionally)//
								),//

						new UnsignedWordElement(0xA703,
								// prefix  = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCSuggestiveInformation4", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDCCurrentAbnormityBeforeDCConverterWorkOnHighVoltageSide)
								.warningBit(1, WarningCharger.PvDCDCCurrentAbnormityBeforeDCConverterWorkOnLowVoltageSXide)
								.warningBit(2, WarningCharger.PvDCDCInitialDutyRatioAbnormityBeforeDCConverterWork)
								.warningBit(3, WarningCharger.PvDCDCVoltageAbnormityBeforeDCConverterWorkOnHighVoltageSide)
								.warningBit(4, WarningCharger.PvDCDCVoltageAbnormityBeforeDCConverterWorkOnLowVoltageSide)
								),//

						new UnsignedWordElement(0xA704,//
								// prefix  = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCSuggestiveInformation5", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDCHighVoltageBreakerInspectionAbnormity)//
								.warningBit(1, WarningCharger.PvDCDCLowVoltageBreakerInspectionAbnormity)//
								.warningBit(2, WarningCharger.PvDCDCBsmDCDC5DCPrechargeContactorInspectionAbnormity)//
								.warningBit(3, WarningCharger.PvDCDCDCPrechargeContactorOpenUnsuccessfully)//
								.warningBit(4, WarningCharger.PvDCDCDCMainContactorInspectionAbnormity)//
								.warningBit(5, WarningCharger.PvDCDCDCMainContactorOpenUnsuccessfully)//
								.warningBit(6, WarningCharger.PvDCDCOutputContactorCloseUnsuccessfully)//
								.warningBit(7, WarningCharger.PvDCDCOutputContactorOpenUnsuccessfully)//
								.warningBit(8, WarningCharger.PvDCDCACMainContactorCloseUnsuccessfully)//
								.warningBit(9, WarningCharger.PvDCDCACMainContactorOpenUnsuccessfully)//
								.warningBit(10,WarningCharger.PvDCDCNegContactorOpenUnsuccessfully)//
								.warningBit(11,WarningCharger.PvDCDCNegContactorCloseUnsuccessfully)//
								.warningBit(12,WarningCharger.PvDCDCNegContactorStateAbnormal)//
								),//

						new DummyElement(0xA705, 0xA70F),
						new UnsignedWordElement(0xA710,//
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity1", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDCHighVoltageSideOfDCConverterUndervoltage)//
								.faultBit(1, FaultCharger.PvDCDCHighVoltageSideOfDCConverterOvervoltage)//
								.faultBit(2, FaultCharger.PvDCDCLowVoltageSideOfDCConverterUndervoltage)//
								.faultBit(3, FaultCharger.PvDCDCLowVoltageSideOfDCConverterOvervoltage)//
								.faultBit(4, FaultCharger.PvDCDCHighVoltageSideOfDCConverterOvercurrentFault)//
								.faultBit(5, FaultCharger.PvDCDCLowVoltageSideOfDCConverterOvercurrentFault)//
								.faultBit(6, FaultCharger.PvDCDCDCConverterIGBTFault)//
								.faultBit(7, FaultCharger.PvDCDCDCConverterPrechargeUnmet)//
								),//

						new UnsignedWordElement(0xA711,
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity2", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDCBECUCommunicationDisconnected)//
								.faultBit(1, FaultCharger.PvDCDCDCConverterCommunicationDisconnected)//
								.faultBit(2, FaultCharger.PvDCDCCurrentConfigurationOverRange)//
								.faultBit(3, FaultCharger.PvDCDCTheBatteryRequestStop)//
								.faultBit(5, FaultCharger.PvDCDCOvercurrentRelayFault)//
								.faultBit(6, FaultCharger.PvDCDCLightningProtectionDeviceFault)//
								.faultBit(7, FaultCharger.PvDCDCDCConverterPriamaryContactorDisconnectedAbnormally)//
								.faultBit(9, FaultCharger.PvDCDCDCDisconnectedAbnormallyOnLowVoltageSideOfDCConvetor)//
								.faultBit(12,FaultCharger.PvDCDCDCConvetorEEPROMAbnormity1)//
								.faultBit(13,FaultCharger.PvDCDCDCConvetorEEPROMAbnormity1Second)//
								.faultBit(14,FaultCharger.PvDCDCEDCConvetorEEPROMAbnormity1)//
								),//

						new UnsignedWordElement(0xA712,//
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity3", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDCDCConvertorGeneralOverload)//
								.faultBit(1, FaultCharger.PvDCDCDCShortCircuit)//
								.faultBit(2, FaultCharger.PvDCDCPeakPulseCurrentProtection)//
								.faultBit(3, FaultCharger.PvDCDCDCDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
								.faultBit(4, FaultCharger.PvDCDCEffectivePulseValueOverhigh)//
								.faultBit(5, FaultCharger.PvDCDCDCConverteSevereOverload)//
								.faultBit(6, FaultCharger.PvDCDCDCBreakerDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
								.faultBit(7, FaultCharger.PvDCDCDCBreakerDisconnectAbnormallyOnLowVoltageSideOfDCConvetor)//
								.faultBit(8, FaultCharger.PvDCDCDCConvetorPrechargeContactorCloseFailed)//
								.faultBit(9, FaultCharger.PvDCDCDCConvetorMainContactorCloseFailed)//
								.faultBit(10,FaultCharger.PvDCDCACContactorStateAbnormityOfDCConvetor)//
								.faultBit(11,FaultCharger.PvDCDCDCConvetorEmergencyStop)//
								.faultBit(12,FaultCharger.PvDCDCDCConverterChargingGunDisconnected)//
								.faultBit(13,FaultCharger.PvDCDCDCCurrentAbnormityBeforeDCConvetorWork)//
								.faultBit(14,FaultCharger.PvDCDCFuSeDisconnected)//
								.faultBit(15,FaultCharger.PvDCDCDCConverterHardwareCurrentOrVoltageFault)//
								),//

						new UnsignedWordElement(0xA713,//
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity4", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDCDCConverterCrystalOscillatorCircuitInvalidation)//
								.faultBit(1, FaultCharger.PvDCDCDCConverterResetCircuitInvalidation)//
								.faultBit(2, FaultCharger.PvDCDCDCConverterSamplingCircuitInvalidation)//
								.faultBit(3, FaultCharger.PvDCDCDCConverterDigitalIOCircuitInvalidation)//
								.faultBit(4, FaultCharger.PvDCDCDCConverterPWMCircuitInvalidation)//
								.faultBit(5, FaultCharger.PvDCDCDCConverterX5045CircuitInvalidation)//
								.faultBit(6, FaultCharger.PvDCDCDCConverterCANCircuitInvalidation)//
								.faultBit(7, FaultCharger.PvDCDCDCConverterSoftwareANDHardwareProtectionCircuitInvalidation)//
								.faultBit(8, FaultCharger.PvDCDCDCConverterPowerCircuitInvalidation)//
								.faultBit(9, FaultCharger.PvDCDCDCConverterCPUInvalidation)//
								.faultBit(10,FaultCharger.PvDCDCDCConverterTINT0InterruptInvalidation)//
								.faultBit(11,FaultCharger.PvDCDCDCConverterADCInterruptInvalidation)//
								.faultBit(12,FaultCharger.PvDCDCDCConverterCAPITN4InterruptInvalidation)//
								.faultBit(13,FaultCharger.PvDCDCDCConverterCAPINT6InterruptInvalidation)//
								.faultBit(14,FaultCharger.PvDCDCDCConverterT3PINTinterruptInvalidation)//
								.faultBit(15,FaultCharger.PvDCDCDCConverterT4PINTinterruptInvalidation)//
								),//

						new UnsignedWordElement(0xA714,
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity5", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDCConverterPDPINTAInterruptInvalidation)//
								.faultBit(1, FaultCharger.PvDCDCConverterT1PINTInterruptInvalidationSecond)//
								.faultBit(2, FaultCharger.PvDCDCConverterRESVInterruptInvalidation)//
								.faultBit(3, FaultCharger.PvDCDCConverter100usTaskInvalidation)//
								.faultBit(4, FaultCharger.PvDCDCConverterClockInvalidation)//
								.faultBit(5, FaultCharger.PvDCDCConverterEMSMemoryInvalidation)//
								.faultBit(6, FaultCharger.PvDCDCConverterExteriorCommunicationInvalidation)//
								.faultBit(7, FaultCharger.PvDCDCConverterIOInterfaceInvalidation)//
								.faultBit(8, FaultCharger.PvDCDCConverterInputVoltageBoundFault)//
								.faultBit(9, FaultCharger.PvDCDCConverterOutterVoltageBoundFault)//
								.faultBit(10,FaultCharger.PvDCDCConverterOutputVoltageBoundFault)//
								.faultBit(11,FaultCharger.PvDCDCConverterInductCurrentBoundFault)//
								.faultBit(12,FaultCharger.PvDCDCConverterInputCurrentBoundFault)//
								.faultBit(13,FaultCharger.PvDCDCConverterOutputCurrentBoundFault)//
								),//

						new UnsignedWordElement(0xA715,//
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity6", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDCDCReactorOverTemperature)//
								.faultBit(1, FaultCharger.PvDCDCDCIGBTOverTemperature)//
								.faultBit(2, FaultCharger.PvDCDCDCConverterChanel3OverTemperature)//
								.faultBit(3, FaultCharger.PvDCDCDCConverterChanel4OverTemperature)//
								.faultBit(4, FaultCharger.PvDCDCDCConverterChanel5OverTemperature)//
								.faultBit(5, FaultCharger.PvDCDCDCConverterChanel6OverTemperature)//
								.faultBit(6, FaultCharger.PvDCDCDCConverterChanel7OverTemperature)//
								.faultBit(7, FaultCharger.PvDCDCDCConverterChanel8OverTemperature)//
								.faultBit(8, FaultCharger.PvDCDCDCReactorTemperatureSamplingInvalidation)//
								.faultBit(9, FaultCharger.PvDCDCDCIGBTTemperatureSamplingInvalidation)//
								.faultBit(10,FaultCharger.PvDCDCDCConverterChanel3TemperatureSamplingInvalidation)//
								.faultBit(11,FaultCharger.PvDCDCDCConverterChanel4TemperatureSamplingInvalidation)//
								.faultBit(12,FaultCharger.PvDCDCDCConverterChanel5TemperatureSamplingInvalidation)//
								.faultBit(13,FaultCharger.PvDCDCDCConverterChanel6TemperatureSamplingInvalidation)//
								.faultBit(14,FaultCharger.PvDCDCDCConverterChanel7TemperatureSamplingInvalidation)//
								.faultBit(15,FaultCharger.PvDCDCDCConverterChanel8TemperatureSamplingInvalidation)//
								),//

						new UnsignedWordElement(0xA716,//
								// prefix = "PvDCDC"
								new ModbusBitWrappingChannel("PvDCDCAbnormity7", this, this.thingState)//
								.faultBit(4, FaultCharger.PvDCDCDCConverterInductanceCurrentSamplingInvalidation)//
								.faultBit(5, FaultCharger.PvDCDCCurrentSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
								.faultBit(6, FaultCharger.PvDCDCVoltageSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
								.faultBit(7, FaultCharger.PvDCDCInsulationInspectionFault)//
								.faultBit(8, FaultCharger.PvDCDCNegContactorCloseUnsuccessly)//
								.faultBit(9, FaultCharger.PvDCDCNegContactorCutWhenRunning)//
								),//

						new DummyElement(0xA717, 0xA71F),
						new UnsignedWordElement(0xA720,
								pvDCDCSwitchState = new StatusBitChannel("PvDCDCSwitchState", this)//
								.label(1, "DC precharge contactor")//
								.label(2, "DC main contactor")//
								.label(4, "Output contactor")//
								.label(8, "Output breaker")//
								.label(16, "Input breaker")//
								.label(32, "AC contactor")//
								.label(64, "Emergency stop button")//
								.label(128, "NegContactor"))),
				new ModbusRegisterRange(0xA730, //
						new SignedWordElement(0xA730,
								pvDCDCOutputVoltage = new ModbusReadLongChannel("PvDCDCOutputVoltage", this).unit("mV")
								.multiplier(2)),
						new SignedWordElement(0xA731,
								pvDCDCOutputCurrent = new ModbusReadLongChannel("PvDCDCOutputCurrent", this).unit("mA")
								.multiplier(2)),
						new SignedWordElement(0xA732,
								pvDCDCOutputPower = new ModbusReadLongChannel("PvDCDCOutputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xA733,
								pvDCDCInputVoltage = new ModbusReadLongChannel("PvDCDCInputVoltage", this).unit("mV")
								.multiplier(2)),
						new SignedWordElement(0xA734,
								pvDCDCInputCurrent = new ModbusReadLongChannel("PvDCDCInputCurrent", this).unit("mA")
								.multiplier(2)),
						new SignedWordElement(0xA735,
								pvDCDCInputPower = new ModbusReadLongChannel("PvDCDCInputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xA736,
								pvDCDCInputEnergy = new ModbusReadLongChannel("PvDCDCInputEnergy", this).unit("Wh")
								.multiplier(2)),
						new SignedWordElement(0xA737,
								pvDCDCOutputEnergy = new ModbusReadLongChannel("PvDCDCOutputEnergy", this).unit("Wh")
								.multiplier(2)),
						new DummyElement(0xA738, 0xA73F),
						new SignedWordElement(0xA740,
								pvDCDCReactorTemperature = new ModbusReadLongChannel("PvDCDCReactorTemperature", this)
								.unit("°C")),
						new SignedWordElement(0xA741,
								pvDCDCIgbtTemperature = new ModbusReadLongChannel("PvDCDCIgbtTemperature", this)
								.unit("°C")),
						new DummyElement(0xA742, 0xA74F),
						new UnsignedDoublewordElement(0xA750,
								pvDCDCInputTotalChargeEnergy = new ModbusReadLongChannel("PvDCDCInputTotalChargeEnergy",
										this).unit("Wh").multiplier(2)).wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA752,
								pvDCDCInputTotalDischargeEnergy = new ModbusReadLongChannel(
										"PvDCDCInputTotalDischargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA754,
								pvDCDCOutputTotalChargeEnergy = new ModbusReadLongChannel(
										"PvDCDCOutputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xA756,
								pvDCDCOutputTotalDischargeEnergy = new ModbusReadLongChannel(
										"PvDCDCOutputTotalDischargeEnergy", this).unit("Wh")
								.multiplier(2)).wordOrder(WordOrder.LSWMSW)),
				new ModbusRegisterRange(0xA900, //
						new UnsignedWordElement(0xA900,
								pvDCDC1WorkState = new ModbusReadLongChannel("PvDCDC1WorkState", this)//
								.label(2, "Initial")//
								.label(4, "Stop")//
								.label(8, "Ready")//
								.label(16, "Running")//
								.label(32, "Fault")//
								.label(64, "Debug")//
								.label(128, "Locked")),
						new UnsignedWordElement(0xA901,
								pvDCDC1WorkMode = new ModbusReadLongChannel("PvDCDC1WorkMode", this)//
								.label(128, "Constant Current")//
								.label(256, "Constant Voltage")//
								.label(512, "Boost MPPT"))),
				new ModbusRegisterRange(0xAA00, //
						new UnsignedWordElement(0xAA00,//
								// Prefix = "PvDCDC1 "
								new ModbusBitWrappingChannel("PvDCDC1SuggestiveInformation1", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDC1CurrentSamplingChannelAbnormityOnHighVoltageSide)//
								.warningBit(1, WarningCharger.PvDCDC1CurrentSamplingChannelAbnormityOnLowVoltageSide)//
								.warningBit(6, WarningCharger.PvDCDC1EEPROMParametersOverRange)//
								.warningBit(7, WarningCharger.PvDCDC1UpdateEEPROMFailed)//
								.warningBit(8, WarningCharger.PvDCDC1ReadEEPROMFailed)//
								.warningBit(9, WarningCharger.PvDCDC1CurrentSamplingChannelAbnormityBeforeInductance)//
								),//

						new UnsignedWordElement(0xAA01, //
								// Prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1SuggestiveInformation2", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDC1ReactorPowerDecreaseCausedByOvertemperature)//
								.warningBit(1, WarningCharger.PvDCDC1IGBTPowerDecreaseCausedByOvertemperature)//
								.warningBit(2, WarningCharger.PvDCDC1TemperatureChanel3PowerDecreaseCausedByOvertemperature)//
								.warningBit(3, WarningCharger.PvDCDC1TemperatureChanel4PowerDecreaseCausedByOvertemperature)//
								.warningBit(4, WarningCharger.PvDCDC1TemperatureChanel5PowerDecreaseCausedByOvertemperature)//
								.warningBit(5, WarningCharger.PvDCDC1TemperatureChanel6PowerDecreaseCausedByOvertemperature)//
								.warningBit(6, WarningCharger.PvDCDC1TemperatureChanel7PowerDecreaseCausedByOvertemperature)//
								.warningBit(7, WarningCharger.PvDCDC1TemperatureChanel8PowerDecreaseCausedByOvertemperature)//
								.warningBit(8, WarningCharger.PvDCDC1Fan1StopFailed)//
								.warningBit(9, WarningCharger.PvDCDC1Fan2StopFailed)//
								.warningBit(10,WarningCharger.PvDCDC1Fan3StopFailed)//
								.warningBit(11,WarningCharger.PvDCDC1Fan4StopFailed)//
								.warningBit(12,WarningCharger.PvDCDC1Fan1StartupFailed)//
								.warningBit(13,WarningCharger.PvDCDC1Fan2StartupFailed)//
								.warningBit(14,WarningCharger.PvDCDC1Fan3StartupFailed)//
								.warningBit(15,WarningCharger.PvDCDC1Fan4StartupFailed)//
								),//

						new UnsignedWordElement(0xAA02,//
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1SuggestiveInformation3", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDC1HighVoltageSideOvervoltage)//
								.warningBit(1, WarningCharger.PvDCDC1HighVoltageSideUndervoltage)//
								.warningBit(2, WarningCharger.PvDCDC1HighVoltageSideVoltageChangeUnconventionally)//
								),//

						new UnsignedWordElement(0xAA03,//
								//prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1SuggestiveInformation4", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDC1CurrentAbnormityBeforeDCConverterWorkOnHighVoltageSide)
								.warningBit(1, WarningCharger.PvDCDC1CurrentAbnormityBeforeDCConverterWorkOnLowVoltageSXide)
								.warningBit(2, WarningCharger.PvDCDC1InitialDutyRatioAbnormityBeforeDCConverterWork)
								.warningBit(3, WarningCharger.PvDCDC1VoltageAbnormityBeforeDCConverterWorkOnHighVoltageSide)
								.warningBit(4, WarningCharger.PvDCDC1VoltageAbnormityBeforeDCConverterWorkOnLowVoltageSide)
								),//

						new UnsignedWordElement(0xAA04,
								//prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1SuggestiveInformation5", this, this.thingState)//
								.warningBit(0, WarningCharger.PvDCDC1HighVoltageBreakerInspectionAbnormity)//
								.warningBit(1, WarningCharger.PvDCDC1LowVoltageBreakerInspectionAbnormity)//
								.warningBit(2, WarningCharger.PvDCDC1BsmDCDC5DCPrechargeContactorInspectionAbnormity)//
								.warningBit(3, WarningCharger.PvDCDC1DCPrechargeContactorOpenUnsuccessfully)//
								.warningBit(4, WarningCharger.PvDCDC1DCMainContactorInspectionAbnormity)//
								.warningBit(5, WarningCharger.PvDCDC1DCMainContactorOpenUnsuccessfully)//
								.warningBit(6, WarningCharger.PvDCDC1OutputContactorCloseUnsuccessfully)//
								.warningBit(7, WarningCharger.PvDCDC1OutputContactorOpenUnsuccessfully)//
								.warningBit(8, WarningCharger.PvDCDC1ACMainContactorCloseUnsuccessfully)//
								.warningBit(9, WarningCharger.PvDCDC1ACMainContactorOpenUnsuccessfully)//
								.warningBit(10,WarningCharger.PvDCDC1NegContactorOpenUnsuccessfully)//
								.warningBit(11,WarningCharger.PvDCDC1NegContactorCloseUnsuccessfully)//
								.warningBit(12,WarningCharger.PvDCDC1NegContactorStateAbnormal)//
								),//

						new DummyElement(0xAA05, 0xAA0F),
						new UnsignedWordElement(0xAA10,//
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity1", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDC1HighVoltageSideOfDCConverterUndervoltage)//
								.faultBit(1, FaultCharger.PvDCDC1HighVoltageSideOfDCConverterOvervoltage)//
								.faultBit(2, FaultCharger.PvDCDC1LowVoltageSideOfDCConverterUndervoltage)//
								.faultBit(3, FaultCharger.PvDCDC1LowVoltageSideOfDCConverterOvervoltage)//
								.faultBit(4, FaultCharger.PvDCDC1HighVoltageSideOfDCConverterOvercurrentFault)//
								.faultBit(5, FaultCharger.PvDCDC1LowVoltageSideOfDCConverterOvercurrentFault)//
								.faultBit(6, FaultCharger.PvDCDC1DCConverterIGBTFault)//
								.faultBit(7, FaultCharger.PvDCDC1DCConverterPrechargeUnmet)//
								),//

						new UnsignedWordElement(0xAA11,//
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity2", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDC1BECUCommunicationDisconnected)//
								.faultBit(1, FaultCharger.PvDCDC1DCConverterCommunicationDisconnected)//
								.faultBit(2, FaultCharger.PvDCDC1CurrentConfigurationOverRange)//
								.faultBit(3, FaultCharger.PvDCDC1TheBatteryRequestStop)//
								.faultBit(5, FaultCharger.PvDCDC1OvercurrentRelayFault)//
								.faultBit(6, FaultCharger.PvDCDC1LightningProtectionDeviceFault)//
								.faultBit(7, FaultCharger.PvDCDC1DCConverterPriamaryContactorDisconnectedAbnormally)//
								.faultBit(9, FaultCharger.PvDCDC1DCDisconnectedAbnormallyOnLowVoltageSideOfDCConvetor)//
								.faultBit(12,FaultCharger.PvDCDC1DCConvetorEEPROMAbnormity1)//
								.faultBit(13,FaultCharger.PvDCDC1DCConvetorEEPROMAbnormity1Second)//
								.faultBit(14,FaultCharger.PvDCDC1EDCConvetorEEPROMAbnormity1)//
								),//

						new UnsignedWordElement(0xAA12,
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity3", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDC1DCConvertorGeneralOverload)//
								.faultBit(1, FaultCharger.PvDCDC1DCShortCircuit)//
								.faultBit(2, FaultCharger.PvDCDC1PeakPulseCurrentProtection)//
								.faultBit(3, FaultCharger.PvDCDC1DCDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
								.faultBit(4, FaultCharger.PvDCDC1EffectivePulseValueOverhigh)//
								.faultBit(5, FaultCharger.PvDCDC1DCConverteSevereOverload)//
								.faultBit(6, FaultCharger.PvDCDC1DCBreakerDisconnectAbnormallyOnHighVoltageSideOfDCConvetor)//
								.faultBit(7, FaultCharger.PvDCDC1DCBreakerDisconnectAbnormallyOnLowVoltageSideOfDCConvetor)//
								.faultBit(8, FaultCharger.PvDCDC1DCConvetorPrechargeContactorCloseFailed)//
								.faultBit(9, FaultCharger.PvDCDC1DCConvetorMainContactorCloseFailed)//
								.faultBit(10,FaultCharger.PvDCDC1ACContactorStateAbnormityOfDCConvetor)//
								.faultBit(11,FaultCharger.PvDCDC1DCConvetorEmergencyStop)//
								.faultBit(12,FaultCharger.PvDCDC1DCConverterChargingGunDisconnected)//
								.faultBit(13,FaultCharger.PvDCDC1DCCurrentAbnormityBeforeDCConvetorWork)//
								.faultBit(14,FaultCharger.PvDCDC1FuSeDisconnected)//
								.faultBit(15,FaultCharger.PvDCDC1DCConverterHardwareCurrentOrVoltageFault)//
								),//
						new UnsignedWordElement(0xAA13,
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity4", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDC1DCConverterCrystalOscillatorCircuitInvalidation)//
								.faultBit(1, FaultCharger.PvDCDC1DCConverterResetCircuitInvalidation)//
								.faultBit(2, FaultCharger.PvDCDC1DCConverterSamplingCircuitInvalidation)//
								.faultBit(3, FaultCharger.PvDCDC1DCConverterDigitalIOCircuitInvalidation)//
								.faultBit(4, FaultCharger.PvDCDC1DCConverterPWMCircuitInvalidation)//
								.faultBit(5, FaultCharger.PvDCDC1DCConverterX5045CircuitInvalidation)//
								.faultBit(6, FaultCharger.PvDCDC1DCConverterCANCircuitInvalidation)//
								.faultBit(7, FaultCharger.PvDCDC1DCConverterSoftwareANDHardwareProtectionCircuitInvalidation)//
								.faultBit(8, FaultCharger.PvDCDC1DCConverterPowerCircuitInvalidation)//
								.faultBit(9, FaultCharger.PvDCDC1DCConverterCPUInvalidation)//
								.faultBit(10,FaultCharger.PvDCDC1DCConverterTINT0InterruptInvalidation)//
								.faultBit(11,FaultCharger.PvDCDC1DCConverterADCInterruptInvalidation)//
								.faultBit(12,FaultCharger.PvDCDC1DCConverterCAPITN4InterruptInvalidation)//
								.faultBit(13,FaultCharger.PvDCDC1DCConverterCAPINT6InterruptInvalidation)//
								.faultBit(14,FaultCharger.PvDCDC1DCConverterT3PINTinterruptInvalidation)//
								.faultBit(15,FaultCharger.PvDCDC1DCConverterT4PINTinterruptInvalidation)//
								),//
						new UnsignedWordElement(0xAA14,
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity5", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDC1DCConverterPDPINTAInterruptInvalidation)//
								.faultBit(1, FaultCharger.PvDCDC1DCConverterT1PINTInterruptInvalidationSecond)//
								.faultBit(2, FaultCharger.PvDCDC1DCConverterRESVInterruptInvalidation)//
								.faultBit(3, FaultCharger.PvDCDC1DCConverter100usTaskInvalidation)//
								.faultBit(4, FaultCharger.PvDCDC1DCConverterClockInvalidation)//
								.faultBit(5, FaultCharger.PvDCDC1DCConverterEMSMemoryInvalidation)//
								.faultBit(6, FaultCharger.PvDCDC1DCConverterExteriorCommunicationInvalidation)//
								.faultBit(7, FaultCharger.PvDCDC1DCConverterIOInterfaceInvalidation)//
								.faultBit(8, FaultCharger.PvDCDC1DCConverterInputVoltageBoundFault)//
								.faultBit(9, FaultCharger.PvDCDC1DCConverterOutterVoltageBoundFault)//
								.faultBit(10,FaultCharger.PvDCDC1DCConverterOutputVoltageBoundFault)//
								.faultBit(11,FaultCharger.PvDCDC1DCConverterInductCurrentBoundFault)//
								.faultBit(12,FaultCharger.PvDCDC1DCConverterInputCurrentBoundFault)//
								.faultBit(13,FaultCharger.PvDCDC1DCConverterOutputCurrentBoundFault)//
								),//
						new UnsignedWordElement(0xAA15,
								//prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity6", this, this.thingState)//
								.faultBit(0, FaultCharger.PvDCDC1DCReactorOverTemperature)//
								.faultBit(1, FaultCharger.PvDCDC1DCIGBTOverTemperature)//
								.faultBit(2, FaultCharger.PvDCDC1DCConverterChanel3OverTemperature)//
								.faultBit(3, FaultCharger.PvDCDC1DCConverterChanel4OverTemperature)//
								.faultBit(4, FaultCharger.PvDCDC1DCConverterChanel5OverTemperature)//
								.faultBit(5, FaultCharger.PvDCDC1DCConverterChanel6OverTemperature)//
								.faultBit(6, FaultCharger.PvDCDC1DCConverterChanel7OverTemperature)//
								.faultBit(7, FaultCharger.PvDCDC1DCConverterChanel8OverTemperature)//
								.faultBit(8, FaultCharger.PvDCDC1DCReactorTemperatureSamplingInvalidation)//
								.faultBit(9, FaultCharger.PvDCDC1DCIGBTTemperatureSamplingInvalidation)//
								.faultBit(10,FaultCharger.PvDCDC1DCConverterChanel3TemperatureSamplingInvalidation)//
								.faultBit(11,FaultCharger.PvDCDC1DCConverterChanel4TemperatureSamplingInvalidation)//
								.faultBit(12,FaultCharger.PvDCDC1DCConverterChanel5TemperatureSamplingInvalidation)//
								.faultBit(13,FaultCharger.PvDCDC1DCConverterChanel6TemperatureSamplingInvalidation)//
								.faultBit(14,FaultCharger.PvDCDC1DCConverterChanel7TemperatureSamplingInvalidation)//
								.faultBit(15,FaultCharger.PvDCDC1DCConverterChanel8TemperatureSamplingInvalidation)//
								),//
						new UnsignedWordElement(0xAA16,
								// prefix = "PvDCDC1"
								new ModbusBitWrappingChannel("PvDCDC1Abnormity7", this, this.thingState)//
								.faultBit(4, FaultCharger.PvDCDC1DCConverterInductanceCurrentSamplingInvalidation)//
								.faultBit(5, FaultCharger.PvDCDC1CurrentSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
								.faultBit(6, FaultCharger.PvDCDC1VoltageSamplingInvalidationOnTheLowVoltageSideOfDCConverter)//
								.faultBit(7, FaultCharger.PvDCDC1InsulationInspectionFault)//
								.faultBit(8, FaultCharger.PvDCDC1NegContactorCloseUnsuccessly)//
								.faultBit(9, FaultCharger.PvDCDC1NegContactorCutWhenRunning)//
								),//
						new DummyElement(0xAA17, 0xAA1F),
						new UnsignedWordElement(0xAA20,
								pvDCDC1SwitchState = new StatusBitChannel("PvDCDC1SwitchState", this)//
								.label(1, "DC precharge contactor")//
								.label(2, "DC main contactor")//
								.label(4, "Output contactor")//
								.label(8, "Output breaker")//
								.label(16, "Input breaker")//
								.label(32, "AC contactor")//
								.label(64, "Emergency stop button")//
								.label(128, "NegContactor"))),
				new ModbusRegisterRange(0xAA30, //
						new SignedWordElement(0xAA30,
								pvDCDC1OutputVoltage = new ModbusReadLongChannel("PvDCDC1OutputVoltage", this)
								.unit("mV").multiplier(2)),
						new SignedWordElement(0xAA31,
								pvDCDC1OutputCurrent = new ModbusReadLongChannel("PvDCDC1OutputCurrent", this)
								.unit("mA").multiplier(2)),
						new SignedWordElement(0xAA32,
								pvDCDC1OutputPower = new ModbusReadLongChannel("PvDCDC1OutputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xAA33,
								pvDCDC1InputVoltage = new ModbusReadLongChannel("PvDCDC1InputVoltage", this).unit("mV")
								.multiplier(2)),
						new SignedWordElement(0xAA34,
								pvDCDC1InputCurrent = new ModbusReadLongChannel("PvDCDC1InputCurrent", this).unit("mA")
								.multiplier(2)),
						new SignedWordElement(0xAA35,
								pvDCDC1InputPower = new ModbusReadLongChannel("PvDCDC1InputPower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0xAA36,
								pvDCDC1InputEnergy = new ModbusReadLongChannel("PvDCDC1InputEnergy", this).unit("Wh")
								.multiplier(2)),
						new SignedWordElement(0xAA37,
								pvDCDC1OutputEnergy = new ModbusReadLongChannel("PvDCDC1OutputEnergy", this).unit("Wh")
								.multiplier(2)),
						new DummyElement(0xAA38, 0xAA3F),
						new SignedWordElement(0xAA40,
								pvDCDC1ReactorTemperature = new ModbusReadLongChannel("PvDCDC1ReactorTemperature", this)
								.unit("°C")),
						new SignedWordElement(0xAA41,
								pvDCDC1IgbtTemperature = new ModbusReadLongChannel("PvDCDC1IgbtTemperature", this)
								.unit("°C")),
						new DummyElement(0xAA42, 0xAA4F),
						new UnsignedDoublewordElement(0xAA50,
								pvDCDC1InputTotalChargeEnergy = new ModbusReadLongChannel(
										"PvDCDC1InputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xAA52,
								pvDCDC1InputTotalDischargeEnergy = new ModbusReadLongChannel(
										"PvDCDC1InputTotalDischargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xAA54,
								pvDCDC1OutputTotalChargeEnergy = new ModbusReadLongChannel(
										"PvDCDC1OutputTotalChargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0xAA56,
								pvDCDC1OutputTotalDischargeEnergy = new ModbusReadLongChannel(
										"PvDCDC1OutputTotalDischargeEnergy", this).unit("Wh").multiplier(2))
						.wordOrder(WordOrder.LSWMSW)));
						actualPower = new FunctionalReadChannel<Long>("ActualPower", this, (channels) -> {
							long erg = 0;
							try {
								for (ReadChannel<Long> ch : channels) {
									erg += ch.value();
								}
								return erg;
							} catch (InvalidValueException e) {
								return null;
							}
						}, pvDCDCInputPower, pvDCDC1InputPower);
						inputVoltage = new FunctionalReadChannel<Long>("InputVoltage", this, (channels) -> {
							long erg = 0;
							try {
								for (ReadChannel<Long> ch : channels) {
									if (erg < ch.value()) {
										erg = ch.value();
									}
								}
								return erg;
							} catch (InvalidValueException e) {
								return null;
							}
						}, pvDCDCInputVoltage, pvDCDC1InputVoltage);
						return protocol;
	}

	@Override
	public WriteChannel<Long> setMaxPower() {
		return pvPowerLimitCommand;
	}

	@Override
	public ReadChannel<Long> getNominalPower() {
		return new StaticValueChannel<Long>("NominalPower", this, 60000l);
	}

	@Override
	public ReadChannel<Long> getActualPower() {
		return actualPower;
	}

	@Override
	public ReadChannel<Long> getInputVoltage() {
		return inputVoltage;
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return thingState;
	}

}
