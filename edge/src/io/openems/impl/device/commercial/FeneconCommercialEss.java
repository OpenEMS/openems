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
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
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
	public StatusBitChannels warning;

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
	public ModbusWriteLongChannel setActivePower() {
		return setActivePower;
	}

	@Override
	public ModbusWriteLongChannel setReactivePower() {
		return setReactivePower;
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
	public StatusBitChannels warning() {
		return warning;
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

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		warning = new StatusBitChannels("Warning", this);
		return new ModbusProtocol( //
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
								suggestiveInformation1 = warning
								.channel(new StatusBitChannel("SuggestiveInformation1", this) //
										.label(4, "EmergencyStop") //
										.label(64, "KeyManualStop"))), //
						new UnsignedWordElement(0x0111, //
								suggestiveInformation2 = warning
								.channel(new StatusBitChannel("SuggestiveInformation2", this) //
										.label(4, "EmergencyStop") //
										.label(64, "KeyManualStop"))), //
						new DummyElement(0x0112, 0x0124), //
						new UnsignedWordElement(0x0125, //
								suggestiveInformation3 = warning
								.channel(new StatusBitChannel("SuggestiveInformation3", this) //
										.label(1, "Inverter communication abnormity") //
										.label(2, "Battery stack communication abnormity") //
										.label(4, "Multifunctional ammeter communication abnormity") //
										.label(16, "Remote communication abnormity")//
										.label(256, "PV DC1 communication abnormity")//
										.label(512, "PV DC2 communication abnormity")//
										)), //
						new UnsignedWordElement(0x0126, //
								suggestiveInformation4 = warning
								.channel(new StatusBitChannel("SuggestiveInformation4", this) //
										.label(8, "Transformer severe overtemperature"))), //
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
						new UnsignedWordElement(0x0180,
								abnormity1 = warning.channel(new StatusBitChannel("Abnormity1", this)//
										.label(1, "DC precharge contactor close unsuccessfully") //
										.label(2, "AC precharge contactor close unsuccessfully") //
										.label(4, "AC main contactor close unsuccessfully") //
										.label(8, "DC electrical breaker 1 close unsuccessfully") //
										.label(16, "DC main contactor close unsuccessfully") //
										.label(32, "AC breaker trip") //
										.label(64, "AC main contactor open when running") //
										.label(128, "DC main contactor open when running") //
										.label(256, "AC main contactor open unsuccessfully") //
										.label(512, "DC electrical breaker 1 open unsuccessfully") //
										.label(1024, "DC main contactor open unsuccessfully") //
										.label(2048, "Hardware PDP fault") //
										.label(4096, "Master stop suddenly"))),
						new DummyElement(0x0181),
						new UnsignedWordElement(0x0182,
								abnormity2 = warning.channel(new StatusBitChannel("Abnormity2", this) //
										.label(1, "DC short circuit protection") //
										.label(2, "DC overvoltage protection") //
										.label(4, "DC undervoltage protection") //
										.label(8, "DC inverse/no connection protection") //
										.label(16, "DC disconnection protection") //
										.label(32, "Commuting voltage abnormity protection") //
										.label(64, "DC overcurrent protection") //
										.label(128, "Phase 1 peak current over limit protection") //
										.label(256, "Phase 2 peak current over limit protection") //
										.label(512, "Phase 3 peak current over limit protection") //
										.label(1024, "Phase 1 grid voltage sampling invalidation") //
										.label(2048, "Phase 2 virtual current over limit protection") //
										.label(4096, "Phase 3 virtual current over limit protection") //
										.label(8192, "Phase 1 grid voltage sampling invalidation2") // TODO same as
										// above
										.label(16384, "Phase 2 grid voltage sampling invalidation") //
										.label(32768, "Phase 3 grid voltage sampling invalidation"))),
						new UnsignedWordElement(0x0183,
								abnormity3 = warning.channel(new StatusBitChannel("Abnormity3", this) //
										.label(1, "Phase 1 invert voltage sampling invalidation") //
										.label(2, "Phase 2 invert voltage sampling invalidation") //
										.label(4, "Phase 3 invert voltage sampling invalidation") //
										.label(8, "AC current sampling invalidation") //
										.label(16, "DC current sampling invalidation") //
										.label(32, "Phase 1 overtemperature protection") //
										.label(64, "Phase 2 overtemperature protection") //
										.label(128, "Phase 3 overtemperature protection") //
										.label(256, "Phase 1 temperature sampling invalidation") //
										.label(512, "Phase 2 temperature sampling invalidation") //
										.label(1024, "Phase 3 temperature sampling invalidation") //
										.label(2048, "Phase 1 precharge unmet protection") //
										.label(4096, "Phase 2 precharge unmet protection") //
										.label(8192, "Phase 3 precharge unmet protection") //
										.label(16384, "Unadaptable phase sequence error protection")//
										.label(132768, "DSP protection"))),
						new UnsignedWordElement(0x0184,
								abnormity4 = warning.channel(new StatusBitChannel("Abnormity4", this) //
										.label(1, "Phase 1 grid voltage severe overvoltage protection") //
										.label(2, "Phase 1 grid voltage general overvoltage protection") //
										.label(4, "Phase 2 grid voltage severe overvoltage protection") //
										.label(8, "Phase 2 grid voltage general overvoltage protection") //
										.label(16, "Phase 3 grid voltage severe overvoltage protection") //
										.label(32, "Phase 3 grid voltage general overvoltage protection") //
										.label(64, "Phase 1 grid voltage severe undervoltage protection") //
										.label(128, "Phase 1 grid voltage general undervoltage protection") //
										.label(256, "Phase 2 grid voltage severe undervoltage protection") //
										.label(512, "Phase 2 grid voltage general undervoltage protection") //
										.label(1024, "Phase 2 Inverter voltage general overvoltage protection") //
										.label(2048, "Phase 3 Inverter voltage severe overvoltage protection") //
										.label(4096, "Phase 3 Inverter voltage general overvoltage protection") //
										.label(8192, "Inverter peak voltage high protection cause by AC disconnect"))),
						new UnsignedWordElement(0x0185,
								abnormity5 = warning.channel(new StatusBitChannel("Abnormity5", this) //
										.label(1, "Phase 1 grid loss") //
										.label(2, "Phase 2 grid loss") //
										.label(4, "Phase 3 grid loss") //
										.label(8, "Islanding protection") //
										.label(16, "Phase 1 under voltage ride through") //
										.label(32, "Phase 2 under voltage ride through") //
										.label(64, "Phase 3 under voltage ride through ") //
										.label(128, "Phase 1 Inverter voltage severe overvoltage protection") //
										.label(256, "Phase 1 Inverter voltage general overvoltage protection") //
										.label(512, "Phase 2 Inverter voltage severe overvoltage protection") //
										.label(1024, "Phase 2 Inverter voltage general overvoltage protection") //
										.label(2048, "Phase 3 Inverter voltage severe overvoltage protection") //
										.label(4096, "Phase 3 Inverter voltage general overvoltage protection") //
										.label(8192, "Inverter peak voltage high protection cause by AC disconnect"))),
						new UnsignedWordElement(0x0186,
								suggestiveInformation5 = warning
								.channel(new StatusBitChannel("SuggestiveInformation5", this) //
										.label(1, "DC precharge contactor inspection abnormity") //
										.label(2, "DC breaker 1 inspection abnormity ") //
										.label(4, "DC breaker 2 inspection abnormity ") //
										.label(8, "AC precharge contactor inspection abnormity ") //
										.label(16, "AC main contactor inspection abnormity ") //
										.label(32, "AC breaker inspection abnormity ") //
										.label(64, "DC breaker 1 close unsuccessfully") //
										.label(128, "DC breaker 2 close unsuccessfully") //
										.label(256, "Control signal close abnormally inspected by system") //
										.label(512, "Control signal open abnormally inspected by system") //
										.label(1024, "Neutral wire contactor close unsuccessfully") //
										.label(2048, "Neutral wire contactor open unsuccessfully") //
										.label(4096, "Work door open") //
										.label(8192, "Emergency stop") //
										.label(16384, "AC breaker close unsuccessfully")//
										.label(132768, "Control switch stop"))),
						new UnsignedWordElement(0x0187,
								suggestiveInformation6 = warning
								.channel(new StatusBitChannel("SuggestiveInformation6", this) //
										.label(1, "General overload") //
										.label(2, "Severe overload") //
										.label(4, "Battery current over limit") //
										.label(8, "Power decrease caused by overtemperature") //
										.label(16, "Inverter general overtemperature") //
										.label(32, "AC three-phase current unbalance") //
										.label(64, "Rstore factory setting unsuccessfully") //
										.label(128, "Pole-board invalidation") //
										.label(256, "Self-inspection failed") //
										.label(512, "Receive BMS fault and stop") //
										.label(1024, "Refrigeration equipment invalidation") //
										.label(2048, "Large temperature difference among IGBT three phases") //
										.label(4096, "EEPROM parameters over range") //
										.label(8192, "EEPROM parameters backup failed") //
										.label(16384, "DC breaker close unsuccessfully"))),
						new UnsignedWordElement(0x0188,
								suggestiveInformation7 = warning
								.channel(new StatusBitChannel("SuggestiveInformation7", this) //
										.label(1, "Communication between inverter and BSMU disconnected") //
										.label(2, "Communication between inverter and Master disconnected") //
										.label(4, "Communication between inverter and UC disconnected") //
										.label(8, "BMS start overtime controlled by PCS") //
										.label(16, "BMS stop overtime controlled by PCS") //
										.label(32, "Sync signal invalidation") //
										.label(64, "Sync signal continuous caputure fault") //
										.label(128, "Sync signal several times caputure fault")))),
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
								batteryCellAverageTemperature = new ModbusReadLongChannel("BatteryCellAverageTemperature", this).unit("°C"))
						));

	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	// @IsChannel(id = "BatteryAccumulatedCharge")
	// public final ModbusReadChannel _batteryAccumulatedCharge = new OldModbusChannelBuilder().nature(this).unit("Wh")
	// .build();
	// @IsChannel(id = "BatteryAccumulatedDischarge")
	// public final ModbusReadChannel _batteryAccumulatedDischarge = new
	// OldModbusChannelBuilder().nature(this).unit("Wh")
	// .build();
	// @IsChannel(id = "BatteryChargeCycles")
	// public final ModbusReadChannel _batteryChargeCycles = new OldModbusChannelBuilder().nature(this).build();

	// @IsChannel(id = "BatteryPower")
	// public final ModbusReadChannel _batteryPower = new
	// OldModbusChannelBuilder().nature(this).unit("W").multiplier(100)
	// .build();
	// @IsChannel(id = "BatteryStringTotalCurrent")
	// public final ModbusReadChannel _batteryStringTotalCurrent = new OldModbusChannelBuilder().nature(this).unit("mA")
	// .multiplier(100).build();
	// @IsChannel(id = "BatteryStringAbnormity1")
	// public final ModbusReadChannel _batteryStringAbnormity1 = new OldModbusChannelBuilder().nature(this) //
	// .label(4, "Battery string voltage sampling route invalidation") //
	// .label(16, "Battery string voltage sampling route disconnected") //
	// .label(32, "Battery string temperature sampling route disconnected") //
	// .label(64, "Battery string inside CAN disconnected") //
	// .label(512, "Battery string current sampling circuit abnormity") //
	// .label(1024, "Battery string battery cell invalidation") //
	// .label(2048, "Battery string main contactor inspection abnormity") //
	// .label(4096, "Battery string precharge contactor inspection abnormity") //
	// .label(8192, "Battery string negative contactor inspection abnormity") //
	// .label(16384, "Battery string power supply relay inspection abnormity")//
	// .label(132768, "Battery string middle relay abnormity").build();
	// @IsChannel(id = "BatteryStringAbnormity2")
	// public final ModbusReadChannel _batteryStringAbnormity2 = new OldModbusChannelBuilder().nature(this) //
	// .label(4, "Battery string severe overtemperature") //
	// .label(128, "Battery string smog fault") //
	// .label(256, "Battery string blown fuse indicator fault") //
	// .label(1024, "Battery string general leakage") //
	// .label(2048, "Battery string severe leakage") //
	// .label(4096, "Communication between BECU and periphery CAN disconnected") //
	// .label(16384, "Battery string power supply relay contactor disconnected").build();
	// @IsChannel(id = "BatteryStringCellAverageTemperature")
	// public final ModbusReadChannel _batteryStringCellAverageTemperature = new OldModbusChannelBuilder().nature(this)
	// .unit("�C").multiplier(100).build();
	// @IsChannel(id = "BatteryStringChargeCurrentLimit")
	// public final ModbusReadChannel _batteryStringChargeCurrentLimit = new OldModbusChannelBuilder().nature(this)
	// .unit("mA").multiplier(100).build();
	// @IsChannel(id = "BatteryStringDischargeCurrentLimit")
	// public final ModbusReadChannel _batteryStringDischargeCurrentLimit = new OldModbusChannelBuilder().nature(this)
	// .unit("mA").multiplier(100).build();
	// @IsChannel(id = "BatteryStringPeripheralIoState")
	// public final ModbusReadChannel _batteryStringPeripheralIoState = new OldModbusChannelBuilder().nature(this)
	// .label(1, "Fuse state") //
	// .label(2, "Isolated switch state").build();
	// @IsChannel(id = "BatteryStringSOH")
	// public final ModbusReadChannel _batteryStringSOH = new OldModbusChannelBuilder().nature(this).unit("%")
	// .multiplier(100).build();
	// @IsChannel(id = "BatteryStringSuggestiveInformation")
	// public final ModbusReadChannel _batteryStringSuggestiveInformation = new OldModbusChannelBuilder().nature(this)
	// .label(1, "Battery string charge general overcurrent") //
	// .label(2, "Battery string discharge general overcurrent") //
	// .label(4, "Battery string charge current over limit") //
	// .label(8, "Battery string discharge current over limit") //
	// .label(16, "Battery string general overvoltage") //
	// .label(32, "Battery string general undervoltage") //
	// .label(128, "Battery string general over temperature") //
	// .label(256, "Battery string general under temperature") //
	// .label(1024, "Battery string severe overvoltage") //
	// .label(2048, "Battery string severe under voltage") //
	// .label(4096, "Battery string severe under temperature") //
	// .label(8192, "Battery string charge severe overcurrent") //
	// .label(16384, "Battery string discharge severe overcurrent")//
	// .label(132768, "Battery string capacity abnormity").build();

	// @IsChannel(id = "BatteryStringTotalVoltage")
	// public final ModbusReadChannel _batteryStringTotalVoltage = new OldModbusChannelBuilder().nature(this).unit("mV")
	// .multiplier(100).build();
	// @IsChannel(id = "BatteryStringWorkState")
	// public final ModbusReadChannel _batteryStringWorkState = new OldModbusChannelBuilder().nature(this) //
	// .label(1, "Initial") //
	// .label(2, "Stop") //
	// .label(4, "Starting up") //
	// .label(8, "Running") //
	// .label(16, "Fault").build();

	// private final OldConfigChannel _minSoc = new OldConfigChannelBuilder().nature(this).defaultValue(DEFAULT_MINSOC)
	// .percentType().build();

	// @IsChannel(id = "Abnormity1")

	// @IsChannel(id = "SwitchState")
	// public final ModbusReadChannel _switchState = new OldModbusChannelBuilder().nature(this) //
	// .label(2, "DC main contactor state") //
	// .label(4, "DC precharge contactor state") //
	// .label(8, "AC breaker state") //
	// .label(16, "AC main contactor state") //
	// .label(32, "AC precharge contactor state").build();

	// @IsChannel(id = "TotalDateEnergy")
	// public final ModbusReadChannel _totalDateEnergy = new OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalEnergy")
	// public final ModbusReadChannel _totalEnergy = new OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy0")
	// public final ModbusReadChannel _totalHourEnergy0 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy1")
	// public final ModbusReadChannel _totalHourEnergy1 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy10")
	// public final ModbusReadChannel _totalHourEnergy10 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy11")
	// public final ModbusReadChannel _totalHourEnergy11 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy12")
	// public final ModbusReadChannel _totalHourEnergy12 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy13")
	// public final ModbusReadChannel _totalHourEnergy13 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy14")
	// public final ModbusReadChannel _totalHourEnergy14 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy15")
	// public final ModbusReadChannel _totalHourEnergy15 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy16")
	// public final ModbusReadChannel _totalHourEnergy16 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy17")
	// public final ModbusReadChannel _totalHourEnergy17 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy18")
	// public final ModbusReadChannel _totalHourEnergy18 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy19")
	// public final ModbusReadChannel _totalHourEnergy19 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy2")
	// public final ModbusReadChannel _totalHourEnergy2 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy20")
	// public final ModbusReadChannel _totalHourEnergy20 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy21")
	// public final ModbusReadChannel _totalHourEnergy21 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy22")
	// public final ModbusReadChannel _totalHourEnergy22 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy23")
	// public final ModbusReadChannel _totalHourEnergy23 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy3")
	// public final ModbusReadChannel _totalHourEnergy3 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy4")
	// public final ModbusReadChannel _totalHourEnergy4 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy5")
	// public final ModbusReadChannel _totalHourEnergy5 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy6")
	// public final ModbusReadChannel _totalHourEnergy6 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy7")
	// public final ModbusReadChannel _totalHourEnergy7 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy8")
	// public final ModbusReadChannel _totalHourEnergy8 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalHourEnergy9")
	// public final ModbusReadChannel _totalHourEnergy9 = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalMonthEnergy")
	// public final ModbusReadChannel _totalMonthEnergy = new
	// OldModbusChannelBuilder().nature(this).unit("kWh").build();
	// @IsChannel(id = "TotalYearEnergy")
	// public final ModbusReadChannel _totalYearEnergy = new OldModbusChannelBuilder().nature(this).unit("kWh").build();

	// @IsChannel(id = "MaxVoltageCellNo")
	// public final ModbusReadChannel _maxVoltageCellNo = new OldModbusChannelBuilder().nature(this).build();
	// @IsChannel(id = "MaxVoltageCellVoltage")
	// public final ModbusReadChannel _maxVoltageCellVoltage = new OldModbusChannelBuilder().nature(this).unit("mV")
	// .build();
	// @IsChannel(id = "MaxVoltageCellTemp")
	// public final ModbusReadChannel _maxVoltageCellTemp = new
	// OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "MinVoltageCellNo")
	// public final ModbusReadChannel _minVoltageCellNo = new OldModbusChannelBuilder().nature(this).build();
	// @IsChannel(id = "MinVoltageCellVoltage")
	// public final ModbusReadChannel _minVoltageCellVoltage = new OldModbusChannelBuilder().nature(this).unit("mV")
	// .build();
	// @IsChannel(id = "MinVoltageCellTemp")
	// public final ModbusReadChannel _minVoltageCellTemp = new
	// OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "MaxTempCellNo")
	// public final ModbusReadChannel _maxTempCellNo = new OldModbusChannelBuilder().nature(this).build();
	// @IsChannel(id = "MaxTempCellVoltage")
	// public final ModbusReadChannel _maxTempCellVoltage = new
	// OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "MaxTempCellTemp")
	// public final ModbusReadChannel _maxTempCellTemp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "MinTempCellNo")
	// public final ModbusReadChannel _minTempCellNo = new OldModbusChannelBuilder().nature(this).build();
	// @IsChannel(id = "MinTempCellVoltage")
	// public final ModbusReadChannel _minTempCellVoltage = new
	// OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "MinTempCellTemp")
	// public final ModbusReadChannel _minTempCellTemp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell1Voltage")
	// public final ModbusReadChannel _cell1Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell2Voltage")
	// public final ModbusReadChannel _cell2Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell3Voltage")
	// public final ModbusReadChannel _cell3Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell4Voltage")
	// public final ModbusReadChannel _cell4Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell5Voltage")
	// public final ModbusReadChannel _cell5Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell6Voltage")
	// public final ModbusReadChannel _cell6Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell7Voltage")
	// public final ModbusReadChannel _cell7Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell8Voltage")
	// public final ModbusReadChannel _cell8Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell9Voltage")
	// public final ModbusReadChannel _cell9Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell10Voltage")
	// public final ModbusReadChannel _cell10Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell11Voltage")
	// public final ModbusReadChannel _cell11Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell12Voltage")
	// public final ModbusReadChannel _cell12Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell13Voltage")
	// public final ModbusReadChannel _cell13Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell14Voltage")
	// public final ModbusReadChannel _cell14Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell15Voltage")
	// public final ModbusReadChannel _cell15Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell16Voltage")
	// public final ModbusReadChannel _cell16Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell17Voltage")
	// public final ModbusReadChannel _cell17Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell18Voltage")
	// public final ModbusReadChannel _cell18Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell19Voltage")
	// public final ModbusReadChannel _cell19Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell20Voltage")
	// public final ModbusReadChannel _cell20Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell21Voltage")
	// public final ModbusReadChannel _cell21Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell22Voltage")
	// public final ModbusReadChannel _cell22Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell23Voltage")
	// public final ModbusReadChannel _cell23Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell24Voltage")
	// public final ModbusReadChannel _cell24Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell25Voltage")
	// public final ModbusReadChannel _cell25Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell26Voltage")
	// public final ModbusReadChannel _cell26Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell27Voltage")
	// public final ModbusReadChannel _cell27Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell28Voltage")
	// public final ModbusReadChannel _cell28Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell29Voltage")
	// public final ModbusReadChannel _cell29Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell30Voltage")
	// public final ModbusReadChannel _cell30Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell31Voltage")
	// public final ModbusReadChannel _cell31Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell32Voltage")
	// public final ModbusReadChannel _cell32Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell33Voltage")
	// public final ModbusReadChannel _cell33Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell34Voltage")
	// public final ModbusReadChannel _cell34Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell35Voltage")
	// public final ModbusReadChannel _cell35Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell36Voltage")
	// public final ModbusReadChannel _cell36Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell37Voltage")
	// public final ModbusReadChannel _cell37Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell38Voltage")
	// public final ModbusReadChannel _cell38Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell39Voltage")
	// public final ModbusReadChannel _cell39Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell40Voltage")
	// public final ModbusReadChannel _cell40Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell41Voltage")
	// public final ModbusReadChannel _cell41Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell42Voltage")
	// public final ModbusReadChannel _cell42Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell43Voltage")
	// public final ModbusReadChannel _cell43Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell44Voltage")
	// public final ModbusReadChannel _cell44Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell45Voltage")
	// public final ModbusReadChannel _cell45Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell46Voltage")
	// public final ModbusReadChannel _cell46Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell47Voltage")
	// public final ModbusReadChannel _cell47Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell48Voltage")
	// public final ModbusReadChannel _cell48Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell49Voltage")
	// public final ModbusReadChannel _cell49Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell50Voltage")
	// public final ModbusReadChannel _cell50Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell51Voltage")
	// public final ModbusReadChannel _cell51Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell52Voltage")
	// public final ModbusReadChannel _cell52Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell53Voltage")
	// public final ModbusReadChannel _cell53Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell54Voltage")
	// public final ModbusReadChannel _cell54Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell55Voltage")
	// public final ModbusReadChannel _cell55Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell56Voltage")
	// public final ModbusReadChannel _cell56Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell57Voltage")
	// public final ModbusReadChannel _cell57Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell58Voltage")
	// public final ModbusReadChannel _cell58Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell59Voltage")
	// public final ModbusReadChannel _cell59Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell60Voltage")
	// public final ModbusReadChannel _cell60Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell61Voltage")
	// public final ModbusReadChannel _cell61Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell62Voltage")
	// public final ModbusReadChannel _cell62Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell63Voltage")
	// public final ModbusReadChannel _cell63Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	// @IsChannel(id = "Cell64Voltage")
	// public final ModbusReadChannel _cell64Voltage = new OldModbusChannelBuilder().nature(this).unit("mV").build();
	//
	// @IsChannel(id = "Cell1Temp")
	// public final ModbusReadChannel _cell1Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell2Temp")
	// public final ModbusReadChannel _cell2Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell3Temp")
	// public final ModbusReadChannel _cell3Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell4Temp")
	// public final ModbusReadChannel _cell4Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell5Temp")
	// public final ModbusReadChannel _cell5Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell6Temp")
	// public final ModbusReadChannel _cell6Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell7Temp")
	// public final ModbusReadChannel _cell7Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell8Temp")
	// public final ModbusReadChannel _cell8Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell9Temp")
	// public final ModbusReadChannel _cell9Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell10Temp")
	// public final ModbusReadChannel _cell10Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell11Temp")
	// public final ModbusReadChannel _cell11Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell12Temp")
	// public final ModbusReadChannel _cell12Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell13Temp")
	// public final ModbusReadChannel _cell13Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell14Temp")
	// public final ModbusReadChannel _cell14Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell15Temp")
	// public final ModbusReadChannel _cell15Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell16Temp")
	// public final ModbusReadChannel _cell16Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell17Temp")
	// public final ModbusReadChannel _cell17Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell18Temp")
	// public final ModbusReadChannel _cell18Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell19Temp")
	// public final ModbusReadChannel _cell19Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell20Temp")
	// public final ModbusReadChannel _cell20Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell21Temp")
	// public final ModbusReadChannel _cell21Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell22Temp")
	// public final ModbusReadChannel _cell22Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell23Temp")
	// public final ModbusReadChannel _cell23Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell24Temp")
	// public final ModbusReadChannel _cell24Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell25Temp")
	// public final ModbusReadChannel _cell25Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell26Temp")
	// public final ModbusReadChannel _cell26Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell27Temp")
	// public final ModbusReadChannel _cell27Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell28Temp")
	// public final ModbusReadChannel _cell28Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell29Temp")
	// public final ModbusReadChannel _cell29Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell30Temp")
	// public final ModbusReadChannel _cell30Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell31Temp")
	// public final ModbusReadChannel _cell31Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell32Temp")
	// public final ModbusReadChannel _cell32Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell33Temp")
	// public final ModbusReadChannel _cell33Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell34Temp")
	// public final ModbusReadChannel _cell34Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell35Temp")
	// public final ModbusReadChannel _cell35Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell36Temp")
	// public final ModbusReadChannel _cell36Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell37Temp")
	// public final ModbusReadChannel _cell37Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell38Temp")
	// public final ModbusReadChannel _cell38Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell39Temp")
	// public final ModbusReadChannel _cell39Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell40Temp")
	// public final ModbusReadChannel _cell40Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell41Temp")
	// public final ModbusReadChannel _cell41Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell42Temp")
	// public final ModbusReadChannel _cell42Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell43Temp")
	// public final ModbusReadChannel _cell43Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell44Temp")
	// public final ModbusReadChannel _cell44Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell45Temp")
	// public final ModbusReadChannel _cell45Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell46Temp")
	// public final ModbusReadChannel _cell46Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell47Temp")
	// public final ModbusReadChannel _cell47Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell48Temp")
	// public final ModbusReadChannel _cell48Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell49Temp")
	// public final ModbusReadChannel _cell49Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell50Temp")
	// public final ModbusReadChannel _cell50Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell51Temp")
	// public final ModbusReadChannel _cell51Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell52Temp")
	// public final ModbusReadChannel _cell52Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell53Temp")
	// public final ModbusReadChannel _cell53Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell54Temp")
	// public final ModbusReadChannel _cell54Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell55Temp")
	// public final ModbusReadChannel _cell55Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell56Temp")
	// public final ModbusReadChannel _cell56Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell57Temp")
	// public final ModbusReadChannel _cell57Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell58Temp")
	// public final ModbusReadChannel _cell58Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell59Temp")
	// public final ModbusReadChannel _cell59Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell60Temp")
	// public final ModbusReadChannel _cell60Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell61Temp")
	// public final ModbusReadChannel _cell61Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell62Temp")
	// public final ModbusReadChannel _cell62Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell63Temp")
	// public final ModbusReadChannel _cell63Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();
	// @IsChannel(id = "Cell64Temp")
	// public final ModbusReadChannel _cell64Temp = new OldModbusChannelBuilder().nature(this).unit("�C").build();

	// @Override
	// protected ModbusProtocol defineModbusProtocol() throws ConfigException {
	//

	// new ModbusRange(0x0300, //
	// new ElementBuilder().address(0x0300).channel(_totalEnergy).doubleword().build(),
	// new ElementBuilder().address(0x0302).channel(_totalYearEnergy).doubleword().build(),
	// new ElementBuilder().address(0x0304).channel(_totalMonthEnergy).doubleword().build(),
	// new ElementBuilder().address(0x0306).channel(_totalDateEnergy).build(),
	// new ElementBuilder().address(0x0307).channel(_totalHourEnergy0).build(),
	// new ElementBuilder().address(0x0308).channel(_totalHourEnergy1).build(),
	// new ElementBuilder().address(0x0309).channel(_totalHourEnergy2).build(),
	// new ElementBuilder().address(0x030A).channel(_totalHourEnergy3).build(),
	// new ElementBuilder().address(0x030B).channel(_totalHourEnergy4).build(),
	// new ElementBuilder().address(0x030C).channel(_totalHourEnergy5).build(),
	// new ElementBuilder().address(0x030D).channel(_totalHourEnergy6).build(),
	// new ElementBuilder().address(0x030E).channel(_totalHourEnergy7).build(),
	// new ElementBuilder().address(0x030F).channel(_totalHourEnergy8).build(),
	// new ElementBuilder().address(0x0310).channel(_totalHourEnergy9).build(),
	// new ElementBuilder().address(0x0311).channel(_totalHourEnergy10).build(),
	// new ElementBuilder().address(0x0312).channel(_totalHourEnergy11).build(),
	// new ElementBuilder().address(0x0313).channel(_totalHourEnergy12).build(),
	// new ElementBuilder().address(0x0314).channel(_totalHourEnergy13).build(),
	// new ElementBuilder().address(0x0315).channel(_totalHourEnergy14).build(),
	// new ElementBuilder().address(0x0316).channel(_totalHourEnergy15).build(),
	// new ElementBuilder().address(0x0317).channel(_totalHourEnergy16).build(),
	// new ElementBuilder().address(0x0318).channel(_totalHourEnergy17).build(),
	// new ElementBuilder().address(0x0319).channel(_totalHourEnergy18).build(),
	// new ElementBuilder().address(0x031A).channel(_totalHourEnergy19).build(),
	// new ElementBuilder().address(0x031B).channel(_totalHourEnergy20).build(),
	// new ElementBuilder().address(0x031C).channel(_totalHourEnergy21).build(),
	// new ElementBuilder().address(0x031D).channel(_totalHourEnergy22).build(),
	// new ElementBuilder().address(0x031E).channel(_totalHourEnergy23).build()),

	// new ModbusRange(0x1100, //
	// new ElementBuilder().address(0x1100).channel(_batteryStringWorkState).build(),
	// new ElementBuilder().address(0x1101).channel(_batteryStringSwitchState).build(),
	// new ElementBuilder().address(0x1102).channel(_batteryStringPeripheralIoState).build(),
	// new ElementBuilder().address(0x1103).channel(_batteryStringSuggestiveInformation).build(),
	// new ElementBuilder().address(0x1104).dummy().build(),
	// new ElementBuilder().address(0x1105).channel(_batteryStringAbnormity1).build(),
	// new ElementBuilder().address(0x1106).channel(_batteryStringAbnormity2).build()),
	// new ModbusRange(0x1400, //
	// new ElementBuilder().address(0x1400).channel(_batteryStringTotalVoltage).build(),
	// new ElementBuilder().address(0x1401).channel(_batteryStringTotalCurrent).signed().build(),
	// new ElementBuilder().address(0x1402).channel(_soc).build(),
	// new ElementBuilder().address(0x1403).channel(_batteryStringSOH).build(),
	// new ElementBuilder().address(0x1404).channel(_batteryStringCellAverageTemperature).signed()
	// .build(),
	// new ElementBuilder().address(0x1405).dummy().build(),
	// new ElementBuilder().address(0x1406).channel(_batteryStringChargeCurrentLimit).build(),
	// new ElementBuilder().address(0x1407).channel(_batteryStringDischargeCurrentLimit).build(),
	// new ElementBuilder().address(0x1408).dummy(0x140A - 0x1408).build(),
	// new ElementBuilder().address(0x140A).channel(_batteryChargeCycles).doubleword().build(),
	// new ElementBuilder().address(0x140C).dummy(0x1418 - 0x140C).build(),
	// new ElementBuilder().address(0x1418).channel(_batteryAccumulatedCharge).doubleword().build(),
	// new ElementBuilder().address(0x141A).channel(_batteryAccumulatedDischarge).doubleword().build(),
	// new ElementBuilder().address(0x141C).dummy(0x1420 - 0x141C).build(),
	// new ElementBuilder().address(0x1420).channel(_batteryPower).signed().build(),
	// new ElementBuilder().address(0x1421).dummy(0x1430 - 0x1421).build(),
	// new ElementBuilder().address(0x1430).channel(_maxVoltageCellNo).build(),
	// new ElementBuilder().address(0x1431).channel(_maxVoltageCellVoltage).build(),
	// new ElementBuilder().address(0x1432).channel(_maxVoltageCellTemp).signed().build(),
	// new ElementBuilder().address(0x1433).channel(_minVoltageCellNo).build(),
	// new ElementBuilder().address(0x1434).channel(_minVoltageCellVoltage).build(),
	// new ElementBuilder().address(0x1435).channel(_minVoltageCellTemp).signed().build(),
	// new ElementBuilder().address(0x1436).dummy(0x143A - 0x1436).build(),
	// new ElementBuilder().address(0x143A).channel(_maxTempCellNo).build(),
	// new ElementBuilder().address(0x143B).channel(_maxTempCellTemp).signed().build(),
	// new ElementBuilder().address(0x143C).channel(_maxTempCellVoltage).build(),
	// new ElementBuilder().address(0x143D).channel(_minTempCellNo).build(),
	// new ElementBuilder().address(0x143E).channel(_minTempCellTemp).signed().build(),
	// new ElementBuilder().address(0x143F).channel(_minTempCellVoltage).build()), //
	// new ModbusRange(0x1500, new ElementBuilder().address(0x1500).channel(_cell1Voltage).build(),
	// new ElementBuilder().address(0x1501).channel(_cell2Voltage).build(),
	// new ElementBuilder().address(0x1502).channel(_cell3Voltage).build(),
	// new ElementBuilder().address(0x1503).channel(_cell4Voltage).build(),
	// new ElementBuilder().address(0x1504).channel(_cell5Voltage).build(),
	// new ElementBuilder().address(0x1505).channel(_cell6Voltage).build(),
	// new ElementBuilder().address(0x1506).channel(_cell7Voltage).build(),
	// new ElementBuilder().address(0x1507).channel(_cell8Voltage).build(),
	// new ElementBuilder().address(0x1508).channel(_cell9Voltage).build(),
	// new ElementBuilder().address(0x1509).channel(_cell10Voltage).build(),
	// new ElementBuilder().address(0x150a).channel(_cell11Voltage).build(),
	// new ElementBuilder().address(0x150b).channel(_cell12Voltage).build(),
	// new ElementBuilder().address(0x150c).channel(_cell13Voltage).build(),
	// new ElementBuilder().address(0x150d).channel(_cell14Voltage).build(),
	// new ElementBuilder().address(0x150e).channel(_cell15Voltage).build(),
	// new ElementBuilder().address(0x150f).channel(_cell16Voltage).build(),
	// new ElementBuilder().address(0x1510).channel(_cell17Voltage).build(),
	// new ElementBuilder().address(0x1511).channel(_cell18Voltage).build(),
	// new ElementBuilder().address(0x1512).channel(_cell19Voltage).build(),
	// new ElementBuilder().address(0x1513).channel(_cell20Voltage).build(),
	// new ElementBuilder().address(0x1514).channel(_cell21Voltage).build(),
	// new ElementBuilder().address(0x1515).channel(_cell22Voltage).build(),
	// new ElementBuilder().address(0x1516).channel(_cell23Voltage).build(),
	// new ElementBuilder().address(0x1517).channel(_cell24Voltage).build(),
	// new ElementBuilder().address(0x1518).channel(_cell25Voltage).build(),
	// new ElementBuilder().address(0x1519).channel(_cell26Voltage).build(),
	// new ElementBuilder().address(0x151a).channel(_cell27Voltage).build(),
	// new ElementBuilder().address(0x151b).channel(_cell28Voltage).build(),
	// new ElementBuilder().address(0x151c).channel(_cell29Voltage).build(),
	// new ElementBuilder().address(0x151d).channel(_cell30Voltage).build(),
	// new ElementBuilder().address(0x151e).channel(_cell31Voltage).build(),
	// new ElementBuilder().address(0x151f).channel(_cell32Voltage).build(),
	// new ElementBuilder().address(0x1520).channel(_cell33Voltage).build(),
	// new ElementBuilder().address(0x1521).channel(_cell34Voltage).build(),
	// new ElementBuilder().address(0x1522).channel(_cell35Voltage).build(),
	// new ElementBuilder().address(0x1523).channel(_cell36Voltage).build(),
	// new ElementBuilder().address(0x1524).channel(_cell37Voltage).build(),
	// new ElementBuilder().address(0x1525).channel(_cell38Voltage).build(),
	// new ElementBuilder().address(0x1526).channel(_cell39Voltage).build(),
	// new ElementBuilder().address(0x1527).channel(_cell40Voltage).build(),
	// new ElementBuilder().address(0x1528).channel(_cell41Voltage).build(),
	// new ElementBuilder().address(0x1529).channel(_cell42Voltage).build(),
	// new ElementBuilder().address(0x152a).channel(_cell43Voltage).build(),
	// new ElementBuilder().address(0x152b).channel(_cell44Voltage).build(),
	// new ElementBuilder().address(0x152c).channel(_cell45Voltage).build(),
	// new ElementBuilder().address(0x152d).channel(_cell46Voltage).build(),
	// new ElementBuilder().address(0x152e).channel(_cell47Voltage).build(),
	// new ElementBuilder().address(0x152f).channel(_cell48Voltage).build(),
	// new ElementBuilder().address(0x1530).channel(_cell49Voltage).build(),
	// new ElementBuilder().address(0x1531).channel(_cell50Voltage).build(),
	// new ElementBuilder().address(0x1532).channel(_cell51Voltage).build(),
	// new ElementBuilder().address(0x1533).channel(_cell52Voltage).build(),
	// new ElementBuilder().address(0x1534).channel(_cell53Voltage).build(),
	// new ElementBuilder().address(0x1535).channel(_cell54Voltage).build(),
	// new ElementBuilder().address(0x1536).channel(_cell55Voltage).build(),
	// new ElementBuilder().address(0x1537).channel(_cell56Voltage).build(),
	// new ElementBuilder().address(0x1538).channel(_cell57Voltage).build(),
	// new ElementBuilder().address(0x1539).channel(_cell58Voltage).build(),
	// new ElementBuilder().address(0x153a).channel(_cell59Voltage).build(),
	// new ElementBuilder().address(0x153b).channel(_cell60Voltage).build(),
	// new ElementBuilder().address(0x153c).channel(_cell61Voltage).build(),
	// new ElementBuilder().address(0x153d).channel(_cell62Voltage).build(),
	// new ElementBuilder().address(0x153e).channel(_cell63Voltage).build(),
	// new ElementBuilder().address(0x153f).channel(_cell64Voltage).build()),
	// new ModbusRange(0x1700, //
	// new ElementBuilder().address(0x1700).channel(_cell1Temp).build(),
	// new ElementBuilder().address(0x1701).channel(_cell2Temp).build(),
	// new ElementBuilder().address(0x1702).channel(_cell3Temp).build(),
	// new ElementBuilder().address(0x1703).channel(_cell4Temp).build(),
	// new ElementBuilder().address(0x1704).channel(_cell5Temp).build(),
	// new ElementBuilder().address(0x1705).channel(_cell6Temp).build(),
	// new ElementBuilder().address(0x1706).channel(_cell7Temp).build(),
	// new ElementBuilder().address(0x1707).channel(_cell8Temp).build(),
	// new ElementBuilder().address(0x1708).channel(_cell9Temp).build(),
	// new ElementBuilder().address(0x1709).channel(_cell10Temp).build(),
	// new ElementBuilder().address(0x170a).channel(_cell11Temp).build(),
	// new ElementBuilder().address(0x170b).channel(_cell12Temp).build(),
	// new ElementBuilder().address(0x170c).channel(_cell13Temp).build(),
	// new ElementBuilder().address(0x170d).channel(_cell14Temp).build(),
	// new ElementBuilder().address(0x170e).channel(_cell15Temp).build(),
	// new ElementBuilder().address(0x170f).channel(_cell16Temp).build(),
	// new ElementBuilder().address(0x1710).channel(_cell17Temp).build(),
	// new ElementBuilder().address(0x1711).channel(_cell18Temp).build(),
	// new ElementBuilder().address(0x1712).channel(_cell19Temp).build(),
	// new ElementBuilder().address(0x1713).channel(_cell20Temp).build(),
	// new ElementBuilder().address(0x1714).channel(_cell21Temp).build(),
	// new ElementBuilder().address(0x1715).channel(_cell22Temp).build(),
	// new ElementBuilder().address(0x1716).channel(_cell23Temp).build(),
	// new ElementBuilder().address(0x1717).channel(_cell24Temp).build(),
	// new ElementBuilder().address(0x1718).channel(_cell25Temp).build(),
	// new ElementBuilder().address(0x1719).channel(_cell26Temp).build(),
	// new ElementBuilder().address(0x171a).channel(_cell27Temp).build(),
	// new ElementBuilder().address(0x171b).channel(_cell28Temp).build(),
	// new ElementBuilder().address(0x171c).channel(_cell29Temp).build(),
	// new ElementBuilder().address(0x171d).channel(_cell30Temp).build(),
	// new ElementBuilder().address(0x171e).channel(_cell31Temp).build(),
	// new ElementBuilder().address(0x171f).channel(_cell32Temp).build(),
	// new ElementBuilder().address(0x1720).channel(_cell33Temp).build(),
	// new ElementBuilder().address(0x1721).channel(_cell34Temp).build(),
	// new ElementBuilder().address(0x1722).channel(_cell35Temp).build(),
	// new ElementBuilder().address(0x1723).channel(_cell36Temp).build(),
	// new ElementBuilder().address(0x1724).channel(_cell37Temp).build(),
	// new ElementBuilder().address(0x1725).channel(_cell38Temp).build(),
	// new ElementBuilder().address(0x1726).channel(_cell39Temp).build(),
	// new ElementBuilder().address(0x1727).channel(_cell40Temp).build(),
	// new ElementBuilder().address(0x1728).channel(_cell41Temp).build(),
	// new ElementBuilder().address(0x1729).channel(_cell42Temp).build(),
	// new ElementBuilder().address(0x172a).channel(_cell43Temp).build(),
	// new ElementBuilder().address(0x172b).channel(_cell44Temp).build(),
	// new ElementBuilder().address(0x172c).channel(_cell45Temp).build(),
	// new ElementBuilder().address(0x172d).channel(_cell46Temp).build(),
	// new ElementBuilder().address(0x172e).channel(_cell47Temp).build(),
	// new ElementBuilder().address(0x172f).channel(_cell48Temp).build(),
	// new ElementBuilder().address(0x1730).channel(_cell49Temp).build(),
	// new ElementBuilder().address(0x1731).channel(_cell50Temp).build(),
	// new ElementBuilder().address(0x1732).channel(_cell51Temp).build(),
	// new ElementBuilder().address(0x1733).channel(_cell52Temp).build(),
	// new ElementBuilder().address(0x1734).channel(_cell53Temp).build(),
	// new ElementBuilder().address(0x1735).channel(_cell54Temp).build(),
	// new ElementBuilder().address(0x1736).channel(_cell55Temp).build(),
	// new ElementBuilder().address(0x1737).channel(_cell56Temp).build(),
	// new ElementBuilder().address(0x1738).channel(_cell57Temp).build(),
	// new ElementBuilder().address(0x1739).channel(_cell58Temp).build(),
	// new ElementBuilder().address(0x173a).channel(_cell59Temp).build(),
	// new ElementBuilder().address(0x173b).channel(_cell60Temp).build(),
	// new ElementBuilder().address(0x173c).channel(_cell61Temp).build(),
	// new ElementBuilder().address(0x173d).channel(_cell62Temp).build(),
	// new ElementBuilder().address(0x173e).channel(_cell63Temp).build(),
	// new ElementBuilder().address(0x173f).channel(_cell64Temp).build()));
	// }
}
