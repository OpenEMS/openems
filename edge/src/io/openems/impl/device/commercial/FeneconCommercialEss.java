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
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.core.utilities.power.MaxCosPhiLimitation;
import io.openems.core.utilities.power.PGreaterEqualLimitation;
import io.openems.core.utilities.power.PSmallerEqualLimitation;
import io.openems.core.utilities.power.SMaxLimitation;
import io.openems.core.utilities.power.SymmetricPowerImpl;
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
	private SymmetricPowerImpl power;
	private MaxCosPhiLimitation cosPhiLimit;
	private PGreaterEqualLimitation allowedChargeLimit;
	private PSmallerEqualLimitation allowedDischargeLimit;
	private SMaxLimitation allowedApparentLimit;
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
		this.power = new SymmetricPowerImpl(40000, setActivePower, setReactivePower);
		this.cosPhiLimit  = new MaxCosPhiLimitation(power);
		this.cosPhiLimit.setMaxCosPhi(0.8);
		this.power.addStaticLimitation(cosPhiLimit);
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
		return protocol;
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public SymmetricPowerImpl getPower() {
		return power;
	}

}
