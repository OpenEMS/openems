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
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRegisterRange;

@ThingInfo(title = "FENECON Commercial DC-Charger")
public class FeneconCommercialCharger extends ModbusDeviceNature implements ChargerNature {

	/*
	 * Constructors
	 */
	public FeneconCommercialCharger(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Inherited Channels
	 */

	public StatusBitChannels warning;

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
		warning = new StatusBitChannels("Warning", this);
		ModbusProtocol protocol = new ModbusProtocol(//
				new WriteableModbusRegisterRange(0x0503, new UnsignedWordElement(0x0503,
						pvPowerLimitCommand = new ModbusWriteLongChannel("PvPowerLimitCommand", this).multiplier(2)
								.unit("W"))),
				new ModbusRegisterRange(0xA000, //
						new UnsignedWordElement(0xA000,
								bmsDCDCWorkState = new ModbusReadLongChannel("BmsDCDCWorkState", this)//
										.label(2, "Initial")//
										.label(4, "Stop")//
										.label(8, "Ready")//
										.label(16, "Running")//
										.label(32, "Fault")//
										.label(64, "Debug")//
										.label(128, "Locked")),
						new UnsignedWordElement(0xA001,
								bmsDCDCWorkMode = new ModbusReadLongChannel("BmsDCDCWorkMode", this)//
										.label(128, "Constant Current")//
										.label(256, "Constant Voltage")//
										.label(512, "Boost MPPT"))),
				new ModbusRegisterRange(0xA100, //
						new UnsignedWordElement(0xA100,
								bmsDCDCSuggestiveInformation1 = warning
										.channel(new StatusBitChannel("BmsDCDCSuggestiveInformation1", this)//
												.label(1, "Current sampling channel abnormity on high voltage side")//
												.label(2, "Current sampling channel abnormity on low voltage side")//
												.label(64, "EEPROM parameters over range")//
												.label(128, "Update EEPROM failed")//
												.label(256, "Read EEPROM failed")//
												.label(512, "Current sampling channel abnormity before inductance"))),
						new UnsignedWordElement(0xA101, bmsDCDCSuggestiveInformation2 = warning
								.channel(new StatusBitChannel("BmsDCDCSuggestiveInformation2", this)//
										.label(1, "Reactor  power decrease caused by overtemperature")//
										.label(2, "IGBT  power decrease caused by overtemperature")//
										.label(4, "Temperature chanel3  power decrease caused by overtemperature")//
										.label(8, "Temperature chanel4  power decrease caused by overtemperature")//
										.label(16, "Temperature chanel5  power decrease caused by overtemperature")//
										.label(32, "Temperature chanel6  power decrease caused by overtemperature")//
										.label(64, "Temperature chanel7  power decrease caused by overtemperature")//
										.label(128, "Temperature chanel8 power decrease caused by overtemperature")//
										.label(256, "Fan 1 stop failed")//
										.label(512, "Fan 2 stop failed")//
										.label(1024, "Fan 3 stop failed")//
										.label(2048, "Fan 4 stop failed")//
										.label(4096, "Fan 1 sartup failed")//
										.label(8192, "Fan 2 sartup failed")//
										.label(16384, "Fan 3 sartup failed")//
										.label(32768, "Fan 4 sartup failed"))),
						new UnsignedWordElement(0xA102,
								bmsDCDCSuggestiveInformation3 = warning
										.channel(new StatusBitChannel("BmsDCDCSuggestiveInformation3", this)//
												.label(1, "High voltage side overvoltage")//
												.label(2, "High voltage side undervoltage")//
												.label(4, "EEPROM parameters over range")//
												.label(8, "High voltage side voltage change unconventionally"))),
						new UnsignedWordElement(0xA103, bmsDCDCSuggestiveInformation4 = warning
								.channel(new StatusBitChannel("BmsDCDCSuggestiveInformation4", this)//
										.label(1, "Current abnormity before DC Converter work on high voltage side")//
										.label(2, "Current abnormity before DC Converter work on low voltage side")//
										.label(4, "Initial Duty Ratio abnormity before DC Converter work")//
										.label(8, "Voltage abnormity before DC Converter work on high voltage side")//
										.label(16, "Voltage abnormity before  DC Converter work on low voltage side"))),
						new UnsignedWordElement(0xA104,
								bmsDCDCSuggestiveInformation5 = warning
										.channel(new StatusBitChannel("BmsDCDCSuggestiveInformation5", this)//
												.label(1, "High voltage breaker inspection abnormity")//
												.label(2, "Low voltage breaker inspection abnormity")//
												.label(4, "DC precharge contactor inspection abnormity")//
												.label(8, "DC precharge contactor open unsuccessfully")//
												.label(16, "DC main contactor inspection abnormity")//
												.label(32, "DC main contactor open unsuccessfully")//
												.label(64, "Output contactor close unsuccessfully")//
												.label(128, "Output contactor open unsuccessfully")//
												.label(256, "AC main contactor close unsuccessfully")//
												.label(512, "AC main contactor open unsuccessfully")//
												.label(1024, "NegContactor open unsuccessfully")//
												.label(2048, "NegContactor close unsuccessfully")//
												.label(4096, "NegContactor state abnormal"))),
						new DummyElement(0xA105, 0xA10F),
						new UnsignedWordElement(0xA110,
								bmsDCDCAbnormity1 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity1", this)//
										.label(1, "High voltage side of DC Converter undervoltage")//
										.label(2, "High voltage side of DC Converter overvoltage")//
										.label(4, "Low voltage side  of DC Converter undervoltage")//
										.label(8, "Low voltage side  of DC Converter overvoltage")//
										.label(16, "High voltage side of DC Converter overcurrent fault")//
										.label(32, "Low voltage side of DC Converter overcurrent fault")//
										.label(64, "DC Converter IGBT fault")//
										.label(128, "DC Converter Precharge unmet"))),
						new UnsignedWordElement(0xA111,
								bmsDCDCAbnormity2 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity2", this)//
										.label(1, "BECU communication disconnected")//
										.label(2, "DC Converter communication disconnected")//
										.label(4, "Current configuration over range")//
										.label(8, "The battery request stop")//
										.label(32, "Overcurrent relay fault")//
										.label(64, "Lightning protection device fault")//
										.label(128, "DC Converter priamary contactor disconnected abnormally")//
										.label(512, "DC disconnected abnormally on low voltage side of DC convetor")//
										.label(4096, "DC convetor EEPROM abnormity 1")//
										.label(8192, "DC convetor EEPROM abnormity 1")//
										.label(16384, "EDC convetor EEPROM abnormity 1"))),
						new UnsignedWordElement(0xA112,
								bmsDCDCAbnormity3 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity3", this)//
										.label(1, "DC Convertor general overload")//
										.label(2, "DC short circuit")//
										.label(4, "Peak pulse current protection")//
										.label(8, "DC disconnect abnormally on high voltage side of DC convetor")//
										.label(16, "Effective pulse value overhigh")//
										.label(32, "DC Converte severe overload")//
										.label(64,
												"DC breaker disconnect abnormally on high voltage side of DC convetor")//
										.label(128,
												"DC breaker disconnect abnormally on low voltage side of DC convetor")//
										.label(256, "DC convetor precharge contactor close failed ")//
										.label(512, "DC convetor main contactor close failed")//
										.label(1024, "AC contactor state abnormity of DC convetor")//
										.label(2048, "DC convetor emergency stop")//
										.label(4096, "DC converter charging gun disconnected")//
										.label(8192, "DC current abnormity before DC convetor work")//
										.label(16384, "Fuse disconnected")//
										.label(32768, "DC converter hardware current or voltage fault"))),
						new UnsignedWordElement(0xA113,
								bmsDCDCAbnormity4 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity4", this)//
										.label(1, "DC converter crystal oscillator circuit invalidation")//
										.label(2, "DC converter reset circuit invalidation")//
										.label(4, "DC converter sampling circuit invalidation")//
										.label(8, "DC converter digital I/O circuit invalidation")//
										.label(16, "DC converter PWM circuit invalidation")//
										.label(32, "DC converter X5045 circuit invalidation")//
										.label(64, "DC converter CAN circuit invalidation")//
										.label(128, "DC converter software&hardware protection circuit invalidation")//
										.label(256, "DC converter power circuit invalidation")//
										.label(512, "DC converter CPU invalidation")//
										.label(1024, "DC converter TINT0 interrupt invalidation")//
										.label(2048, "DC converter ADC interrupt invalidation")//
										.label(4096, "DC converter CAPITN4 interrupt invalidation")//
										.label(8192, "DC converter CAPINT6 interrupt invalidation")//
										.label(16384, "DC converter T3PINTinterrupt invalidation")//
										.label(32768, "DC converter T4PINTinterrupt invalidation"))),
						new UnsignedWordElement(0xA114,
								bmsDCDCAbnormity5 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity5", this)//
										.label(1, "DC converter PDPINTA interrupt invalidation")//
										.label(2, "DC converter T1PINT interrupt invalidation")//
										.label(4, "DC converter RESV interrupt invalidation")//
										.label(8, "DC converter 100us task invalidation")//
										.label(16, "DC converter clock  invalidation")//
										.label(32, "DC converter EMS memory invalidation")//
										.label(64, "DC converter exterior communication invalidation")//
										.label(128, "DC converter IO Interface invalidation")//
										.label(256, "DC converter Input Voltage bound fault")//
										.label(512, "DC converter Outter Voltage bound fault")//
										.label(1024, "DC converter Output Voltage bound fault")//
										.label(2048, "DC converter Induct Current bound fault")//
										.label(4096, "DC converter Input Current bound fault")//
										.label(8192, "DC converter Output Current bound fault"))),
						new UnsignedWordElement(0xA115,
								bmsDCDCAbnormity6 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity6", this)//
										.label(1, "DC Reactor over temperature")//
										.label(2, "DC IGBT over temperature")//
										.label(4, "DC Converter chanel 3 over temperature")//
										.label(8, "DC Converter chanel 4 over temperature")//
										.label(16, "DC Converter chanel 5 over temperature")//
										.label(32, "DC Converter chanel 6 over temperature")//
										.label(64, "DC Converter chanel 7 over temperature")//
										.label(128, "DC Converter chanel 8 over temperature")//
										.label(256, "DC Reactor temperature sampling invalidation")//
										.label(512, "DC IGBT temperature sampling invalidation")//
										.label(1024, "DC Converter chanel 3 temperature sampling invalidation")//
										.label(2048, "DC Converter chanel 4 temperature sampling invalidation")//
										.label(4096, "DC Converter chanel 5 temperature sampling invalidation")//
										.label(8192, "DC Converter chanel 6 temperature sampling invalidation")//
										.label(16384, "DC Converter chanel 7 temperature sampling invalidation")//
										.label(32768, "DC Converter chanel 8 temperature sampling invalidation"))),
						new UnsignedWordElement(0xA116,
								bmsDCDCAbnormity7 = warning.channel(new StatusBitChannel("BmsDCDCAbnormity7", this)//
										.label(32, "DC Converter inductance current sampling invalidation")//
										.label(64,
												"Current sampling invalidation on the low voltage sideof DC Converter")//
										.label(128,
												"Voltage sampling invalidation on the low voltage side of DC Converter")//
										.label(256, "Insulation inspection fault")//
										.label(512, "NegContactor close unsuccessly")//
										.label(1024, "NegContactor cut When running"))),
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
						new UnsignedWordElement(0xA400,
								bmsDCDC1SuggestiveInformation1 = warning
										.channel(new StatusBitChannel("BmsDCDC1SuggestiveInformation1", this)//
												.label(1, "Current sampling channel abnormity on high voltage side")//
												.label(2, "Current sampling channel abnormity on low voltage side")//
												.label(64, "EEPROM parameters over range")//
												.label(128, "Update EEPROM failed")//
												.label(256, "Read EEPROM failed")//
												.label(512, "Current sampling channel abnormity before inductance"))),
						new UnsignedWordElement(0xA401, bmsDCDC1SuggestiveInformation2 = warning
								.channel(new StatusBitChannel("BmsDCDC1SuggestiveInformation2", this)//
										.label(1, "Reactor  power decrease caused by overtemperature")//
										.label(2, "IGBT  power decrease caused by overtemperature")//
										.label(4, "Temperature chanel3  power decrease caused by overtemperature")//
										.label(8, "Temperature chanel4  power decrease caused by overtemperature")//
										.label(16, "Temperature chanel5  power decrease caused by overtemperature")//
										.label(32, "Temperature chanel6  power decrease caused by overtemperature")//
										.label(64, "Temperature chanel7  power decrease caused by overtemperature")//
										.label(128, "Temperature chanel8 power decrease caused by overtemperature")//
										.label(256, "Fan 1 stop failed")//
										.label(512, "Fan 2 stop failed")//
										.label(1024, "Fan 3 stop failed")//
										.label(2048, "Fan 4 stop failed")//
										.label(4096, "Fan 1 sartup failed")//
										.label(8192, "Fan 2 sartup failed")//
										.label(16384, "Fan 3 sartup failed")//
										.label(32768, "Fan 4 sartup failed"))),
						new UnsignedWordElement(0xA402,
								bmsDCDC1SuggestiveInformation3 = warning
										.channel(new StatusBitChannel("BmsDCDC1SuggestiveInformation3", this)//
												.label(1, "High voltage side overvoltage")//
												.label(2, "High voltage side undervoltage")//
												.label(4, "EEPROM parameters over range")//
												.label(8, "High voltage side voltage change unconventionally"))),
						new UnsignedWordElement(0xA403, bmsDCDC1SuggestiveInformation4 = warning
								.channel(new StatusBitChannel("BmsDCDC1SuggestiveInformation4", this)//
										.label(1, "Current abnormity before DC Converter work on high voltage side")//
										.label(2, "Current abnormity before DC Converter work on low voltage side")//
										.label(4, "Initial Duty Ratio abnormity before DC Converter work")//
										.label(8, "Voltage abnormity before DC Converter work on high voltage side")//
										.label(16, "Voltage abnormity before  DC Converter work on low voltage side"))),
						new UnsignedWordElement(0xA404,
								bmsDCDC1SuggestiveInformation5 = warning
										.channel(new StatusBitChannel("BmsDCDC1SuggestiveInformation5", this)//
												.label(1, "High voltage breaker inspection abnormity")//
												.label(2, "Low voltage breaker inspection abnormity")//
												.label(4, "DC precharge contactor inspection abnormity")//
												.label(8, "DC precharge contactor open unsuccessfully")//
												.label(16, "DC main contactor inspection abnormity")//
												.label(32, "DC main contactor open unsuccessfully")//
												.label(64, "Output contactor close unsuccessfully")//
												.label(128, "Output contactor open unsuccessfully")//
												.label(256, "AC main contactor close unsuccessfully")//
												.label(512, "AC main contactor open unsuccessfully")//
												.label(1024, "NegContactor open unsuccessfully")//
												.label(2048, "NegContactor close unsuccessfully")//
												.label(4096, "NegContactor state abnormal"))),
						new DummyElement(0xA405, 0xA40F),
						new UnsignedWordElement(0xA410,
								bmsDCDC1Abnormity1 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity1", this)//
										.label(1, "High voltage side of DC Converter undervoltage")//
										.label(2, "High voltage side of DC Converter overvoltage")//
										.label(4, "Low voltage side  of DC Converter undervoltage")//
										.label(8, "Low voltage side  of DC Converter overvoltage")//
										.label(16, "High voltage side of DC Converter overcurrent fault")//
										.label(32, "Low voltage side of DC Converter overcurrent fault")//
										.label(64, "DC Converter IGBT fault")//
										.label(128, "DC Converter Precharge unmet"))),
						new UnsignedWordElement(0xA411,
								bmsDCDC1Abnormity2 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity2", this)//
										.label(1, "BECU communication disconnected")//
										.label(2, "DC Converter communication disconnected")//
										.label(4, "Current configuration over range")//
										.label(8, "The battery request stop")//
										.label(32, "Overcurrent relay fault")//
										.label(64, "Lightning protection device fault")//
										.label(128, "DC Converter priamary contactor disconnected abnormally")//
										.label(512, "DC disconnected abnormally on low voltage side of DC convetor")//
										.label(4096, "DC convetor EEPROM abnormity 1")//
										.label(8192, "DC convetor EEPROM abnormity 1")//
										.label(16384, "EDC convetor EEPROM abnormity 1"))),
						new UnsignedWordElement(0xA412,
								bmsDCDC1Abnormity3 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity3", this)//
										.label(1, "DC Convertor general overload")//
										.label(2, "DC short circuit")//
										.label(4, "Peak pulse current protection")//
										.label(8, "DC disconnect abnormally on high voltage side of DC convetor")//
										.label(16, "Effective pulse value overhigh")//
										.label(32, "DC Converte severe overload")//
										.label(64,
												"DC breaker disconnect abnormally on high voltage side of DC convetor")//
										.label(128,
												"DC breaker disconnect abnormally on low voltage side of DC convetor")//
										.label(256, "DC convetor precharge contactor close failed ")//
										.label(512, "DC convetor main contactor close failed")//
										.label(1024, "AC contactor state abnormity of DC convetor")//
										.label(2048, "DC convetor emergency stop")//
										.label(4096, "DC converter charging gun disconnected")//
										.label(8192, "DC current abnormity before DC convetor work")//
										.label(16384, "Fuse disconnected")//
										.label(32768, "DC converter hardware current or voltage fault"))),
						new UnsignedWordElement(0xA413,
								bmsDCDC1Abnormity4 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity4", this)//
										.label(1, "DC converter crystal oscillator circuit invalidation")//
										.label(2, "DC converter reset circuit invalidation")//
										.label(4, "DC converter sampling circuit invalidation")//
										.label(8, "DC converter digital I/O circuit invalidation")//
										.label(16, "DC converter PWM circuit invalidation")//
										.label(32, "DC converter X5045 circuit invalidation")//
										.label(64, "DC converter CAN circuit invalidation")//
										.label(128, "DC converter software&hardware protection circuit invalidation")//
										.label(256, "DC converter power circuit invalidation")//
										.label(512, "DC converter CPU invalidation")//
										.label(1024, "DC converter TINT0 interrupt invalidation")//
										.label(2048, "DC converter ADC interrupt invalidation")//
										.label(4096, "DC converter CAPITN4 interrupt invalidation")//
										.label(8192, "DC converter CAPINT6 interrupt invalidation")//
										.label(16384, "DC converter T3PINTinterrupt invalidation")//
										.label(32768, "DC converter T4PINTinterrupt invalidation"))),
						new UnsignedWordElement(0xA414,
								bmsDCDC1Abnormity5 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity5", this)//
										.label(1, "DC converter PDPINTA interrupt invalidation")//
										.label(2, "DC converter T1PINT interrupt invalidation")//
										.label(4, "DC converter RESV interrupt invalidation")//
										.label(8, "DC converter 100us task invalidation")//
										.label(16, "DC converter clock  invalidation")//
										.label(32, "DC converter EMS memory invalidation")//
										.label(64, "DC converter exterior communication invalidation")//
										.label(128, "DC converter IO Interface invalidation")//
										.label(256, "DC converter Input Voltage bound fault")//
										.label(512, "DC converter Outter Voltage bound fault")//
										.label(1024, "DC converter Output Voltage bound fault")//
										.label(2048, "DC converter Induct Current bound fault")//
										.label(4096, "DC converter Input Current bound fault")//
										.label(8192, "DC converter Output Current bound fault"))),
						new UnsignedWordElement(0xA415,
								bmsDCDC1Abnormity6 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity6", this)//
										.label(1, "DC Reactor over temperature")//
										.label(2, "DC IGBT over temperature")//
										.label(4, "DC Converter chanel 3 over temperature")//
										.label(8, "DC Converter chanel 4 over temperature")//
										.label(16, "DC Converter chanel 5 over temperature")//
										.label(32, "DC Converter chanel 6 over temperature")//
										.label(64, "DC Converter chanel 7 over temperature")//
										.label(128, "DC Converter chanel 8 over temperature")//
										.label(256, "DC Reactor temperature sampling invalidation")//
										.label(512, "DC IGBT temperature sampling invalidation")//
										.label(1024, "DC Converter chanel 3 temperature sampling invalidation")//
										.label(2048, "DC Converter chanel 4 temperature sampling invalidation")//
										.label(4096, "DC Converter chanel 5 temperature sampling invalidation")//
										.label(8192, "DC Converter chanel 6 temperature sampling invalidation")//
										.label(16384, "DC Converter chanel 7 temperature sampling invalidation")//
										.label(32768, "DC Converter chanel 8 temperature sampling invalidation"))),
						new UnsignedWordElement(0xA416,
								bmsDCDC1Abnormity7 = warning.channel(new StatusBitChannel("BmsDCDC1Abnormity7", this)//
										.label(32, "DC Converter inductance current sampling invalidation")//
										.label(64,
												"Current sampling invalidation on the low voltage sideof DC Converter")//
										.label(128,
												"Voltage sampling invalidation on the low voltage side of DC Converter")//
										.label(256, "Insulation inspection fault")//
										.label(512, "NegContactor close unsuccessly")//
										.label(1024, "NegContactor cut When running"))),
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
						new UnsignedWordElement(0xA700,
								pvDCDCSuggestiveInformation1 = warning
										.channel(new StatusBitChannel("PvDCDCSuggestiveInformation1", this)//
												.label(1, "Current sampling channel abnormity on high voltage side")//
												.label(2, "Current sampling channel abnormity on low voltage side")//
												.label(64, "EEPROM parameters over range")//
												.label(128, "Update EEPROM failed")//
												.label(256, "Read EEPROM failed")//
												.label(512, "Current sampling channel abnormity before inductance"))),
						new UnsignedWordElement(0xA701, pvDCDCSuggestiveInformation2 = warning
								.channel(new StatusBitChannel("PvDCDCSuggestiveInformation2", this)//
										.label(1, "Reactor  power decrease caused by overtemperature")//
										.label(2, "IGBT  power decrease caused by overtemperature")//
										.label(4, "Temperature chanel3  power decrease caused by overtemperature")//
										.label(8, "Temperature chanel4  power decrease caused by overtemperature")//
										.label(16, "Temperature chanel5  power decrease caused by overtemperature")//
										.label(32, "Temperature chanel6  power decrease caused by overtemperature")//
										.label(64, "Temperature chanel7  power decrease caused by overtemperature")//
										.label(128, "Temperature chanel8 power decrease caused by overtemperature")//
										.label(256, "Fan 1 stop failed")//
										.label(512, "Fan 2 stop failed")//
										.label(1024, "Fan 3 stop failed")//
										.label(2048, "Fan 4 stop failed")//
										.label(4096, "Fan 1 sartup failed")//
										.label(8192, "Fan 2 sartup failed")//
										.label(16384, "Fan 3 sartup failed")//
										.label(32768, "Fan 4 sartup failed"))),
						new UnsignedWordElement(0xA702,
								pvDCDCSuggestiveInformation3 = warning
										.channel(new StatusBitChannel("PvDCDCSuggestiveInformation3", this)//
												.label(1, "High voltage side overvoltage")//
												.label(2, "High voltage side undervoltage")//
												.label(4, "EEPROM parameters over range")//
												.label(8, "High voltage side voltage change unconventionally"))),
						new UnsignedWordElement(0xA703, pvDCDCSuggestiveInformation4 = warning
								.channel(new StatusBitChannel("PvDCDCSuggestiveInformation4", this)//
										.label(1, "Current abnormity before DC Converter work on high voltage side")//
										.label(2, "Current abnormity before DC Converter work on low voltage side")//
										.label(4, "Initial Duty Ratio abnormity before DC Converter work")//
										.label(8, "Voltage abnormity before DC Converter work on high voltage side")//
										.label(16, "Voltage abnormity before  DC Converter work on low voltage side"))),
						new UnsignedWordElement(0xA704,
								pvDCDCSuggestiveInformation5 = warning
										.channel(new StatusBitChannel("PvDCDCSuggestiveInformation5", this)//
												.label(1, "High voltage breaker inspection abnormity")//
												.label(2, "Low voltage breaker inspection abnormity")//
												.label(4, "DC precharge contactor inspection abnormity")//
												.label(8, "DC precharge contactor open unsuccessfully")//
												.label(16, "DC main contactor inspection abnormity")//
												.label(32, "DC main contactor open unsuccessfully")//
												.label(64, "Output contactor close unsuccessfully")//
												.label(128, "Output contactor open unsuccessfully")//
												.label(256, "AC main contactor close unsuccessfully")//
												.label(512, "AC main contactor open unsuccessfully")//
												.label(1024, "NegContactor open unsuccessfully")//
												.label(2048, "NegContactor close unsuccessfully")//
												.label(4096, "NegContactor state abnormal"))),
						new DummyElement(0xA705, 0xA70F),
						new UnsignedWordElement(0xA710,
								pvDCDCAbnormity1 = warning.channel(new StatusBitChannel("PvDCDCAbnormity1", this)//
										.label(1, "High voltage side of DC Converter undervoltage")//
										.label(2, "High voltage side of DC Converter overvoltage")//
										.label(4, "Low voltage side  of DC Converter undervoltage")//
										.label(8, "Low voltage side  of DC Converter overvoltage")//
										.label(16, "High voltage side of DC Converter overcurrent fault")//
										.label(32, "Low voltage side of DC Converter overcurrent fault")//
										.label(64, "DC Converter IGBT fault")//
										.label(128, "DC Converter Precharge unmet"))),
						new UnsignedWordElement(0xA711,
								pvDCDCAbnormity2 = warning.channel(new StatusBitChannel("PvDCDCAbnormity2", this)//
										.label(1, "BECU communication disconnected")//
										.label(2, "DC Converter communication disconnected")//
										.label(4, "Current configuration over range")//
										.label(8, "The battery request stop")//
										.label(32, "Overcurrent relay fault")//
										.label(64, "Lightning protection device fault")//
										.label(128, "DC Converter priamary contactor disconnected abnormally")//
										.label(512, "DC disconnected abnormally on low voltage side of DC convetor")//
										.label(4096, "DC convetor EEPROM abnormity 1")//
										.label(8192, "DC convetor EEPROM abnormity 1")//
										.label(16384, "EDC convetor EEPROM abnormity 1"))),
						new UnsignedWordElement(0xA712,
								pvDCDCAbnormity3 = warning.channel(new StatusBitChannel("PvDCDCAbnormity3", this)//
										.label(1, "DC Convertor general overload")//
										.label(2, "DC short circuit")//
										.label(4, "Peak pulse current protection")//
										.label(8, "DC disconnect abnormally on high voltage side of DC convetor")//
										.label(16, "Effective pulse value overhigh")//
										.label(32, "DC Converte severe overload")//
										.label(64,
												"DC breaker disconnect abnormally on high voltage side of DC convetor")//
										.label(128,
												"DC breaker disconnect abnormally on low voltage side of DC convetor")//
										.label(256, "DC convetor precharge contactor close failed ")//
										.label(512, "DC convetor main contactor close failed")//
										.label(1024, "AC contactor state abnormity of DC convetor")//
										.label(2048, "DC convetor emergency stop")//
										.label(4096, "DC converter charging gun disconnected")//
										.label(8192, "DC current abnormity before DC convetor work")//
										.label(16384, "Fuse disconnected")//
										.label(32768, "DC converter hardware current or voltage fault"))),
						new UnsignedWordElement(0xA713,
								pvDCDCAbnormity4 = warning.channel(new StatusBitChannel("PvDCDCAbnormity4", this)//
										.label(1, "DC converter crystal oscillator circuit invalidation")//
										.label(2, "DC converter reset circuit invalidation")//
										.label(4, "DC converter sampling circuit invalidation")//
										.label(8, "DC converter digital I/O circuit invalidation")//
										.label(16, "DC converter PWM circuit invalidation")//
										.label(32, "DC converter X5045 circuit invalidation")//
										.label(64, "DC converter CAN circuit invalidation")//
										.label(128, "DC converter software&hardware protection circuit invalidation")//
										.label(256, "DC converter power circuit invalidation")//
										.label(512, "DC converter CPU invalidation")//
										.label(1024, "DC converter TINT0 interrupt invalidation")//
										.label(2048, "DC converter ADC interrupt invalidation")//
										.label(4096, "DC converter CAPITN4 interrupt invalidation")//
										.label(8192, "DC converter CAPINT6 interrupt invalidation")//
										.label(16384, "DC converter T3PINTinterrupt invalidation")//
										.label(32768, "DC converter T4PINTinterrupt invalidation"))),
						new UnsignedWordElement(0xA714,
								pvDCDCAbnormity5 = warning.channel(new StatusBitChannel("PvDCDCAbnormity5", this)//
										.label(1, "DC converter PDPINTA interrupt invalidation")//
										.label(2, "DC converter T1PINT interrupt invalidation")//
										.label(4, "DC converter RESV interrupt invalidation")//
										.label(8, "DC converter 100us task invalidation")//
										.label(16, "DC converter clock  invalidation")//
										.label(32, "DC converter EMS memory invalidation")//
										.label(64, "DC converter exterior communication invalidation")//
										.label(128, "DC converter IO Interface invalidation")//
										.label(256, "DC converter Input Voltage bound fault")//
										.label(512, "DC converter Outter Voltage bound fault")//
										.label(1024, "DC converter Output Voltage bound fault")//
										.label(2048, "DC converter Induct Current bound fault")//
										.label(4096, "DC converter Input Current bound fault")//
										.label(8192, "DC converter Output Current bound fault"))),
						new UnsignedWordElement(0xA715,
								pvDCDCAbnormity6 = warning.channel(new StatusBitChannel("PvDCDCAbnormity6", this)//
										.label(1, "DC Reactor over temperature")//
										.label(2, "DC IGBT over temperature")//
										.label(4, "DC Converter chanel 3 over temperature")//
										.label(8, "DC Converter chanel 4 over temperature")//
										.label(16, "DC Converter chanel 5 over temperature")//
										.label(32, "DC Converter chanel 6 over temperature")//
										.label(64, "DC Converter chanel 7 over temperature")//
										.label(128, "DC Converter chanel 8 over temperature")//
										.label(256, "DC Reactor temperature sampling invalidation")//
										.label(512, "DC IGBT temperature sampling invalidation")//
										.label(1024, "DC Converter chanel 3 temperature sampling invalidation")//
										.label(2048, "DC Converter chanel 4 temperature sampling invalidation")//
										.label(4096, "DC Converter chanel 5 temperature sampling invalidation")//
										.label(8192, "DC Converter chanel 6 temperature sampling invalidation")//
										.label(16384, "DC Converter chanel 7 temperature sampling invalidation")//
										.label(32768, "DC Converter chanel 8 temperature sampling invalidation"))),
						new UnsignedWordElement(0xA716,
								pvDCDCAbnormity7 = warning.channel(new StatusBitChannel("PvDCDCAbnormity7", this)//
										.label(32, "DC Converter inductance current sampling invalidation")//
										.label(64,
												"Current sampling invalidation on the low voltage sideof DC Converter")//
										.label(128,
												"Voltage sampling invalidation on the low voltage side of DC Converter")//
										.label(256, "Insulation inspection fault")//
										.label(512, "NegContactor close unsuccessly")//
										.label(1024, "NegContactor cut When running"))),
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
						new UnsignedWordElement(0xAA00,
								pvDCDC1SuggestiveInformation1 = warning
										.channel(new StatusBitChannel("PvDCDC1SuggestiveInformation1", this)//
												.label(1, "Current sampling channel abnormity on high voltage side")//
												.label(2, "Current sampling channel abnormity on low voltage side")//
												.label(64, "EEPROM parameters over range")//
												.label(128, "Update EEPROM failed")//
												.label(256, "Read EEPROM failed")//
												.label(512, "Current sampling channel abnormity before inductance"))),
						new UnsignedWordElement(0xAA01, pvDCDC1SuggestiveInformation2 = warning
								.channel(new StatusBitChannel("PvDCDC1SuggestiveInformation2", this)//
										.label(1, "Reactor  power decrease caused by overtemperature")//
										.label(2, "IGBT  power decrease caused by overtemperature")//
										.label(4, "Temperature chanel3  power decrease caused by overtemperature")//
										.label(8, "Temperature chanel4  power decrease caused by overtemperature")//
										.label(16, "Temperature chanel5  power decrease caused by overtemperature")//
										.label(32, "Temperature chanel6  power decrease caused by overtemperature")//
										.label(64, "Temperature chanel7  power decrease caused by overtemperature")//
										.label(128, "Temperature chanel8 power decrease caused by overtemperature")//
										.label(256, "Fan 1 stop failed")//
										.label(512, "Fan 2 stop failed")//
										.label(1024, "Fan 3 stop failed")//
										.label(2048, "Fan 4 stop failed")//
										.label(4096, "Fan 1 sartup failed")//
										.label(8192, "Fan 2 sartup failed")//
										.label(16384, "Fan 3 sartup failed")//
										.label(32768, "Fan 4 sartup failed"))),
						new UnsignedWordElement(0xAA02,
								pvDCDC1SuggestiveInformation3 = warning
										.channel(new StatusBitChannel("PvDCDC1SuggestiveInformation3", this)//
												.label(1, "High voltage side overvoltage")//
												.label(2, "High voltage side undervoltage")//
												.label(4, "EEPROM parameters over range")//
												.label(8, "High voltage side voltage change unconventionally"))),
						new UnsignedWordElement(0xAA03, pvDCDC1SuggestiveInformation4 = warning
								.channel(new StatusBitChannel("PvDCDC1SuggestiveInformation4", this)//
										.label(1, "Current abnormity before DC Converter work on high voltage side")//
										.label(2, "Current abnormity before DC Converter work on low voltage side")//
										.label(4, "Initial Duty Ratio abnormity before DC Converter work")//
										.label(8, "Voltage abnormity before DC Converter work on high voltage side")//
										.label(16, "Voltage abnormity before  DC Converter work on low voltage side"))),
						new UnsignedWordElement(0xAA04,
								pvDCDC1SuggestiveInformation5 = warning
										.channel(new StatusBitChannel("PvDCDC1SuggestiveInformation5", this)//
												.label(1, "High voltage breaker inspection abnormity")//
												.label(2, "Low voltage breaker inspection abnormity")//
												.label(4, "DC precharge contactor inspection abnormity")//
												.label(8, "DC precharge contactor open unsuccessfully")//
												.label(16, "DC main contactor inspection abnormity")//
												.label(32, "DC main contactor open unsuccessfully")//
												.label(64, "Output contactor close unsuccessfully")//
												.label(128, "Output contactor open unsuccessfully")//
												.label(256, "AC main contactor close unsuccessfully")//
												.label(512, "AC main contactor open unsuccessfully")//
												.label(1024, "NegContactor open unsuccessfully")//
												.label(2048, "NegContactor close unsuccessfully")//
												.label(4096, "NegContactor state abnormal"))),
						new DummyElement(0xAA05, 0xAA0F),
						new UnsignedWordElement(0xAA10,
								pvDCDC1Abnormity1 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity1", this)//
										.label(1, "High voltage side of DC Converter undervoltage")//
										.label(2, "High voltage side of DC Converter overvoltage")//
										.label(4, "Low voltage side  of DC Converter undervoltage")//
										.label(8, "Low voltage side  of DC Converter overvoltage")//
										.label(16, "High voltage side of DC Converter overcurrent fault")//
										.label(32, "Low voltage side of DC Converter overcurrent fault")//
										.label(64, "DC Converter IGBT fault")//
										.label(128, "DC Converter Precharge unmet"))),
						new UnsignedWordElement(0xAA11,
								pvDCDC1Abnormity2 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity2", this)//
										.label(1, "BECU communication disconnected")//
										.label(2, "DC Converter communication disconnected")//
										.label(4, "Current configuration over range")//
										.label(8, "The battery request stop")//
										.label(32, "Overcurrent relay fault")//
										.label(64, "Lightning protection device fault")//
										.label(128, "DC Converter priamary contactor disconnected abnormally")//
										.label(512, "DC disconnected abnormally on low voltage side of DC convetor")//
										.label(4096, "DC convetor EEPROM abnormity 1")//
										.label(8192, "DC convetor EEPROM abnormity 1")//
										.label(16384, "EDC convetor EEPROM abnormity 1"))),
						new UnsignedWordElement(0xAA12,
								pvDCDC1Abnormity3 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity3", this)//
										.label(1, "DC Convertor general overload")//
										.label(2, "DC short circuit")//
										.label(4, "Peak pulse current protection")//
										.label(8, "DC disconnect abnormally on high voltage side of DC convetor")//
										.label(16, "Effective pulse value overhigh")//
										.label(32, "DC Converte severe overload")//
										.label(64,
												"DC breaker disconnect abnormally on high voltage side of DC convetor")//
										.label(128,
												"DC breaker disconnect abnormally on low voltage side of DC convetor")//
										.label(256, "DC convetor precharge contactor close failed ")//
										.label(512, "DC convetor main contactor close failed")//
										.label(1024, "AC contactor state abnormity of DC convetor")//
										.label(2048, "DC convetor emergency stop")//
										.label(4096, "DC converter charging gun disconnected")//
										.label(8192, "DC current abnormity before DC convetor work")//
										.label(16384, "Fuse disconnected")//
										.label(32768, "DC converter hardware current or voltage fault"))),
						new UnsignedWordElement(0xAA13,
								pvDCDC1Abnormity4 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity4", this)//
										.label(1, "DC converter crystal oscillator circuit invalidation")//
										.label(2, "DC converter reset circuit invalidation")//
										.label(4, "DC converter sampling circuit invalidation")//
										.label(8, "DC converter digital I/O circuit invalidation")//
										.label(16, "DC converter PWM circuit invalidation")//
										.label(32, "DC converter X5045 circuit invalidation")//
										.label(64, "DC converter CAN circuit invalidation")//
										.label(128, "DC converter software&hardware protection circuit invalidation")//
										.label(256, "DC converter power circuit invalidation")//
										.label(512, "DC converter CPU invalidation")//
										.label(1024, "DC converter TINT0 interrupt invalidation")//
										.label(2048, "DC converter ADC interrupt invalidation")//
										.label(4096, "DC converter CAPITN4 interrupt invalidation")//
										.label(8192, "DC converter CAPINT6 interrupt invalidation")//
										.label(16384, "DC converter T3PINTinterrupt invalidation")//
										.label(32768, "DC converter T4PINTinterrupt invalidation"))),
						new UnsignedWordElement(0xAA14,
								pvDCDC1Abnormity5 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity5", this)//
										.label(1, "DC converter PDPINTA interrupt invalidation")//
										.label(2, "DC converter T1PINT interrupt invalidation")//
										.label(4, "DC converter RESV interrupt invalidation")//
										.label(8, "DC converter 100us task invalidation")//
										.label(16, "DC converter clock  invalidation")//
										.label(32, "DC converter EMS memory invalidation")//
										.label(64, "DC converter exterior communication invalidation")//
										.label(128, "DC converter IO Interface invalidation")//
										.label(256, "DC converter Input Voltage bound fault")//
										.label(512, "DC converter Outter Voltage bound fault")//
										.label(1024, "DC converter Output Voltage bound fault")//
										.label(2048, "DC converter Induct Current bound fault")//
										.label(4096, "DC converter Input Current bound fault")//
										.label(8192, "DC converter Output Current bound fault"))),
						new UnsignedWordElement(0xAA15,
								pvDCDC1Abnormity6 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity6", this)//
										.label(1, "DC Reactor over temperature")//
										.label(2, "DC IGBT over temperature")//
										.label(4, "DC Converter chanel 3 over temperature")//
										.label(8, "DC Converter chanel 4 over temperature")//
										.label(16, "DC Converter chanel 5 over temperature")//
										.label(32, "DC Converter chanel 6 over temperature")//
										.label(64, "DC Converter chanel 7 over temperature")//
										.label(128, "DC Converter chanel 8 over temperature")//
										.label(256, "DC Reactor temperature sampling invalidation")//
										.label(512, "DC IGBT temperature sampling invalidation")//
										.label(1024, "DC Converter chanel 3 temperature sampling invalidation")//
										.label(2048, "DC Converter chanel 4 temperature sampling invalidation")//
										.label(4096, "DC Converter chanel 5 temperature sampling invalidation")//
										.label(8192, "DC Converter chanel 6 temperature sampling invalidation")//
										.label(16384, "DC Converter chanel 7 temperature sampling invalidation")//
										.label(32768, "DC Converter chanel 8 temperature sampling invalidation"))),
						new UnsignedWordElement(0xAA16,
								pvDCDC1Abnormity7 = warning.channel(new StatusBitChannel("PvDCDC1Abnormity7", this)//
										.label(32, "DC Converter inductance current sampling invalidation")//
										.label(64,
												"Current sampling invalidation on the low voltage sideof DC Converter")//
										.label(128,
												"Voltage sampling invalidation on the low voltage side of DC Converter")//
										.label(256, "Insulation inspection fault")//
										.label(512, "NegContactor close unsuccessly")//
										.label(1024, "NegContactor cut When running"))),
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

}
