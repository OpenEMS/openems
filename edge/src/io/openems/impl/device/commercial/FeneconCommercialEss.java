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

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.core.utilities.power.symmetric.PGreaterEqualLimitation;
import io.openems.core.utilities.power.symmetric.PSmallerEqualLimitation;
import io.openems.core.utilities.power.symmetric.QGreaterEqualLimitation;
import io.openems.core.utilities.power.symmetric.QSmallerEqualLimitation;
import io.openems.core.utilities.power.symmetric.SMaxLimitation;
import io.openems.core.utilities.power.symmetric.SymmetricPowerImpl;
import io.openems.impl.protocol.modbus.ModbusBitWrappingChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRegisterRange;

@ThingInfo(title = "FENECON Commercial ESS")
public class FeneconCommercialEss extends ModbusDeviceNature implements SymmetricEssNature {

	private ThingStateChannels thingState;

	/*
	 * Constructors
	 */
	public FeneconCommercialEss(String thingId, Device parent) throws ConfigException {
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
	private ModbusReadLongChannel soc;
	private ModbusReadLongChannel activePower;
	private ModbusReadLongChannel allowedCharge;
	private ModbusReadLongChannel allowedDischarge;
	private ModbusReadLongChannel apparentPower;
	private ModbusReadLongChannel gridMode;
	private ModbusReadLongChannel reactivePower;
	private ModbusReadLongChannel systemState;
	private ModbusWriteLongChannel setActivePower;
	private ModbusWriteLongChannel setReactivePower;
	private ModbusWriteLongChannel setWorkState;
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 40000L)
			.unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 40000L).unit("Wh");
	private SymmetricPowerImpl power;
	private QGreaterEqualLimitation qMinLimit;
	private QSmallerEqualLimitation qMaxLimit;
	private PGreaterEqualLimitation allowedChargeLimit;
	private PSmallerEqualLimitation allowedDischargeLimit;
	private SMaxLimitation allowedApparentLimit;

	@Override
	public ModbusReadLongChannel soc() {
		return soc;
	}

	@Override
	public ModbusReadLongChannel activePower() {
		return activePower;
	}

	@Override
	public ModbusReadLongChannel allowedCharge() {
		return allowedCharge;
	}

	@Override
	public ModbusReadLongChannel allowedDischarge() {
		return allowedDischarge;
	}

	@Override
	public ModbusReadLongChannel apparentPower() {
		return apparentPower;
	}

	@Override
	public ModbusReadLongChannel gridMode() {
		return gridMode;
	}

	@Override
	public ModbusReadLongChannel reactivePower() {
		return reactivePower;
	}

	@Override
	public ModbusReadLongChannel systemState() {
		return systemState;
	}

	@Override
	public ModbusWriteLongChannel setWorkState() {
		return setWorkState;
	}

	@Override
	public ModbusReadLongChannel allowedApparent() {
		return allowedApparent;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel controlMode;
	public ModbusReadLongChannel batteryMaintenanceState;
	public ModbusReadLongChannel inverterState;
	public ModbusReadLongChannel protocolVersion;
	public ModbusReadLongChannel systemManufacturer;
	public ModbusReadLongChannel systemType;
	public StatusBitChannel switchState;
	public ModbusReadLongChannel batteryVoltage;
	public ModbusReadLongChannel batteryCurrent;
	public ModbusReadLongChannel batteryPower;
	public ModbusReadLongChannel acChargeEnergy;
	public ModbusReadLongChannel acDischargeEnergy;
	public ModbusReadLongChannel currentL1;
	public ModbusReadLongChannel currentL2;
	public ModbusReadLongChannel currentL3;
	public ModbusReadLongChannel voltageL1;
	public ModbusReadLongChannel voltageL2;
	public ModbusReadLongChannel voltageL3;
	public ModbusReadLongChannel frequency;
	public ModbusReadLongChannel inverterVoltageL1;
	public ModbusReadLongChannel inverterVoltageL2;
	public ModbusReadLongChannel inverterVoltageL3;
	public ModbusReadLongChannel inverterCurrentL1;
	public ModbusReadLongChannel inverterCurrentL2;
	public ModbusReadLongChannel inverterCurrentL3;
	public ModbusReadLongChannel ipmTemperatureL1;
	public ModbusReadLongChannel ipmTemperatureL2;
	public ModbusReadLongChannel ipmTemperatureL3;
	public ModbusReadLongChannel transformerTemperatureL2;
	public ModbusReadLongChannel allowedApparent;
	public ModbusReadLongChannel gridActivePower;
	public ModbusReadLongChannel soh;
	public ModbusReadLongChannel batteryCellAverageTemperature;
	public StatusBitChannel suggestiveInformation1;
	public StatusBitChannel suggestiveInformation2;
	public StatusBitChannel suggestiveInformation3;
	public StatusBitChannel suggestiveInformation4;
	public StatusBitChannel suggestiveInformation5;
	public StatusBitChannel suggestiveInformation6;
	public StatusBitChannel suggestiveInformation7;
	public StatusBitChannel abnormity1;
	public StatusBitChannel abnormity2;
	public StatusBitChannel abnormity3;
	public StatusBitChannel abnormity4;
	public StatusBitChannel abnormity5;
	public ModbusReadLongChannel batteryCell1Voltage;
	public ModbusReadLongChannel batteryCell2Voltage;
	public ModbusReadLongChannel batteryCell3Voltage;
	public ModbusReadLongChannel batteryCell4Voltage;
	public ModbusReadLongChannel batteryCell5Voltage;
	public ModbusReadLongChannel batteryCell6Voltage;
	public ModbusReadLongChannel batteryCell7Voltage;
	public ModbusReadLongChannel batteryCell8Voltage;
	public ModbusReadLongChannel batteryCell9Voltage;
	public ModbusReadLongChannel batteryCell10Voltage;
	public ModbusReadLongChannel batteryCell11Voltage;
	public ModbusReadLongChannel batteryCell12Voltage;
	public ModbusReadLongChannel batteryCell13Voltage;
	public ModbusReadLongChannel batteryCell14Voltage;
	public ModbusReadLongChannel batteryCell15Voltage;
	public ModbusReadLongChannel batteryCell16Voltage;
	public ModbusReadLongChannel batteryCell17Voltage;
	public ModbusReadLongChannel batteryCell18Voltage;
	public ModbusReadLongChannel batteryCell19Voltage;
	public ModbusReadLongChannel batteryCell20Voltage;
	public ModbusReadLongChannel batteryCell21Voltage;
	public ModbusReadLongChannel batteryCell22Voltage;
	public ModbusReadLongChannel batteryCell23Voltage;
	public ModbusReadLongChannel batteryCell24Voltage;
	public ModbusReadLongChannel batteryCell25Voltage;
	public ModbusReadLongChannel batteryCell26Voltage;
	public ModbusReadLongChannel batteryCell27Voltage;
	public ModbusReadLongChannel batteryCell28Voltage;
	public ModbusReadLongChannel batteryCell29Voltage;
	public ModbusReadLongChannel batteryCell30Voltage;
	public ModbusReadLongChannel batteryCell31Voltage;
	public ModbusReadLongChannel batteryCell32Voltage;
	public ModbusReadLongChannel batteryCell33Voltage;
	public ModbusReadLongChannel batteryCell34Voltage;
	public ModbusReadLongChannel batteryCell35Voltage;
	public ModbusReadLongChannel batteryCell36Voltage;
	public ModbusReadLongChannel batteryCell37Voltage;
	public ModbusReadLongChannel batteryCell38Voltage;
	public ModbusReadLongChannel batteryCell39Voltage;
	public ModbusReadLongChannel batteryCell40Voltage;
	public ModbusReadLongChannel batteryCell41Voltage;
	public ModbusReadLongChannel batteryCell42Voltage;
	public ModbusReadLongChannel batteryCell43Voltage;
	public ModbusReadLongChannel batteryCell44Voltage;
	public ModbusReadLongChannel batteryCell45Voltage;
	public ModbusReadLongChannel batteryCell46Voltage;
	public ModbusReadLongChannel batteryCell47Voltage;
	public ModbusReadLongChannel batteryCell48Voltage;
	public ModbusReadLongChannel batteryCell49Voltage;
	public ModbusReadLongChannel batteryCell50Voltage;
	public ModbusReadLongChannel batteryCell51Voltage;
	public ModbusReadLongChannel batteryCell52Voltage;
	public ModbusReadLongChannel batteryCell53Voltage;
	public ModbusReadLongChannel batteryCell54Voltage;
	public ModbusReadLongChannel batteryCell55Voltage;
	public ModbusReadLongChannel batteryCell56Voltage;
	public ModbusReadLongChannel batteryCell57Voltage;
	public ModbusReadLongChannel batteryCell58Voltage;
	public ModbusReadLongChannel batteryCell59Voltage;
	public ModbusReadLongChannel batteryCell60Voltage;
	public ModbusReadLongChannel batteryCell61Voltage;
	public ModbusReadLongChannel batteryCell62Voltage;
	public ModbusReadLongChannel batteryCell63Voltage;
	public ModbusReadLongChannel batteryCell64Voltage;
	public ModbusReadLongChannel batteryCell65Voltage;
	public ModbusReadLongChannel batteryCell66Voltage;
	public ModbusReadLongChannel batteryCell67Voltage;
	public ModbusReadLongChannel batteryCell68Voltage;
	public ModbusReadLongChannel batteryCell69Voltage;
	public ModbusReadLongChannel batteryCell70Voltage;
	public ModbusReadLongChannel batteryCell71Voltage;
	public ModbusReadLongChannel batteryCell72Voltage;
	public ModbusReadLongChannel batteryCell73Voltage;
	public ModbusReadLongChannel batteryCell74Voltage;
	public ModbusReadLongChannel batteryCell75Voltage;
	public ModbusReadLongChannel batteryCell76Voltage;
	public ModbusReadLongChannel batteryCell77Voltage;
	public ModbusReadLongChannel batteryCell78Voltage;
	public ModbusReadLongChannel batteryCell79Voltage;
	public ModbusReadLongChannel batteryCell80Voltage;
	public ModbusReadLongChannel batteryCell81Voltage;
	public ModbusReadLongChannel batteryCell82Voltage;
	public ModbusReadLongChannel batteryCell83Voltage;
	public ModbusReadLongChannel batteryCell84Voltage;
	public ModbusReadLongChannel batteryCell85Voltage;
	public ModbusReadLongChannel batteryCell86Voltage;
	public ModbusReadLongChannel batteryCell87Voltage;
	public ModbusReadLongChannel batteryCell88Voltage;
	public ModbusReadLongChannel batteryCell89Voltage;
	public ModbusReadLongChannel batteryCell90Voltage;
	public ModbusReadLongChannel batteryCell91Voltage;
	public ModbusReadLongChannel batteryCell92Voltage;
	public ModbusReadLongChannel batteryCell93Voltage;
	public ModbusReadLongChannel batteryCell94Voltage;
	public ModbusReadLongChannel batteryCell95Voltage;
	public ModbusReadLongChannel batteryCell96Voltage;
	public ModbusReadLongChannel batteryCell97Voltage;
	public ModbusReadLongChannel batteryCell98Voltage;
	public ModbusReadLongChannel batteryCell99Voltage;
	public ModbusReadLongChannel batteryCell100Voltage;
	public ModbusReadLongChannel batteryCell101Voltage;
	public ModbusReadLongChannel batteryCell102Voltage;
	public ModbusReadLongChannel batteryCell103Voltage;
	public ModbusReadLongChannel batteryCell104Voltage;
	public ModbusReadLongChannel batteryCell105Voltage;
	public ModbusReadLongChannel batteryCell106Voltage;
	public ModbusReadLongChannel batteryCell107Voltage;
	public ModbusReadLongChannel batteryCell108Voltage;
	public ModbusReadLongChannel batteryCell109Voltage;
	public ModbusReadLongChannel batteryCell110Voltage;
	public ModbusReadLongChannel batteryCell111Voltage;
	public ModbusReadLongChannel batteryCell112Voltage;
	public ModbusReadLongChannel batteryCell113Voltage;
	public ModbusReadLongChannel batteryCell114Voltage;
	public ModbusReadLongChannel batteryCell115Voltage;
	public ModbusReadLongChannel batteryCell116Voltage;
	public ModbusReadLongChannel batteryCell117Voltage;
	public ModbusReadLongChannel batteryCell118Voltage;
	public ModbusReadLongChannel batteryCell119Voltage;
	public ModbusReadLongChannel batteryCell120Voltage;
	public ModbusReadLongChannel batteryCell121Voltage;
	public ModbusReadLongChannel batteryCell122Voltage;
	public ModbusReadLongChannel batteryCell123Voltage;
	public ModbusReadLongChannel batteryCell124Voltage;
	public ModbusReadLongChannel batteryCell125Voltage;
	public ModbusReadLongChannel batteryCell126Voltage;
	public ModbusReadLongChannel batteryCell127Voltage;
	public ModbusReadLongChannel batteryCell128Voltage;
	public ModbusReadLongChannel batteryCell129Voltage;
	public ModbusReadLongChannel batteryCell130Voltage;
	public ModbusReadLongChannel batteryCell131Voltage;
	public ModbusReadLongChannel batteryCell132Voltage;
	public ModbusReadLongChannel batteryCell133Voltage;
	public ModbusReadLongChannel batteryCell134Voltage;
	public ModbusReadLongChannel batteryCell135Voltage;
	public ModbusReadLongChannel batteryCell136Voltage;
	public ModbusReadLongChannel batteryCell137Voltage;
	public ModbusReadLongChannel batteryCell138Voltage;
	public ModbusReadLongChannel batteryCell139Voltage;
	public ModbusReadLongChannel batteryCell140Voltage;
	public ModbusReadLongChannel batteryCell141Voltage;
	public ModbusReadLongChannel batteryCell142Voltage;
	public ModbusReadLongChannel batteryCell143Voltage;
	public ModbusReadLongChannel batteryCell144Voltage;
	public ModbusReadLongChannel batteryCell145Voltage;
	public ModbusReadLongChannel batteryCell146Voltage;
	public ModbusReadLongChannel batteryCell147Voltage;
	public ModbusReadLongChannel batteryCell148Voltage;
	public ModbusReadLongChannel batteryCell149Voltage;
	public ModbusReadLongChannel batteryCell150Voltage;
	public ModbusReadLongChannel batteryCell151Voltage;
	public ModbusReadLongChannel batteryCell152Voltage;
	public ModbusReadLongChannel batteryCell153Voltage;
	public ModbusReadLongChannel batteryCell154Voltage;
	public ModbusReadLongChannel batteryCell155Voltage;
	public ModbusReadLongChannel batteryCell156Voltage;
	public ModbusReadLongChannel batteryCell157Voltage;
	public ModbusReadLongChannel batteryCell158Voltage;
	public ModbusReadLongChannel batteryCell159Voltage;
	public ModbusReadLongChannel batteryCell160Voltage;
	public ModbusReadLongChannel batteryCell161Voltage;
	public ModbusReadLongChannel batteryCell162Voltage;
	public ModbusReadLongChannel batteryCell163Voltage;
	public ModbusReadLongChannel batteryCell164Voltage;
	public ModbusReadLongChannel batteryCell165Voltage;
	public ModbusReadLongChannel batteryCell166Voltage;
	public ModbusReadLongChannel batteryCell167Voltage;
	public ModbusReadLongChannel batteryCell168Voltage;
	public ModbusReadLongChannel batteryCell169Voltage;
	public ModbusReadLongChannel batteryCell170Voltage;
	public ModbusReadLongChannel batteryCell171Voltage;
	public ModbusReadLongChannel batteryCell172Voltage;
	public ModbusReadLongChannel batteryCell173Voltage;
	public ModbusReadLongChannel batteryCell174Voltage;
	public ModbusReadLongChannel batteryCell175Voltage;
	public ModbusReadLongChannel batteryCell176Voltage;
	public ModbusReadLongChannel batteryCell177Voltage;
	public ModbusReadLongChannel batteryCell178Voltage;
	public ModbusReadLongChannel batteryCell179Voltage;
	public ModbusReadLongChannel batteryCell180Voltage;
	public ModbusReadLongChannel batteryCell181Voltage;
	public ModbusReadLongChannel batteryCell182Voltage;
	public ModbusReadLongChannel batteryCell183Voltage;
	public ModbusReadLongChannel batteryCell184Voltage;
	public ModbusReadLongChannel batteryCell185Voltage;
	public ModbusReadLongChannel batteryCell186Voltage;
	public ModbusReadLongChannel batteryCell187Voltage;
	public ModbusReadLongChannel batteryCell188Voltage;
	public ModbusReadLongChannel batteryCell189Voltage;
	public ModbusReadLongChannel batteryCell190Voltage;
	public ModbusReadLongChannel batteryCell191Voltage;
	public ModbusReadLongChannel batteryCell192Voltage;
	public ModbusReadLongChannel batteryCell193Voltage;
	public ModbusReadLongChannel batteryCell194Voltage;
	public ModbusReadLongChannel batteryCell195Voltage;
	public ModbusReadLongChannel batteryCell196Voltage;
	public ModbusReadLongChannel batteryCell197Voltage;
	public ModbusReadLongChannel batteryCell198Voltage;
	public ModbusReadLongChannel batteryCell199Voltage;
	public ModbusReadLongChannel batteryCell200Voltage;
	public ModbusReadLongChannel batteryCell201Voltage;
	public ModbusReadLongChannel batteryCell202Voltage;
	public ModbusReadLongChannel batteryCell203Voltage;
	public ModbusReadLongChannel batteryCell204Voltage;
	public ModbusReadLongChannel batteryCell205Voltage;
	public ModbusReadLongChannel batteryCell206Voltage;
	public ModbusReadLongChannel batteryCell207Voltage;
	public ModbusReadLongChannel batteryCell208Voltage;
	public ModbusReadLongChannel batteryCell209Voltage;
	public ModbusReadLongChannel batteryCell210Voltage;
	public ModbusReadLongChannel batteryCell211Voltage;
	public ModbusReadLongChannel batteryCell212Voltage;
	public ModbusReadLongChannel batteryCell213Voltage;
	public ModbusReadLongChannel batteryCell214Voltage;
	public ModbusReadLongChannel batteryCell215Voltage;
	public ModbusReadLongChannel batteryCell216Voltage;
	public ModbusReadLongChannel batteryCell217Voltage;
	public ModbusReadLongChannel batteryCell218Voltage;
	public ModbusReadLongChannel batteryCell219Voltage;
	public ModbusReadLongChannel batteryCell220Voltage;
	public ModbusReadLongChannel batteryCell221Voltage;
	public ModbusReadLongChannel batteryCell222Voltage;
	public ModbusReadLongChannel batteryCell223Voltage;
	public ModbusReadLongChannel batteryCell224Voltage;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(0x0101, //
						new UnsignedWordElement(0x0101, //
								systemState = new ModbusReadLongChannel("SystemState", this) //
								.label(2, STOP) //
								.label(4, "PV-Charge") //
								.label(8, "Standby") //
								.label(16, START) //
								.label(32, FAULT) //
								.label(64, "Debug")), //
						new UnsignedWordElement(0x0102, //
								controlMode = new ModbusReadLongChannel("ControlMode", this) //
								.label(1, "Remote") //
								.label(2, "Local")), //
						new DummyElement(0x0103), // WorkMode: RemoteDispatch
						new UnsignedWordElement(0x0104, //
								batteryMaintenanceState = new ModbusReadLongChannel("BatteryMaintenanceState", this) //
								.label(0, OFF) //
								.label(1, ON)), //
						new UnsignedWordElement(0x0105, //
								inverterState = new ModbusReadLongChannel("InverterState", this) //
								.label(0, "Init") //
								.label(2, "Fault") //
								.label(4, STOP) //
								.label(8, STANDBY) //
								.label(16, "Grid-Monitor") // ,
								.label(32, "Ready") //
								.label(64, START) //
								.label(128, "Debug")), //
						new UnsignedWordElement(0x0106, //
								gridMode = new ModbusReadLongChannel("GridMode", this) //
								.label(1, OFF_GRID) //
								.label(2, ON_GRID)), //
						new DummyElement(0x0107), //
						new UnsignedWordElement(0x0108, //
								protocolVersion = new ModbusReadLongChannel("ProtocolVersion", this)), //
						new UnsignedWordElement(0x0109, //
								systemManufacturer = new ModbusReadLongChannel("SystemManufacturer", this) //
								.label(1, "BYD")), //
						new UnsignedWordElement(0x010A, //
								systemType = new ModbusReadLongChannel("SystemType", this) //
								.label(1, "CESS")), //
						new DummyElement(0x010B, 0x010F), //
						new UnsignedWordElement(0x0110, //
								new ModbusBitWrappingChannel("SuggestiveInformation1", this, this.thingState) //
								.warningBit(2, WarningEss.EmergencyStop) // EmergencyStop
								.warningBit(6, WarningEss.KeyManualStop)), // KeyManualStop
						new UnsignedWordElement(0x0111, //
								new ModbusBitWrappingChannel("SuggestiveInformation2", this, this.thingState) //
								.warningBit(3, WarningEss.TransformerPhaseBTemperatureSensorInvalidation) // Transformer
								// phase
								// B
								// temperature
								// sensor
								// invalidation
								.warningBit(12, WarningEss.SDMemoryCardInvalidation)), // SD memory card
						// invalidation
						new DummyElement(0x0112, 0x0124), //
						new UnsignedWordElement(0x0125, //
								new ModbusBitWrappingChannel("SuggestiveInformation3", this, this.thingState)//
								.warningBit(0, WarningEss.InverterCommunicationAbnormity)//
								.warningBit(1, WarningEss.BatteryStackCommunicationAbnormity)//
								.warningBit(2, WarningEss.MultifunctionalAmmeterCommunicationAbnormity)//
								.warningBit(4, WarningEss.RemoteCommunicationAbnormity)//
								.warningBit(8, WarningEss.PVDC1CommunicationAbnormity)//
								.warningBit(9, WarningEss.PVDC2CommunicationAbnormity)//
								), //

						new UnsignedWordElement(0x0126, //
								new ModbusBitWrappingChannel("SuggestiveInformation4", this, this.thingState)//
								.warningBit(3, WarningEss.TransformerSevereOvertemperature)//
								), //

						new DummyElement(0x0127, 0x014F), //
						new UnsignedWordElement(0x0150, //
								switchState = new StatusBitChannel("BatteryStringSwitchState", this) //
								.label(1, "Main contactor") //
								.label(2, "Precharge contactor") //
								.label(4, "FAN contactor") //
								.label(8, "BMU power supply relay") //
								.label(16, "Middle relay"))//
						), //
				new ModbusRegisterRange(0x0180, //
						new UnsignedWordElement(0x0180, //
								new ModbusBitWrappingChannel("Abnormity1", this, this.thingState)//
								.faultBit(0, FaultEss.DCPrechargeContactorCloseUnsuccessfully)//
								.faultBit(1, FaultEss.ACPrechargeContactorCloseUnsuccessfully)//
								.faultBit(2, FaultEss.ACMainContactorCloseUnsuccessfully)//
								.faultBit(3, FaultEss.DCElectricalBreaker1CloseUnsuccessfully)//
								.faultBit(4, FaultEss.DCMainContactorCloseUnsuccessfully)//
								.faultBit(5, FaultEss.ACBreakerTrip)//
								.faultBit(6, FaultEss.ACMainContactorOpenWhenRunning)//
								.faultBit(7, FaultEss.DCMainContactorOpenWhenRunning)//
								.faultBit(8, FaultEss.ACMainContactorOpenUnsuccessfully)//
								.faultBit(9, FaultEss.DCElectricalBreaker1OpenUnsuccessfully)//
								.faultBit(10, FaultEss.DCMainContactorOpenUnsuccessfully)//
								.faultBit(11, FaultEss.HardwarePDPFault)//
								.faultBit(12, FaultEss.MasterStopSuddenly)//
								),

						new DummyElement(0x0181), new UnsignedWordElement(0x0182, //
								new ModbusBitWrappingChannel("Abnormity2", this, this.thingState)//
								.faultBit(0, FaultEss.DCShortCircuitProtection)//
								.faultBit(1, FaultEss.DCOvervoltageProtection)//
								.faultBit(2, FaultEss.DCUndervoltageProtection)//
								.faultBit(3, FaultEss.DCInverseNoConnectionProtection)//
								.faultBit(4, FaultEss.DCDisconnectionProtection)//
								.faultBit(5, FaultEss.CommutingVoltageAbnormityProtection)//
								.faultBit(6, FaultEss.DCOvercurrentProtection)//
								.faultBit(7, FaultEss.Phase1PeakCurrentOverLimitProtection)//
								.faultBit(8, FaultEss.Phase2PeakCurrentOverLimitProtection)//
								.faultBit(9, FaultEss.Phase3PeakCurrentOverLimitProtection)//
								.faultBit(10,FaultEss.Phase1GridVoltageSamplingInvalidation)//
								.faultBit(11, FaultEss.Phase2VirtualCurrentOverLimitProtection)//
								.faultBit(12, FaultEss.Phase3VirtualCurrentOverLimitProtection)//
								.faultBit(13, FaultEss.Phase1GridVoltageSamplingInvalidation2)//
								.faultBit(14, FaultEss.Phase2ridVoltageSamplingInvalidation)//
								.faultBit(15, FaultEss.Phase3GridVoltageSamplingInvalidation)//
								), //

						new UnsignedWordElement(0x0183, //
								new ModbusBitWrappingChannel("Abnormity3", this, this.thingState)//
								.faultBit(0, FaultEss.Phase1InvertVoltageSamplingInvalidation)//
								.faultBit(1, FaultEss.Phase2InvertVoltageSamplingInvalidation)//
								.faultBit(2, FaultEss.Phase3InvertVoltageSamplingInvalidation)//
								.faultBit(3, FaultEss.ACCurrentSamplingInvalidation)//
								.faultBit(4, FaultEss.DCCurrentSamplingInvalidation)//
								.faultBit(5, FaultEss.Phase1OvertemperatureProtection)//
								.faultBit(6, FaultEss.Phase2OvertemperatureProtection)//
								.faultBit(7, FaultEss.Phase3OvertemperatureProtection)//
								.faultBit(8, FaultEss.Phase1TemperatureSamplingInvalidation)//
								.faultBit(9, FaultEss.Phase2TemperatureSamplingInvalidation)//
								.faultBit(10, FaultEss.Phase3TemperatureSamplingInvalidation)//
								.faultBit(11, FaultEss.Phase1PrechargeUnmetProtection)//
								.faultBit(12, FaultEss.Phase2PrechargeUnmetProtection)//
								.faultBit(13, FaultEss.Phase3PrechargeUnmetProtection)//
								.faultBit(14, FaultEss.UnadaptablePhaseSequenceErrorProtection)//
								.faultBit(15, FaultEss.DSPProtection)//
								), //

						new UnsignedWordElement(0x0184, //
								new ModbusBitWrappingChannel("Abnormity4", this, this.thingState)//
								.faultBit(0, FaultEss.Phase1GridVoltageSevereOvervoltageProtection)//
								.faultBit(1, FaultEss.Phase1GridVoltageGeneralOvervoltageProtection)//
								.faultBit(2, FaultEss.Phase2GridVoltageSevereOvervoltageProtection)//
								.faultBit(3, FaultEss.Phase2GridVoltageGeneralOvervoltageProtection)//
								.faultBit(4, FaultEss.Phase3GridVoltageSevereOvervoltageProtection)//
								.faultBit(5, FaultEss.Phase3GridVoltageGeneralOvervoltageProtection)//
								.faultBit(6, FaultEss.Phase1GridVoltageSevereUndervoltageProtection)//
								.faultBit(7, FaultEss.Phase1GridVoltageGeneralUndervoltageProtection)//
								.faultBit(8, FaultEss.Phase2GridVoltageSevereUndervoltageProtection)//
								.faultBit(9, FaultEss.Phase2GridVoltageGeneralUndervoltageProtection)//
								.faultBit(10, FaultEss.Phase3GridVoltageSevereUndervoltageProtection)//
								.faultBit(11, FaultEss.Phase3GridVoltageGeneralUndervoltageProtection)//
								.faultBit(12, FaultEss.SevereOverfrequncyProtection)//
								.faultBit(13, FaultEss.GeneralOverfrequncyProtection)//
								.faultBit(14, FaultEss.SevereUnderfrequncyProtection)//
								.faultBit(15, FaultEss.GeneralsUnderfrequncyProtection)//
								), //

						new UnsignedWordElement(0x0185, //
								new ModbusBitWrappingChannel("Abnormity5", this, this.thingState)//
								.faultBit(0, FaultEss.Phase1Gridloss)//
								.faultBit(1, FaultEss.Phase2Gridloss)//
								.faultBit(2, FaultEss.Phase3Gridloss)//
								.faultBit(3, FaultEss.IslandingProtection)//
								.faultBit(4, FaultEss.Phase1UnderVoltageRideThrough)//
								.faultBit(5, FaultEss.Phase2UnderVoltageRideThrough)//
								.faultBit(6, FaultEss.Phase3UnderVoltageRideThrough)//
								.faultBit(7, FaultEss.Phase1InverterVoltageSevereOvervoltageProtection)//
								.faultBit(8, FaultEss.Phase1InverterVoltageGeneralOvervoltageProtection)//
								.faultBit(9, FaultEss.Phase2InverterVoltageSevereOvervoltageProtection)//
								.faultBit(10, FaultEss.Phase2InverterVoltageGeneralOvervoltageProtection)//
								.faultBit(11, FaultEss.Phase3InverterVoltageSevereOvervoltageProtection)//
								.faultBit(12, FaultEss.Phase3InverterVoltageGeneralOvervoltageProtection)//
								.faultBit(13, FaultEss.InverterPeakVoltageHighProtectionCauseByACDisconnect)//
								), //

						new UnsignedWordElement(0x0186, //
								new ModbusBitWrappingChannel("SuggestiveInformation5", this, this.thingState)//
								.warningBit(0, WarningEss.DCPrechargeContactorInspectionAbnormity)//
								.warningBit(1, WarningEss.DCBreaker1InspectionAbnormity)//
								.warningBit(2, WarningEss.DCBreaker2InspectionAbnormity)//
								.warningBit(3, WarningEss.ACPrechargeContactorInspectionAbnormity)//
								.warningBit(4, WarningEss.ACMainontactorInspectionAbnormity)//
								.warningBit(5, WarningEss.ACBreakerInspectionAbnormity)//
								.warningBit(6, WarningEss.DCBreaker1CloseUnsuccessfully)//
								.warningBit(7, WarningEss.DCBreaker2CloseUnsuccessfully)//
								.warningBit(8, WarningEss.ControlSignalCloseAbnormallyInspectedBySystem)//
								.warningBit(9, WarningEss.ControlSignalOpenAbnormallyInspectedBySystem)//
								.warningBit(10, WarningEss.NeutralWireContactorCloseUnsuccessfully)//
								.warningBit(11, WarningEss.NeutralWireContactorOpenUnsuccessfully)//
								.warningBit(12, WarningEss.WorkDoorOpen)//
								.warningBit(13, WarningEss.Emergency1Stop)//
								.warningBit(14, WarningEss.ACBreakerCloseUnsuccessfully)//
								.warningBit(15, WarningEss.ControlSwitchStop)//
								), //

						new UnsignedWordElement(0x0187, //
								new ModbusBitWrappingChannel("SuggestiveInformation6", this, this.thingState)//
								.warningBit(0, WarningEss.GeneralOverload)//
								.warningBit(1, WarningEss.SevereOverload)//
								.warningBit(2, WarningEss.BatteryCurrentOverLimit)//
								.warningBit(3, WarningEss.PowerDecreaseCausedByOvertemperature)//
								.warningBit(4, WarningEss.InverterGeneralOvertemperature)//
								.warningBit(5, WarningEss.ACThreePhaseCurrentUnbalance)//
								.warningBit(6, WarningEss.RestoreFactorySettingUnsuccessfully)//
								.warningBit(7, WarningEss.PoleBoardInvalidation)//
								.warningBit(8, WarningEss.SelfInspectionFailed)//
								.warningBit(9, WarningEss.ReceiveBMSFaultAndStop)//
								.warningBit(10, WarningEss.RefrigerationEquipmentinvalidation)//
								.warningBit(11, WarningEss.LargeTemperatureDifferenceAmongIGBTThreePhases)//
								.warningBit(12, WarningEss.EEPROMParametersOverRange)//
								.warningBit(13, WarningEss.EEPROMParametersBackupFailed)//
								.warningBit(14, WarningEss.DCBreakerCloseunsuccessfully)//
								), //
						new UnsignedWordElement(0x0188, //
								new ModbusBitWrappingChannel("SuggestiveInformation7", this, this.thingState)//
								.warningBit(0, WarningEss.CommunicationBetweenInverterAndBSMUDisconnected)//
								.warningBit(1, WarningEss.CommunicationBetweenInverterAndMasterDisconnected)//
								.warningBit(2, WarningEss.CommunicationBetweenInverterAndUCDisconnected)//
								.warningBit(3, WarningEss.BMSStartOvertimeControlledByPCS)//
								.warningBit(4, WarningEss.BMSStopOvertimeControlledByPCS)//
								.warningBit(5, WarningEss.SyncSignalInvalidation)//
								.warningBit(6, WarningEss.SyncSignalContinuousCaputureFault)//
								.warningBit(7, WarningEss.SyncSignalSeveralTimesCaputureFault))),

				new ModbusRegisterRange(0x0200, //
						new SignedWordElement(0x0200, //
								batteryVoltage = new ModbusReadLongChannel("BatteryVoltage", this).unit("mV")
								.multiplier(2)),
						new SignedWordElement(0x0201, //
								batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this).unit("mA")
								.multiplier(2)),
						new SignedWordElement(0x0202, //
								batteryPower = new ModbusReadLongChannel("BatteryPower", this).unit("W").multiplier(2)),
						new DummyElement(0x0203, 0x0207), //
						new UnsignedDoublewordElement(0x0208, //
								acChargeEnergy = new ModbusReadLongChannel("AcChargeEnergy", this).unit("Wh")
								.multiplier(2)).wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(0x020A, //
								acDischargeEnergy = new ModbusReadLongChannel("AcDischargeEnergy", this).unit("Wh")
								.multiplier(2)).wordOrder(WordOrder.LSWMSW),
						new DummyElement(0x020C, 0x020F), new SignedWordElement(0x0210, //
								gridActivePower = new ModbusReadLongChannel("GridActivePower", this).unit("W")
								.multiplier(2)),
						new SignedWordElement(0x0211, //
								reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("var")
								.multiplier(2)),
						new UnsignedWordElement(0x0212, //
								apparentPower = new ModbusReadLongChannel("ApparentPower", this).unit("VA")
								.multiplier(2)),
						new SignedWordElement(0x0213, //
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA").multiplier(2)),
						new SignedWordElement(0x0214, //
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA").multiplier(2)),
						new SignedWordElement(0x0215, //
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA").multiplier(2)),
						new DummyElement(0x0216, 0x218), //
						new UnsignedWordElement(0x0219, //
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(2)),
						new UnsignedWordElement(0x021A, //
								voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV").multiplier(2)),
						new UnsignedWordElement(0x021B, //
								voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV").multiplier(2)),
						new UnsignedWordElement(0x021C, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHZ").multiplier(1))),
				new ModbusRegisterRange(0x0222, //
						new UnsignedWordElement(0x0222, //
								inverterVoltageL1 = new ModbusReadLongChannel("InverterVoltageL1", this).unit("mV")
								.multiplier(2)), //
						new UnsignedWordElement(0x0223, //
								inverterVoltageL2 = new ModbusReadLongChannel("InverterVoltageL2", this).unit("mV")
								.multiplier(2)), //
						new UnsignedWordElement(0x0224, //
								inverterVoltageL3 = new ModbusReadLongChannel("InverterVoltageL3", this).unit("mV")
								.multiplier(2)), //
						new UnsignedWordElement(0x0225, //
								inverterCurrentL1 = new ModbusReadLongChannel("InverterCurrentL1", this).unit("mA")
								.multiplier(2)), //
						new UnsignedWordElement(0x0226, //
								inverterCurrentL2 = new ModbusReadLongChannel("InverterCurrentL2", this).unit("mA")
								.multiplier(2)), //
						new UnsignedWordElement(0x0227, //
								inverterCurrentL3 = new ModbusReadLongChannel("InverterCurrentL3", this).unit("mA")
								.multiplier(2)), //
						new SignedWordElement(0x0228, //
								activePower = new ModbusReadLongChannel("ActivePower", this).unit("W").multiplier(2)), //
						new DummyElement(0x0229, 0x022F), new SignedWordElement(0x0230, //
								allowedCharge = new ModbusReadLongChannel("AllowedCharge", this).unit("W")
								.multiplier(2)), //
						new UnsignedWordElement(0x0231, //
								allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this).unit("W")
								.multiplier(2)), //
						new UnsignedWordElement(0x0232, //
								allowedApparent = new ModbusReadLongChannel("AllowedApparent", this).unit("VA")
								.multiplier(2)), //
						new DummyElement(0x0233, 0x23F), new SignedWordElement(0x0240, //
								ipmTemperatureL1 = new ModbusReadLongChannel("IpmTemperatureL1", this).unit("�C")), //
						new SignedWordElement(0x0241, //
								ipmTemperatureL2 = new ModbusReadLongChannel("IpmTemperatureL2", this).unit("�C")), //
						new SignedWordElement(0x0242, //
								ipmTemperatureL3 = new ModbusReadLongChannel("IpmTemperatureL3", this).unit("�C")), //
						new DummyElement(0x0243, 0x0248), new SignedWordElement(0x0249, //
								transformerTemperatureL2 = new ModbusReadLongChannel("TransformerTemperatureL2", this)
								.unit("�C"))),
				new WriteableModbusRegisterRange(0x0500, //
						new UnsignedWordElement(0x0500, //
								setWorkState = new ModbusWriteLongChannel("SetWorkState", this) //
								.label(4, STOP) //
								.label(32, STANDBY) //
								.label(64, START))),
				new WriteableModbusRegisterRange(0x0501, //
						new SignedWordElement(0x0501, //
								setActivePower = new ModbusWriteLongChannel("SetActivePower", this).unit("W")
								.multiplier(2).minWriteChannel(allowedCharge)
								.maxWriteChannel(allowedDischarge)),
						new SignedWordElement(0x0502, //
								setReactivePower = new ModbusWriteLongChannel("SetReactivePower", this).unit("var")
								.multiplier(2).minWriteChannel(allowedCharge)
								.maxWriteChannel(allowedDischarge))),
				new ModbusRegisterRange(0x1402, //
						new UnsignedWordElement(0x1402,
								soc = new ModbusReadLongChannel("Soc", this).unit("%").interval(0, 100)),
						new UnsignedWordElement(0x1403,
								soh = new ModbusReadLongChannel("Soh", this).unit("%").interval(0, 100)),
						new UnsignedWordElement(0x1404,
								batteryCellAverageTemperature = new ModbusReadLongChannel(
										"BatteryCellAverageTemperature", this).unit("°C"))),
				new ModbusRegisterRange(0x1500, //
						new UnsignedWordElement(0x1500,
								batteryCell1Voltage = new ModbusReadLongChannel("Cell1Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1501,
								batteryCell2Voltage = new ModbusReadLongChannel("Cell2Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1502,
								batteryCell3Voltage = new ModbusReadLongChannel("Cell3Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1503,
								batteryCell4Voltage = new ModbusReadLongChannel("Cell4Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1504,
								batteryCell5Voltage = new ModbusReadLongChannel("Cell5Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1505,
								batteryCell6Voltage = new ModbusReadLongChannel("Cell6Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1506,
								batteryCell7Voltage = new ModbusReadLongChannel("Cell7Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1507,
								batteryCell8Voltage = new ModbusReadLongChannel("Cell8Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1508,
								batteryCell9Voltage = new ModbusReadLongChannel("Cell9Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1509,
								batteryCell10Voltage = new ModbusReadLongChannel("Cell10Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x150A,
								batteryCell11Voltage = new ModbusReadLongChannel("Cell11Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x150B,
								batteryCell12Voltage = new ModbusReadLongChannel("Cell12Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x150C,
								batteryCell13Voltage = new ModbusReadLongChannel("Cell13Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x150D,
								batteryCell14Voltage = new ModbusReadLongChannel("Cell14Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x150E,
								batteryCell15Voltage = new ModbusReadLongChannel("Cell15Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x150F,
								batteryCell16Voltage = new ModbusReadLongChannel("Cell16Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1510,
								batteryCell17Voltage = new ModbusReadLongChannel("Cell17Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1511,
								batteryCell18Voltage = new ModbusReadLongChannel("Cell18Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1512,
								batteryCell19Voltage = new ModbusReadLongChannel("Cell19Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1513,
								batteryCell20Voltage = new ModbusReadLongChannel("Cell20Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1514,
								batteryCell21Voltage = new ModbusReadLongChannel("Cell21Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1515,
								batteryCell22Voltage = new ModbusReadLongChannel("Cell22Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1516,
								batteryCell23Voltage = new ModbusReadLongChannel("Cell23Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1517,
								batteryCell24Voltage = new ModbusReadLongChannel("Cell24Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1518,
								batteryCell25Voltage = new ModbusReadLongChannel("Cell25Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1519,
								batteryCell26Voltage = new ModbusReadLongChannel("Cell26Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x151A,
								batteryCell27Voltage = new ModbusReadLongChannel("Cell27Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x151B,
								batteryCell28Voltage = new ModbusReadLongChannel("Cell28Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x151C,
								batteryCell29Voltage = new ModbusReadLongChannel("Cell29Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x151D,
								batteryCell30Voltage = new ModbusReadLongChannel("Cell30Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x151E,
								batteryCell31Voltage = new ModbusReadLongChannel("Cell31Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x151F,
								batteryCell32Voltage = new ModbusReadLongChannel("Cell32Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1520,
								batteryCell33Voltage = new ModbusReadLongChannel("Cell33Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1521,
								batteryCell34Voltage = new ModbusReadLongChannel("Cell34Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1522,
								batteryCell35Voltage = new ModbusReadLongChannel("Cell35Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1523,
								batteryCell36Voltage = new ModbusReadLongChannel("Cell36Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1524,
								batteryCell37Voltage = new ModbusReadLongChannel("Cell37Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1525,
								batteryCell38Voltage = new ModbusReadLongChannel("Cell38Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1526,
								batteryCell39Voltage = new ModbusReadLongChannel("Cell39Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1527,
								batteryCell40Voltage = new ModbusReadLongChannel("Cell40Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1528,
								batteryCell41Voltage = new ModbusReadLongChannel("Cell41Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1529,
								batteryCell42Voltage = new ModbusReadLongChannel("Cell42Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x152A,
								batteryCell43Voltage = new ModbusReadLongChannel("Cell43Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x152B,
								batteryCell44Voltage = new ModbusReadLongChannel("Cell44Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x152C,
								batteryCell45Voltage = new ModbusReadLongChannel("Cell45Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x152D,
								batteryCell46Voltage = new ModbusReadLongChannel("Cell46Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x152E,
								batteryCell47Voltage = new ModbusReadLongChannel("Cell47Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x152F,
								batteryCell48Voltage = new ModbusReadLongChannel("Cell48Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1530,
								batteryCell49Voltage = new ModbusReadLongChannel("Cell49Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1531,
								batteryCell50Voltage = new ModbusReadLongChannel("Cell50Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1532,
								batteryCell51Voltage = new ModbusReadLongChannel("Cell51Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1533,
								batteryCell52Voltage = new ModbusReadLongChannel("Cell52Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1534,
								batteryCell53Voltage = new ModbusReadLongChannel("Cell53Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1535,
								batteryCell54Voltage = new ModbusReadLongChannel("Cell54Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1536,
								batteryCell55Voltage = new ModbusReadLongChannel("Cell55Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1537,
								batteryCell56Voltage = new ModbusReadLongChannel("Cell56Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1538,
								batteryCell57Voltage = new ModbusReadLongChannel("Cell57Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1539,
								batteryCell58Voltage = new ModbusReadLongChannel("Cell58Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x153A,
								batteryCell59Voltage = new ModbusReadLongChannel("Cell59Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x153B,
								batteryCell60Voltage = new ModbusReadLongChannel("Cell60Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x153C,
								batteryCell61Voltage = new ModbusReadLongChannel("Cell61Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x153D,
								batteryCell62Voltage = new ModbusReadLongChannel("Cell62Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x153E,
								batteryCell63Voltage = new ModbusReadLongChannel("Cell63Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x153F,
								batteryCell64Voltage = new ModbusReadLongChannel("Cell64Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1540,
								batteryCell65Voltage = new ModbusReadLongChannel("Cell65Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1541,
								batteryCell66Voltage = new ModbusReadLongChannel("Cell66Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1542,
								batteryCell67Voltage = new ModbusReadLongChannel("Cell67Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1543,
								batteryCell68Voltage = new ModbusReadLongChannel("Cell68Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1544,
								batteryCell69Voltage = new ModbusReadLongChannel("Cell69Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1545,
								batteryCell70Voltage = new ModbusReadLongChannel("Cell70Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1546,
								batteryCell71Voltage = new ModbusReadLongChannel("Cell71Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1547,
								batteryCell72Voltage = new ModbusReadLongChannel("Cell72Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1548,
								batteryCell73Voltage = new ModbusReadLongChannel("Cell73Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1549,
								batteryCell74Voltage = new ModbusReadLongChannel("Cell74Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x154A,
								batteryCell75Voltage = new ModbusReadLongChannel("Cell75Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x154B,
								batteryCell76Voltage = new ModbusReadLongChannel("Cell76Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x154C,
								batteryCell77Voltage = new ModbusReadLongChannel("Cell77Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x154D,
								batteryCell78Voltage = new ModbusReadLongChannel("Cell78Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x154E,
								batteryCell79Voltage = new ModbusReadLongChannel("Cell79Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x154F,
								batteryCell80Voltage = new ModbusReadLongChannel("Cell80Voltage", this).unit("mV")
								)),
				new ModbusRegisterRange(0x1550, //
						new UnsignedWordElement(0x1550,
								batteryCell81Voltage = new ModbusReadLongChannel("Cell81Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1551,
								batteryCell82Voltage = new ModbusReadLongChannel("Cell82Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1552,
								batteryCell83Voltage = new ModbusReadLongChannel("Cell83Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1553,
								batteryCell84Voltage = new ModbusReadLongChannel("Cell84Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1554,
								batteryCell85Voltage = new ModbusReadLongChannel("Cell85Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1555,
								batteryCell86Voltage = new ModbusReadLongChannel("Cell86Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1556,
								batteryCell87Voltage = new ModbusReadLongChannel("Cell87Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1557,
								batteryCell88Voltage = new ModbusReadLongChannel("Cell88Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1558,
								batteryCell89Voltage = new ModbusReadLongChannel("Cell89Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1559,
								batteryCell90Voltage = new ModbusReadLongChannel("Cell90Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x155A,
								batteryCell91Voltage = new ModbusReadLongChannel("Cell91Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x155B,
								batteryCell92Voltage = new ModbusReadLongChannel("Cell92Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x155C,
								batteryCell93Voltage = new ModbusReadLongChannel("Cell93Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x155D,
								batteryCell94Voltage = new ModbusReadLongChannel("Cell94Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x155E,
								batteryCell95Voltage = new ModbusReadLongChannel("Cell95Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x155F,
								batteryCell96Voltage = new ModbusReadLongChannel("Cell96Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1560,
								batteryCell97Voltage = new ModbusReadLongChannel("Cell97Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1561,
								batteryCell98Voltage = new ModbusReadLongChannel("Cell98Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1562,
								batteryCell99Voltage = new ModbusReadLongChannel("Cell99Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1563,
								batteryCell100Voltage = new ModbusReadLongChannel("Cell100Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1564,
								batteryCell101Voltage = new ModbusReadLongChannel("Cell101Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1565,
								batteryCell102Voltage = new ModbusReadLongChannel("Cell102Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1566,
								batteryCell103Voltage = new ModbusReadLongChannel("Cell103Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1567,
								batteryCell104Voltage = new ModbusReadLongChannel("Cell104Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1568,
								batteryCell105Voltage = new ModbusReadLongChannel("Cell105Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1569,
								batteryCell106Voltage = new ModbusReadLongChannel("Cell106Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x156A,
								batteryCell107Voltage = new ModbusReadLongChannel("Cell107Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x156B,
								batteryCell108Voltage = new ModbusReadLongChannel("Cell108Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x156C,
								batteryCell109Voltage = new ModbusReadLongChannel("Cell109Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x156D,
								batteryCell110Voltage = new ModbusReadLongChannel("Cell110Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x156E,
								batteryCell111Voltage = new ModbusReadLongChannel("Cell111Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x156F,
								batteryCell112Voltage = new ModbusReadLongChannel("Cell112Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1570,
								batteryCell113Voltage = new ModbusReadLongChannel("Cell113Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1571,
								batteryCell114Voltage = new ModbusReadLongChannel("Cell114Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1572,
								batteryCell115Voltage = new ModbusReadLongChannel("Cell115Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1573,
								batteryCell116Voltage = new ModbusReadLongChannel("Cell116Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1574,
								batteryCell117Voltage = new ModbusReadLongChannel("Cell117Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1575,
								batteryCell118Voltage = new ModbusReadLongChannel("Cell18Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1576,
								batteryCell119Voltage = new ModbusReadLongChannel("Cell119Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1577,
								batteryCell120Voltage = new ModbusReadLongChannel("Cell120Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1578,
								batteryCell121Voltage = new ModbusReadLongChannel("Cell121Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1579,
								batteryCell122Voltage = new ModbusReadLongChannel("Cell122Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x157A,
								batteryCell123Voltage = new ModbusReadLongChannel("Cell123Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x157B,
								batteryCell124Voltage = new ModbusReadLongChannel("Cell124Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x157C,
								batteryCell125Voltage = new ModbusReadLongChannel("Cell125Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x157D,
								batteryCell126Voltage = new ModbusReadLongChannel("Cell126Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x157E,
								batteryCell127Voltage = new ModbusReadLongChannel("Cell127Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x157F,
								batteryCell128Voltage = new ModbusReadLongChannel("Cell128Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1580,
								batteryCell129Voltage = new ModbusReadLongChannel("Cell129Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1581,
								batteryCell130Voltage = new ModbusReadLongChannel("Cell130Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1582,
								batteryCell131Voltage = new ModbusReadLongChannel("Cell131Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1583,
								batteryCell132Voltage = new ModbusReadLongChannel("Cell132Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1584,
								batteryCell133Voltage = new ModbusReadLongChannel("Cell133Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1585,
								batteryCell134Voltage = new ModbusReadLongChannel("Cell134Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1586,
								batteryCell135Voltage = new ModbusReadLongChannel("Cell135Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1587,
								batteryCell136Voltage = new ModbusReadLongChannel("Cell136Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1588,
								batteryCell137Voltage = new ModbusReadLongChannel("Cell137Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1589,
								batteryCell138Voltage = new ModbusReadLongChannel("Cell138Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x158A,
								batteryCell139Voltage = new ModbusReadLongChannel("Cell139Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x158B,
								batteryCell140Voltage = new ModbusReadLongChannel("Cell140Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x158C,
								batteryCell141Voltage = new ModbusReadLongChannel("Cell141Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x158D,
								batteryCell142Voltage = new ModbusReadLongChannel("Cell142Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x158E,
								batteryCell143Voltage = new ModbusReadLongChannel("Cell143Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x158F,
								batteryCell144Voltage = new ModbusReadLongChannel("Cell144Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1590,
								batteryCell145Voltage = new ModbusReadLongChannel("Cell145Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1591,
								batteryCell146Voltage = new ModbusReadLongChannel("Cell146Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1592,
								batteryCell147Voltage = new ModbusReadLongChannel("Cell147Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1593,
								batteryCell148Voltage = new ModbusReadLongChannel("Cell148Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1594,
								batteryCell149Voltage = new ModbusReadLongChannel("Cell149Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1595,
								batteryCell150Voltage = new ModbusReadLongChannel("Cell150Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1596,
								batteryCell151Voltage = new ModbusReadLongChannel("Cell151Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1597,
								batteryCell152Voltage = new ModbusReadLongChannel("Cell152Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1598,
								batteryCell153Voltage = new ModbusReadLongChannel("Cell153Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x1599,
								batteryCell154Voltage = new ModbusReadLongChannel("Cell154Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x159A,
								batteryCell155Voltage = new ModbusReadLongChannel("Cell155Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x159B,
								batteryCell156Voltage = new ModbusReadLongChannel("Cell156Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x159C,
								batteryCell157Voltage = new ModbusReadLongChannel("Cell157Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x159D,
								batteryCell158Voltage = new ModbusReadLongChannel("Cell158Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x159E,
								batteryCell159Voltage = new ModbusReadLongChannel("Cell159Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x159F,
								batteryCell160Voltage = new ModbusReadLongChannel("Cell160Voltage", this).unit("mV")
								)),//
				new ModbusRegisterRange(0x15A0, //
						new UnsignedWordElement(0x15A0,
								batteryCell161Voltage = new ModbusReadLongChannel("Cell161Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A1,
								batteryCell162Voltage = new ModbusReadLongChannel("Cell162Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A2,
								batteryCell163Voltage = new ModbusReadLongChannel("Cell163Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A3,
								batteryCell164Voltage = new ModbusReadLongChannel("Cell164Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A4,
								batteryCell165Voltage = new ModbusReadLongChannel("Cell165Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A5,
								batteryCell166Voltage = new ModbusReadLongChannel("Cell166Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A6,
								batteryCell167Voltage = new ModbusReadLongChannel("Cell167Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A7,
								batteryCell168Voltage = new ModbusReadLongChannel("Cell168Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A8,
								batteryCell169Voltage = new ModbusReadLongChannel("Cell169Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15A9,
								batteryCell170Voltage = new ModbusReadLongChannel("Cell170Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15AA,
								batteryCell171Voltage = new ModbusReadLongChannel("Cell171Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15AB,
								batteryCell172Voltage = new ModbusReadLongChannel("Cell172Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15AC,
								batteryCell173Voltage = new ModbusReadLongChannel("Cell173Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15AD,
								batteryCell174Voltage = new ModbusReadLongChannel("Cell174Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15AE,
								batteryCell175Voltage = new ModbusReadLongChannel("Cell175Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15AF,
								batteryCell176Voltage = new ModbusReadLongChannel("Cell176Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B0,
								batteryCell177Voltage = new ModbusReadLongChannel("Cell177Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B1,
								batteryCell178Voltage = new ModbusReadLongChannel("Cell178Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B2,
								batteryCell179Voltage = new ModbusReadLongChannel("Cell179Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B3,
								batteryCell180Voltage = new ModbusReadLongChannel("Cell180Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B4,
								batteryCell181Voltage = new ModbusReadLongChannel("Cell181Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B5,
								batteryCell182Voltage = new ModbusReadLongChannel("Cell182Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B6,
								batteryCell183Voltage = new ModbusReadLongChannel("Cell183Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B7,
								batteryCell184Voltage = new ModbusReadLongChannel("Cell184Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B8,
								batteryCell185Voltage = new ModbusReadLongChannel("Cell185Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15B9,
								batteryCell186Voltage = new ModbusReadLongChannel("Cell186Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15BA,
								batteryCell187Voltage = new ModbusReadLongChannel("Cell187Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15BB,
								batteryCell188Voltage = new ModbusReadLongChannel("Cell188Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15BC,
								batteryCell189Voltage = new ModbusReadLongChannel("Cell189Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15BD,
								batteryCell190Voltage = new ModbusReadLongChannel("Cell190Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15BE,
								batteryCell191Voltage = new ModbusReadLongChannel("Cell191Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15BF,
								batteryCell192Voltage = new ModbusReadLongChannel("Cell192Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C0,
								batteryCell193Voltage = new ModbusReadLongChannel("Cell193Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C1,
								batteryCell194Voltage = new ModbusReadLongChannel("Cell194Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C2,
								batteryCell195Voltage = new ModbusReadLongChannel("Cell195Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C3,
								batteryCell196Voltage = new ModbusReadLongChannel("Cell196Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C4,
								batteryCell197Voltage = new ModbusReadLongChannel("Cell197Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C5,
								batteryCell198Voltage = new ModbusReadLongChannel("Cell198Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C6,
								batteryCell199Voltage = new ModbusReadLongChannel("Cell199Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C7,
								batteryCell200Voltage = new ModbusReadLongChannel("Cell200Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C8,
								batteryCell201Voltage = new ModbusReadLongChannel("Cell201Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15C9,
								batteryCell202Voltage = new ModbusReadLongChannel("Cell202Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15CA,
								batteryCell203Voltage = new ModbusReadLongChannel("Cell203Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15CB,
								batteryCell204Voltage = new ModbusReadLongChannel("Cell204Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15CC,
								batteryCell205Voltage = new ModbusReadLongChannel("Cell205Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15CD,
								batteryCell206Voltage = new ModbusReadLongChannel("Cell206Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15CE,
								batteryCell207Voltage = new ModbusReadLongChannel("Cell207Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15CF,
								batteryCell208Voltage = new ModbusReadLongChannel("Cell208Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D0,
								batteryCell209Voltage = new ModbusReadLongChannel("Cell209Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D1,
								batteryCell210Voltage = new ModbusReadLongChannel("Cell210Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D2,
								batteryCell211Voltage = new ModbusReadLongChannel("Cell211Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D3,
								batteryCell212Voltage = new ModbusReadLongChannel("Cell212Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D4,
								batteryCell213Voltage = new ModbusReadLongChannel("Cell213Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D5,
								batteryCell214Voltage = new ModbusReadLongChannel("Cell214Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D6,
								batteryCell215Voltage = new ModbusReadLongChannel("Cell215Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D7,
								batteryCell216Voltage = new ModbusReadLongChannel("Cell216Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D8,
								batteryCell217Voltage = new ModbusReadLongChannel("Cell217Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15D9,
								batteryCell218Voltage = new ModbusReadLongChannel("Cell218Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15DA,
								batteryCell219Voltage = new ModbusReadLongChannel("Cell219Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15DB,
								batteryCell220Voltage = new ModbusReadLongChannel("Cell220Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15DC,
								batteryCell221Voltage = new ModbusReadLongChannel("Cell221Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15DD,
								batteryCell222Voltage = new ModbusReadLongChannel("Cell222Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15DE,
								batteryCell223Voltage = new ModbusReadLongChannel("Cell223Voltage", this).unit("mV")
								),//
						new UnsignedWordElement(0x15DF,
								batteryCell224Voltage = new ModbusReadLongChannel("Cell224Voltage", this).unit("mV")
								)));
		this.power = new SymmetricPowerImpl(40000, setActivePower, setReactivePower, getParent().getBridge());
		this.qMinLimit  = new QGreaterEqualLimitation(power);
		this.qMinLimit.setQ(-10000L);
		this.power.addStaticLimitation(qMinLimit);
		this.qMaxLimit  = new QSmallerEqualLimitation(power);
		this.qMaxLimit.setQ(10000L);
		this.power.addStaticLimitation(qMaxLimit);
		this.allowedApparentLimit = new SMaxLimitation(power);
		this.allowedApparentLimit.setSMax(allowedApparent.valueOptional().orElse(0L), 0L, 0L);
		this.allowedApparent.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				allowedApparentLimit.setSMax(allowedApparent.valueOptional().orElse(0L), 0L, 0L);
			}
		});
		this.power.addStaticLimitation(this.allowedApparentLimit);
		this.allowedChargeLimit = new PGreaterEqualLimitation(power);
		this.allowedChargeLimit.setP(this.allowedCharge.valueOptional().orElse(0L));
		this.allowedCharge.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				allowedChargeLimit.setP(allowedCharge.valueOptional().orElse(0L));
			}
		});
		this.power.addStaticLimitation(this.allowedChargeLimit);
		this.allowedDischargeLimit = new PSmallerEqualLimitation(power);
		this.allowedDischargeLimit.setP(this.allowedDischarge.valueOptional().orElse(0L));
		this.allowedDischarge.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				allowedDischargeLimit.setP(allowedDischarge.valueOptional().orElse(0L));
			}
		});
		this.power.addStaticLimitation(this.allowedDischargeLimit);
		return protocol;
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return thingState;
	}

	@Override
	public SymmetricPowerImpl getPower() {
		return power;
	}

}
