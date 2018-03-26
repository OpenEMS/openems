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
package io.openems.impl.device.blueplanet50tl3;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.StatusBitChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.core.utilities.power.symmetric.SymmetricPowerImpl;
import io.openems.impl.protocol.modbus.ModbusBitWrappingChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.FloatElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Commercial ESS")
public class FeneconBlueplanet50TL3Ess extends ModbusDeviceNature implements SymmetricEssNature {

	private ThingStateChannels thingState;

	/*
	 * Constructors
	 */
	public FeneconBlueplanet50TL3Ess(String thingId, Device parent) throws ConfigException {
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
	private ModbusReadLongChannel reactivePower;
	private ModbusReadLongChannel allowedCharge;
	private ModbusReadLongChannel allowedDischarge;
	private ModbusReadLongChannel apparentPower;
	private ModbusReadLongChannel gridMode;
	private ModbusReadLongChannel systemState;
	private ModbusWriteLongChannel setActivePower;
	private ModbusWriteLongChannel setReactivePower;
	private ModbusWriteLongChannel setWorkState;
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 50000L)
			.unit("VA");
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 50000L).unit("Wh");
	private SymmetricPowerImpl power;
	// private QGreaterEqualLimitation qMinLimit;
	// private QSmallerEqualLimitation qMaxLimit;
	// private PGreaterEqualLimitation allowedChargeLimit;
	// private PSmallerEqualLimitation allowedDischargeLimit;
	// private SMaxLimitation allowedApparentLimit;

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
	public ModbusReadLongChannel currentSumPhases;
	public ModbusReadLongChannel currentL1;
	public ModbusReadLongChannel currentL2;
	public ModbusReadLongChannel currentL3;
	public ModbusReadLongChannel voltageL1;
	public ModbusReadLongChannel voltageL2;
	public ModbusReadLongChannel voltageL3;
	public ModbusReadLongChannel frequency;
	public ModbusReadLongChannel powerFactor;
	public ModbusReadLongChannel wattHours;
	public ModbusReadLongChannel batteryCurrent;
	public ModbusReadLongChannel batteryVoltage;
	public ModbusReadLongChannel batteryPower;
	public ModbusReadLongChannel cabinetTemperature;
	public ModbusReadLongChannel currentSumPhasesSecond;
	public ModbusReadLongChannel currentL1Second;
	public ModbusReadLongChannel currentL2Second;
	public ModbusReadLongChannel currentL3Second;
	public ModbusReadLongChannel voltageL1Second;
	public ModbusReadLongChannel voltageL2Second;
	public ModbusReadLongChannel voltageL3Second;
	public ModbusReadLongChannel activePowerSecond;
	public ModbusReadLongChannel frequencySecond;
	public ModbusReadLongChannel powerFactorSecond;
	public ModbusReadLongChannel apparentPowerSecond;
	public ModbusReadLongChannel reactivePowerSecond;
	public ModbusReadLongChannel wattHours1;
	public ModbusReadLongChannel batteryCurrentSecond;
	public ModbusReadLongChannel batteryVoltageSecond;
	public ModbusReadLongChannel batteryPowerSecond;
	public ModbusReadLongChannel cabinetTemperatureSecond;
	public ModbusReadLongChannel batteryCurrentThird;
	public ModbusReadLongChannel batteryVoltageThird;
	public ModbusReadLongChannel batteryPowerThird;
	public ModbusReadLongChannel deviceType;
	public ModbusReadLongChannel allowedActivePower;
	public ModbusReadLongChannel vRef;
	public ModbusReadLongChannel pVConn;
	public ModbusReadLongChannel eCPConn;
	public ModbusReadLongChannel tms;
	public ModbusReadLongChannel conn;
	public ModbusReadLongChannel wMaxLimPct;
	public ModbusReadLongChannel wMaxLimPctRvrtTms;
	public ModbusReadLongChannel wMaxLimEna;
	public ModbusReadLongChannel outPFSet;
	public ModbusReadLongChannel outPFSetRvrtTms;
	public ModbusReadLongChannel outPFSetEna;
	public ModbusReadLongChannel vArWMaxPct;
	public ModbusReadLongChannel vArPctRvrtTms;
	public ModbusReadLongChannel vArPctMod;
	public ModbusReadLongChannel vArPctEna;
	public ModbusReadLongChannel actCrv;
	public ModbusReadLongChannel modEna;
	public ModbusReadLongChannel nCrv;
	public ModbusReadLongChannel nPt;
	public ModbusReadLongChannel actCrv1;
	public ModbusReadLongChannel modEna1;
	public ModbusReadLongChannel nCrv1;
	public ModbusReadLongChannel nPt1;
	public ModbusReadLongChannel actCrv2;
	public ModbusReadLongChannel modEna2;
	public ModbusReadLongChannel nCrv2;
	public ModbusReadLongChannel nPt2;
	public ModbusReadLongChannel actCrv3;
	public ModbusReadLongChannel modEna3;
	public ModbusReadLongChannel nCrv3;
	public ModbusReadLongChannel nPt3;
	public ModbusReadLongChannel actCrv4;
	public ModbusReadLongChannel modEna4;
	public ModbusReadLongChannel nCrv4;
	public ModbusReadLongChannel nPt4;
	public ModbusReadLongChannel n;
	public ModbusReadLongChannel verMajor;
	public ModbusReadLongChannel verMinor;
	public ModbusReadLongChannel emsErrCode;
	public ModbusReadLongChannel verMajor1;
	public ModbusReadLongChannel verMinor1;
	public ModbusReadLongChannel psetL1;
	public ModbusReadLongChannel psetL2;
	public ModbusReadLongChannel psetL3;
	public ModbusReadLongChannel qsetL1;
	public ModbusReadLongChannel qsetL2;
	public ModbusReadLongChannel qsetL3;
	public ModbusReadLongChannel uChDC1;
	public ModbusReadLongChannel uDisChDC1;
	public ModbusReadLongChannel iMaxChDC1;
	public ModbusReadLongChannel iMaxDisChDC1;
	public ModbusReadLongChannel uChDC2;
	public ModbusReadLongChannel uDisChDC2;
	public ModbusReadLongChannel iMaxChDC2;
	public ModbusReadLongChannel iMaxDisChDC2;
	public ModbusReadLongChannel transferSP;
	public ModbusReadLongChannel modeLim;
	public ModbusReadLongChannel psetL1Lim;
	public ModbusReadLongChannel psetL2Lim;
	public ModbusReadLongChannel psetL3Lim;
	public ModbusReadLongChannel qsetL1Lim;
	public ModbusReadLongChannel qsetL2Lim;
	public ModbusReadLongChannel qsetL3Lim;
	public ModbusReadLongChannel uChDC1Lim;
	public ModbusReadLongChannel uDisChDC1Lim;
	public ModbusReadLongChannel iMaxChDC1Lim;
	public ModbusReadLongChannel iMaxDisChDC1Lim;
	public ModbusReadLongChannel uChDC2Lim;
	public ModbusReadLongChannel uDisChDC2Lim;
	public ModbusReadLongChannel iMaxChDC2Lim;
	public ModbusReadLongChannel iMaxDisChDC2Lim;
	public ModbusReadLongChannel transferSPLim;
	public ModbusReadLongChannel modeLimLim;
	public ModbusReadLongChannel currentlyPermittedPUStates;
	public ModbusReadLongChannel targetPUState;
	public ModbusReadLongChannel pUState;
	public ModbusReadLongChannel pUEvent;
	public ModbusReadLongChannel actPt;
	public ModbusReadLongChannel deptRef;
	public ModbusReadLongChannel v1;
	public ModbusReadLongChannel v2;
	public ModbusReadLongChannel reactivePowerThird;
	public ModbusReadLongChannel v3;
	public ModbusReadLongChannel reactivePowerFourth;
	public ModbusReadLongChannel v4;
	public ModbusReadLongChannel reactivePowerFifth;
	public ModbusReadLongChannel v5;
	public ModbusReadLongChannel reactivePowerSixth;
	public ModbusReadLongChannel v6;
	public ModbusReadLongChannel reactivePowerSeventh;
	public ModbusReadLongChannel v7;
	public ModbusReadLongChannel reactivePowerEigth;
	public ModbusReadLongChannel v8;
	public ModbusReadLongChannel reactivePowerNineth;
	public ModbusReadLongChannel v9;
	public ModbusReadLongChannel reactivePowerTenth;
	public ModbusReadLongChannel v10;
	public ModbusReadLongChannel reactivePowerEleventh;
	public ModbusReadLongChannel readOnly;
	public ModbusReadLongChannel actPt1;
	public ModbusReadLongChannel tms1;
	public ModbusReadLongChannel v1second;
	public ModbusReadLongChannel tms2;
	public ModbusReadLongChannel v2second;
	public ModbusReadLongChannel readOnly1;
	public ModbusReadLongChannel actPt2;
	public ModbusReadLongChannel tms1second;
	public ModbusReadLongChannel v1third;
	public ModbusReadLongChannel tms2second;
	public ModbusReadLongChannel v2third;
	public ModbusReadLongChannel readOnly2;
	public ModbusReadLongChannel actPt3;
	public ModbusReadLongChannel tms1third;
	public ModbusReadLongChannel hz1second;
	public ModbusReadLongChannel tms2third;
	public ModbusReadLongChannel hz2second;
	public ModbusReadLongChannel readOnly3;
	public ModbusReadLongChannel actPt4;
	public ModbusReadLongChannel tms1fourth;
	public ModbusReadLongChannel hz1third;
	public ModbusReadLongChannel tms2fourth;
	public ModbusReadLongChannel hz2third;
	public ModbusReadLongChannel readOnly4;
	public ModbusReadLongChannel iD;

	public ModbusReadLongChannel tms3;
	public ModbusReadLongChannel tmp;
	public ModbusReadLongChannel batID;
	public ModbusReadLongChannel soh;
	public ModbusReadLongChannel batteryTemperature;
	public ModbusReadLongChannel allowedApparent;
	public StatusBitChannel abnormity1;
	public StatusBitChannel abnormity2;


	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //

				// Device ID 103
				new ModbusRegisterRange(40072,
						new UnsignedWordElement(40072,
								currentSumPhases = new ModbusReadLongChannel("SumOfActivePhases", this).unit("A")
								.multiplier(-2)), //
						new UnsignedWordElement(40073,
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("A").multiplier(-2)), //
						new UnsignedWordElement(40074,
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("A").multiplier(-2)), //
						new UnsignedWordElement(40075,
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("A").multiplier(-2)), //
						new DummyElement(40076, 40079), //
						new UnsignedWordElement(40080,
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("V").multiplier(-1)), //
						new UnsignedWordElement(40081,
								voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("V").multiplier(-1)), //
						new UnsignedWordElement(40082,
								voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("V").multiplier(-1)), //
						new DummyElement(40083), //
						new SignedWordElement(40084,
								activePower = new ModbusReadLongChannel("TotalACPower", this).unit("W").multiplier(1)), //
						new DummyElement(40085), //
						new UnsignedWordElement(40086,
								frequency = new ModbusReadLongChannel("Frequency", this).unit("Hz").multiplier(-1)), //
						new DummyElement(40087), //
						new SignedWordElement(40088,
								apparentPower = new ModbusReadLongChannel("ApparentPower", this).unit("VA")
								.multiplier(1)), //
						new DummyElement(40089), //
						new SignedWordElement(40090,
								reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("var")
								.multiplier(1)), //
						new DummyElement(40091), //
						new SignedWordElement(40092,
								powerFactor = new ModbusReadLongChannel("PowerFactor", this).unit("Pct")
								.multiplier(-1)), //
						new DummyElement(40093), //
						new UnsignedDoublewordElement(40094,
								wattHours = new ModbusReadLongChannel("ACEnergy", this).unit("Wh").multiplier(0)), //
						new DummyElement(40096), //
						new UnsignedWordElement(40097,
								batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this).unit("A")
								.multiplier(-2)), //
						new DummyElement(40098), //
						new UnsignedWordElement(40099,
								batteryVoltage = new ModbusReadLongChannel("BatteryVoltage", this).unit("V")
								.multiplier(-1)), //
						new DummyElement(40100), //
						new SignedWordElement(40101,
								batteryPower = new ModbusReadLongChannel("BatteryPower", this).unit("W")
								.multiplier(-1)), //
						new DummyElement(40102), //
						new SignedWordElement(40103,
								cabinetTemperature = new ModbusReadLongChannel("CabinetTemperature", this).unit("C")
								.multiplier(-1)), //
						new DummyElement(40104, 40107)//
						// TODO NEED to add the Vendor Operating State "40109" KACO Powador-proLOG Status Description in [3]
						// which is;
						// Overview_proLOG_Status_KACO_Inverter_date_en.pdf
						), //
				new ModbusRegisterRange(40108, //
						new UnsignedWordElement(40108, //
								systemState = new ModbusReadLongChannel("SystemState", this) //
								.label(2, "OFF") //
								.label(4, "Sleeping") //
								.label(8, "Starting") //
								.label(16, "MPPT") //
								.label(32, "Throttled")//
								.label(64, "ShuttingDown")//
								.label(128, "FAULT")//
								.label(256, "STANDBY")//
								), //
						new DummyElement(40109)), //

				new ModbusRegisterRange(40110, //
						new UnsignedWordElement(40110, //
								new ModbusBitWrappingChannel("Abnormity1", this, this.thingState)//
								.faultBit(0, FaultEss.GroundFault)//
								.faultBit(1, FaultEss.DCOverVolt)//
								.faultBit(2, FaultEss.ACDisconnect)//
								.faultBit(3, FaultEss.DCDisconnect)//
								.faultBit(4, FaultEss.GridDisconnect)//
								.faultBit(5, FaultEss.CabinetOpen)//
								.faultBit(6, FaultEss.ManualShutdown)//
								.faultBit(7, FaultEss.OverTemp)//
								.faultBit(8, FaultEss.OverFrequency)//
								.faultBit(9, FaultEss.UnderFrequency)//
								.faultBit(10, FaultEss.ACOverVolt)//
								.faultBit(11, FaultEss.ACUnderVolt)//
								.faultBit(12, FaultEss.BlownStringFuse)//
								.faultBit(13, FaultEss.UnderTemp)//
								.faultBit(14, FaultEss.MemoryLoss)//
								.faultBit(15, FaultEss.HWTestFailure)//
								), //
						new DummyElement(40111, 40120)), //
				// Device ID 113
				new ModbusRegisterRange(40124, //
						new FloatElement(40124,
								currentSumPhasesSecond = new ModbusReadLongChannel("CurrentSumPhasesSecond", this)
								.unit("A")), //
						new FloatElement(40126,
								currentL1Second = new ModbusReadLongChannel("currentL1Second", this).unit("A")), //
						new FloatElement(40128,
								currentL2Second = new ModbusReadLongChannel("currentL2Second", this).unit("A")), //
						new FloatElement(40130,
								currentL3Second = new ModbusReadLongChannel("currentL3Second", this).unit("A")), //
						new DummyElement(40132, 40137), //
						new FloatElement(40138,
								voltageL1Second = new ModbusReadLongChannel("voltageL1Second", this).unit("V")), //
						new FloatElement(40140,
								voltageL2Second = new ModbusReadLongChannel("voltageL2Second", this).unit("V")), //
						new FloatElement(40142,
								voltageL3Second = new ModbusReadLongChannel("voltageL3Second", this).unit("V")), //
						new FloatElement(40144,
								activePowerSecond = new ModbusReadLongChannel("TotalACPowerSecond", this).unit("W")), //
						new FloatElement(40146,
								frequencySecond = new ModbusReadLongChannel("LineFrequencySecond", this).unit("Hz")), //
						new FloatElement(40148,
								apparentPowerSecond = new ModbusReadLongChannel("ApparentPowerSecond", this)
								.unit("VA")), //
						new FloatElement(40150,
								reactivePowerSecond = new ModbusReadLongChannel("ACReactivePowerSecond", this)
								.unit("var")), //
						new FloatElement(40152,
								powerFactorSecond = new ModbusReadLongChannel("ACPowerFactorSecond", this).unit("Pct")), //
						new FloatElement(40154, wattHours1 = new ModbusReadLongChannel("ACEnergy1", this).unit("Wh")), //
						new FloatElement(40156,
								batteryCurrentSecond = new ModbusReadLongChannel("BatteryCurrentSecond", this)
								.unit("A")), //
						new FloatElement(40158,
								batteryVoltageSecond = new ModbusReadLongChannel("BatteryVoltageSecond", this)
								.unit("V")), //
						new FloatElement(40160,
								batteryPowerSecond = new ModbusReadLongChannel("BatteryPowerSecond", this).unit("W")), //
						new FloatElement(40162,
								cabinetTemperatureSecond = new ModbusReadLongChannel("CabinetTemperatureSecond", this)
								.unit("C")), //
						new DummyElement(40164, 40168)//
						), //

				new ModbusRegisterRange(40170, //
						new UnsignedWordElement(40170, //
								systemState = new ModbusReadLongChannel("SystemState", this) //
								.label(2, "OFF") //
								.label(4, "Sleeping") //
								.label(8, "Starting") //
								.label(16, "MPPT") //
								.label(32, "Throttled")//
								.label(64, "ShuttingDown")//
								.label(128, "FAULT")//
								.label(256, "STANDBY")//
								)), //

				new ModbusRegisterRange(40172, //
						new UnsignedWordElement(40172, //
								new ModbusBitWrappingChannel("Abnormity2", this, this.thingState)//
								.faultBit(0, FaultEss.GroundFault1)//
								.faultBit(1, FaultEss.DCOverVolt1)//
								.faultBit(2, FaultEss.ACDisconnect1)//
								.faultBit(3, FaultEss.DCDisconnect1)//
								.faultBit(4, FaultEss.GridDisconnect1)//
								.faultBit(5, FaultEss.CabinetOpen1)//
								.faultBit(6, FaultEss.ManualShutdown1)//
								.faultBit(7, FaultEss.OverTemp1)//
								.faultBit(8, FaultEss.OverFrequency1)//
								.faultBit(9, FaultEss.UnderFrequency1)//
								.faultBit(10, FaultEss.ACOverVolt1)//
								.faultBit(11, FaultEss.ACUnderVolt1)//
								.faultBit(12, FaultEss.BlownStringFuse1)//
								.faultBit(13, FaultEss.UnderTemp1)//
								.faultBit(14, FaultEss.MemoryLoss1)//
								.faultBit(15, FaultEss.HWTestFailure1)//
								)), //

				// Device ID 120
				new ModbusRegisterRange(40186, //
						new UnsignedWordElement(40186, //
								deviceType = new ModbusReadLongChannel("DeviceType", this)//
								.label(16, "PVDevice")//
								// TODO DERTyp(Device120) Chapter 5.20 PV_STOR (" PV Storage Device") Value used
								// 8 instead of 82
								.label(256, "PVStorageDevice")//
								), //
						new UnsignedWordElement(40187,
								allowedActivePower = new ModbusReadLongChannel("AllowedActivePower", this).unit("W")
								.multiplier(1)), //
						new DummyElement(40188), //
						new UnsignedWordElement(40189,
								allowedApparent = new ModbusReadLongChannel("AllowedApparent", this).unit("VA")
								.multiplier(1)), //
						new DummyElement(40190, 40211)//
						// TODO Address Offset 27 Pad Register (?) need to be add and IF you add it, please make the
						// dummyElement range between (40191,40210)
						), //

				// Device ID 121
				new ModbusRegisterRange(40214, //
						new DummyElement(40214), //
						new UnsignedWordElement(40215,
								vRef = new ModbusReadLongChannel("VoltageAtThePCC", this).unit("V").multiplier(-1)),
						new DummyElement(40216, 40243)), //

				// Device ID 122
				new ModbusRegisterRange(40246, //
						new UnsignedWordElement(40246,
								pVConn = new ModbusReadLongChannel("PVInverterPresentAvailableStatus", this)//
								.label(1, "Connected")//
								.label(2, "Available")//
								.label(4, "Operating")//
								.label(8, "Test")),//

						new DummyElement(40247),//
						new UnsignedWordElement(40248, eCPConn = new ModbusReadLongChannel("ECPConnectionStatus", this)//
						.label(1, "Connected")), //

						new DummyElement(40249, 40284), //
						// TODO "String type elements" with size:4 (Source of time synchronization) I could not add it
						new UnsignedDoublewordElement(40285,
								tms = new ModbusReadLongChannel("SecondSince01012000And0000UTC", this).unit("secs")), //
						new DummyElement(40287, 40291)), //

				// Device ID 123
				new ModbusRegisterRange(40292, //
						new DummyElement(40292, 40293), //
						new UnsignedWordElement(40294, conn = new ModbusReadLongChannel("ConnectionControl", this)//
						.label(1, "Disconnected")//
						.label(2, "Connected")), //
						new UnsignedWordElement(40295, //
								wMaxLimPct = new ModbusReadLongChannel("SetPowerOutputToSpecificLevel", this)
								.unit("%WMax").multiplier(0)), //
						new DummyElement(40296), //
						new UnsignedWordElement(40297, //
								wMaxLimPctRvrtTms = new ModbusReadLongChannel("TimeoutPeriodForPowerLimit", this)
								.unit("secs")), //
						new DummyElement(40298), //
						new UnsignedWordElement(40299, //
								wMaxLimEna = new ModbusReadLongChannel("ThrottleEnableOrDisableControl", this)//
								.label(1, "Disabled")//
								.label(2, "Enabled")), //
						new SignedWordElement(40300,
								outPFSet = new ModbusReadLongChannel("SetPowerFactorToSpecifiedValueCosineOfAngle",
										this).unit("cos()").multiplier(-3)), //
						new DummyElement(40301), //
						new UnsignedWordElement(40302,
								outPFSetRvrtTms = new ModbusReadLongChannel("TimeoutPeriodForPowerFactor", this)
								.unit("secs")), //
						new DummyElement(40303), //
						new UnsignedWordElement(40304,
								outPFSetEna = new ModbusReadLongChannel("FixedPowerFactorEnableOrDisableControl", this)//
								.label(1, "Disabled")//
								.label(2, "Enabled")), //
						new SignedWordElement(40305,
								vArWMaxPct = new ModbusReadLongChannel("ReactivePowerInPercentOfWMax", this)
								.unit("%WMax").multiplier(-1)), //
						new DummyElement(40306, 40308), //
						new UnsignedWordElement(40309,
								vArPctRvrtTms = new ModbusReadLongChannel("TimeoutPeriodForVArLimit", this)
								.unit("secs")), //
						new DummyElement(40310), //
						// TODO Register Number 40311 needs check again; Enum value only 1=WMax
						new UnsignedWordElement(40311,
								vArPctMod = new ModbusReadLongChannel("VArPercentLimitMode", this)//
								.label(2, "WMax")), //
						new UnsignedWordElement(40312,
								vArPctEna = new ModbusReadLongChannel("PercentLimitVArEnableOrDisableControl", this)//
								.label(1, "Disabled")//
								.label(2, "Enabled")), //
						new DummyElement(40313, 40315)), //

				// Device ID 126
				new ModbusRegisterRange(40318, //
						new UnsignedWordElement(40318,
								actCrv = new ModbusReadLongChannel("IndexOfActiveCurveAnd0isNoActiveCurve", this)), //
						new UnsignedWordElement(40319,
								modEna = new ModbusReadLongChannel("IsVoltVARControlActive", this)//
								.label(1, "Enabled")), //
						new DummyElement(40320, 40322), //
						new UnsignedWordElement(40323,
								nCrv = new ModbusReadLongChannel("NumberOfCurvesSupportedRecommend4", this)), //
						new UnsignedWordElement(40324,
								nPt = new ModbusReadLongChannel("NumberOfCurvesSupportedMaximumOf20", this)), //
						new DummyElement(40325, 40327), //
						new UnsignedWordElement(40328,
								actPt = new ModbusReadLongChannel("NumberOfActiveintsInArray", this)), //
						// TODO register type enum16, 1=%WMax
						new UnsignedWordElement(40329,
								deptRef = new ModbusReadLongChannel("MeaningOfDependentVariable", this).multiplier(-1)//
								.label(2, "WMax")), //
						// TODO Rest of the registers which are in the grey part of the table that indicates the
						// repeating block parts.
						new UnsignedWordElement(40330,
								v1 = new ModbusReadLongChannel("Point1Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40331,
								reactivePowerSecond = new ModbusReadLongChannel("Point1VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40332,
								v2 = new ModbusReadLongChannel("Point2Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40333,
								reactivePowerThird = new ModbusReadLongChannel("Point2VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40334,
								v3 = new ModbusReadLongChannel("Point3Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40335,
								reactivePowerFourth = new ModbusReadLongChannel("Point3VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40336,
								v4 = new ModbusReadLongChannel("Point4Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40337,
								reactivePowerFifth = new ModbusReadLongChannel("Point4VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40338,
								v5 = new ModbusReadLongChannel("Point5Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40339,
								reactivePowerSixth = new ModbusReadLongChannel("Point5VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40340,
								v6 = new ModbusReadLongChannel("Point6Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40341,
								reactivePowerSeventh = new ModbusReadLongChannel("Point6VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40342,
								v7 = new ModbusReadLongChannel("Point7Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40343,
								reactivePowerEigth = new ModbusReadLongChannel("Point7VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40344,
								v8 = new ModbusReadLongChannel("Point8Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40345,
								reactivePowerNineth = new ModbusReadLongChannel("Point8VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40346,
								v9 = new ModbusReadLongChannel("Point9Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40347,
								reactivePowerTenth = new ModbusReadLongChannel("Point9VARs", this).multiplier(-1)), //
						new UnsignedWordElement(40348,
								v10 = new ModbusReadLongChannel("Point10Volts", this).unit("%VRef").multiplier(-1)), //
						new SignedWordElement(40349,
								reactivePowerEleventh = new ModbusReadLongChannel("Point10VARs", this).multiplier(-1)), //
						new DummyElement(40350, 40373), //
						// TODO 40370 is CrvNam register type is string did not add
						new UnsignedWordElement(40374,
								readOnly = new ModbusReadLongChannel(
										"BooleanFlagIndicatesIfCurveIsReadOnlyOrCanBeModified", this)//
								.label(1, "ReadWrite")//
								.label(2, "ReadOnly"))//
						), //

				// Device ID 129
				// 40555 pad did not add
				new ModbusRegisterRange(40546, //
						new UnsignedWordElement(40546, actCrv1 = new ModbusReadLongChannel("IndexOfActiveCurve", this)), //
						new UnsignedWordElement(40547,
								modEna1 = new ModbusReadLongChannel("LVRTDControlModeEnableActivecURVE", this)//
								.label(1, "Enabled")), //
						new DummyElement(40548, 40550), //
						new UnsignedWordElement(40551,
								nCrv1 = new ModbusReadLongChannel("NumberOfCurvesSupported", this)), //
						new UnsignedWordElement(40552,
								nPt1 = new ModbusReadLongChannel("NumberOfCurvesSupportedMaximum20", this)), //
						new DummyElement(40553, 40555), //
						// TODO Rest of the registers which are in the grey part of the table that indicates the
						// repeating block parts.
						new UnsignedWordElement(40556,
								actPt1 = new ModbusReadLongChannel("NumberOfActivePointsInArray", this)), //
						new UnsignedWordElement(40557,
								tms1 = new ModbusReadLongChannel("Point1MustDisconnectedDuration", this).unit("secs")
								.multiplier(-3)),
						new UnsignedWordElement(40558,
								v1second = new ModbusReadLongChannel("Point1MustDisconnectedVoltage", this)
								.unit("%VRef").multiplier(-1)),
						new UnsignedWordElement(40559,
								tms2 = new ModbusReadLongChannel("Point1MustDisconnectedDuration", this).unit("secs")
								.multiplier(-3)),
						new UnsignedWordElement(40560,
								v2second = new ModbusReadLongChannel("Point1MustDisconnectedVoltage", this)
								.unit("%VRef").multiplier(-1)),
						new DummyElement(40561, 40604), //
						// TODO 40597 CrvNam register type string did not add
						new UnsignedWordElement(40605,
								readOnly1 = new ModbusReadLongChannel("EnumeratedValueIndicatesIfCurveIsReadOnly", this)//
								.label(1, "ReadWrite")//
								.label(2, "ReadOnly"))//
						), //
				// Device ID 130
				new ModbusRegisterRange(40608, //
						new UnsignedWordElement(40608, actCrv2 = new ModbusReadLongChannel("IndexOfActiveCurve", this)), //
						new UnsignedWordElement(40609,
								modEna2 = new ModbusReadLongChannel("HVRTDControlModeEnableActivecURVE", this)//
								.label(1, "Enabled")), //
						new DummyElement(40610, 40612), //
						new UnsignedWordElement(40613,
								nCrv2 = new ModbusReadLongChannel("NumberOfCurvesSupported", this)), //
						new UnsignedWordElement(40614,
								nPt2 = new ModbusReadLongChannel("NumberOfCurvesSupportedMaximum20", this)), //
						new DummyElement(40615, 40617), //
						// TODO Rest of the registers did not add which are in the grey part of the table that indicates
						// the
						// repeating block parts
						new UnsignedWordElement(40618,
								actPt2 = new ModbusReadLongChannel("NumberOfActivePointsInArray", this)), //
						new UnsignedWordElement(40619,
								tms1second = new ModbusReadLongChannel("Point1MustDisconnectedDuration", this)
								.unit("secs").multiplier(-3)), //
						new UnsignedWordElement(40620,
								v1third = new ModbusReadLongChannel("Point1MustDisconnectedVoltage", this).unit("%VRef")
								.multiplier(-1)), //
						new UnsignedWordElement(40621,
								tms2second = new ModbusReadLongChannel("Point2MustDisconnectedDuration", this)
								.unit("secs").multiplier(-3)), //
						new UnsignedWordElement(40622,
								v2third = new ModbusReadLongChannel("Point2MustDisconnectedVoltage", this).unit("%VRef")
								.multiplier(-1)), //
						new DummyElement(40623, 40661), //
						// TODO 40654 CrvNam register type string did not add
						new UnsignedWordElement(40662,
								readOnly2 = new ModbusReadLongChannel("EnumeratedValueIndicatesIfCurveIsReadOnly", this)//
								.label(1, "ReadWrite")//
								.label(2, "ReadOnly"))//

						),
				// Device ID 135
				new ModbusRegisterRange(40670, //
						new UnsignedWordElement(40670, actCrv3 = new ModbusReadLongChannel("IndexOfActiveCurve", this)), //
						new UnsignedWordElement(40671,
								modEna3 = new ModbusReadLongChannel("LFRTDControlModeEnableActivecURVE", this)//
								.label(1, "Enabled")), //
						new DummyElement(40672, 40674), //
						new UnsignedWordElement(40675,
								nCrv3 = new ModbusReadLongChannel("NumberOfCurvesSupported", this)), //
						new UnsignedWordElement(40676,
								nPt3 = new ModbusReadLongChannel("NumberOfCurvesSupportedMaximum20", this)), //
						new DummyElement(40677, 40679), //
						// TODO Rest of the registers which are in the grey part of the table that indicates
						// the
						// repeating block parts
						new UnsignedWordElement(40680,
								actPt3 = new ModbusReadLongChannel("NumberOfActivePointsInArray", this)), //
						new UnsignedWordElement(40681,
								tms1third = new ModbusReadLongChannel("Point1MustDisconnectedDuration", this)
								.unit("secs").multiplier(-3)), //
						new UnsignedWordElement(40682,
								hz1second = new ModbusReadLongChannel("Point1MustDisconnectFrequency", this).unit("Hz")
								.multiplier(-3)), //
						new UnsignedWordElement(40683,
								tms2third = new ModbusReadLongChannel("Point2MustDisconnectedDuration", this)
								.unit("secs").multiplier(-3)), //
						new UnsignedWordElement(40684,
								hz2second = new ModbusReadLongChannel("Point2MustDisconnectFrequency", this).unit("Hz")
								.multiplier(-3)), //
						new DummyElement(40685, 40728), //
						new UnsignedWordElement(40729,
								readOnly3 = new ModbusReadLongChannel("EnumeratedValueIndicatesIfCurveIsReadOnly", this)//
								.label(1, "ReadWrite")//
								.label(2, "ReadOnly"))//
						), //
				// Device ID 136
				new ModbusRegisterRange(40732, //
						new UnsignedWordElement(40732, actCrv4 = new ModbusReadLongChannel("IndexOfActiveCurve", this)), //
						new UnsignedWordElement(40733,
								modEna4 = new ModbusReadLongChannel("LFRTDControlModeEnableActivecURVE", this)//
								.label(1, "Enabled")), //
						new DummyElement(40734, 40736), //
						new UnsignedWordElement(40737,
								nCrv4 = new ModbusReadLongChannel("NumberOfCurvesSupported", this)), //
						new UnsignedWordElement(40738,
								nPt4 = new ModbusReadLongChannel("NumberOfCurvesSupportedMaximum20", this)), //
						new DummyElement(40739, 40741), //
						// TODO Rest of the registers which are in the grey part of the table that indicates the
						// repeating block parts
						new UnsignedWordElement(40742,
								actPt4 = new ModbusReadLongChannel("NumberOfActivePointsInArray", this)), //
						new UnsignedWordElement(40743,
								tms1fourth = new ModbusReadLongChannel("Point1MustDisconnectedDuration", this)
								.unit("secs").multiplier(-3)), //
						new UnsignedWordElement(40744,
								hz1third = new ModbusReadLongChannel("Point1MustDisconnectFrequency", this).unit("Hz")
								.multiplier(-3)), //
						new UnsignedWordElement(40745,
								tms2fourth = new ModbusReadLongChannel("Point2MustDisconnectedDuration", this)
								.unit("secs").multiplier(-3)), //
						new UnsignedWordElement(40746,
								hz2third = new ModbusReadLongChannel("Point2MustDisconnectFrequency", this).unit("Hz")
								.multiplier(-3)), //
						new DummyElement(40747, 40788), //
						new UnsignedWordElement(40789,
								readOnly4 = new ModbusReadLongChannel("EnumeratedValueIndicatesIfCurveIsReadOnly", this)//
								.label(1, "ReadWrite")//
								.label(2, "ReadOnly"))//
						), // )
				// Device ID160
				new ModbusRegisterRange(40794, //
						new DummyElement(40794, 40797), //
						new UnsignedDoublewordElement(40798, //
								new ModbusBitWrappingChannel("Abnormity2", this, this.thingState)//
								.faultBit(0, FaultEss.GroundFault2)//
								.faultBit(1, FaultEss.InputOverVoltage)//
								.faultBit(3, FaultEss.DCDisconnect2)//
								.faultBit(5, FaultEss.CabinetOpen2)//
								.faultBit(6, FaultEss.ManualShutdown2)//
								.faultBit(7, FaultEss.OverTemp2)//
								.faultBit(12, FaultEss.BlownFuse)//
								.faultBit(13, FaultEss.UnderTemp2)//
								.faultBit(14, FaultEss.MemoryLoss2)//
								.faultBit(15, FaultEss.ArcDetection)//
								.faultBit(20, FaultEss.TestFailed)//
								.faultBit(21, FaultEss.InputUnderVoltage)//
								.faultBit(22, FaultEss.InputOverCurrent)), //
						// TODO 40800 register type needs to check and there is no information about register 40799
						new UnsignedWordElement(40800, n = new ModbusReadLongChannel("NumberOfModules", this)), //
						new DummyElement(40801), //
						// TODO Rest of the registers which are in the grey part of the table that indicates the
						// repeating block parts
						new UnsignedWordElement(40802, iD = new ModbusReadLongChannel("InputID", this)), //
						// TODO 40803 register type Input ID string
						new DummyElement(40803, 40810), //
						new UnsignedWordElement(40811,
								batteryCurrentThird = new ModbusReadLongChannel("BatteryCurrentThird", this).unit("A")
								.multiplier(-2)), //
						new UnsignedWordElement(40812,
								batteryVoltageThird = new ModbusReadLongChannel("BatteryVoltageThird", this).unit("V")
								.multiplier(-1)), //
						new UnsignedWordElement(40813,
								batteryPowerThird = new ModbusReadLongChannel("BatteryPowerThird", this).unit("W")
								.multiplier(1)), //
						new DummyElement(40814), //
						new UnsignedWordElement(40815,
								tms3 = new ModbusReadLongChannel("TimeStamp", this).unit("secs")), //
						new UnsignedWordElement(40816,
								tmp = new ModbusReadLongChannel("Temperature", this).unit("C").multiplier(0)), //
						new DummyElement(40817, 40820)//

						), //

				// Device ID 64201
				// TODO needs to check for bit values of emsErrorCode, labels was not added
				new ModbusRegisterRange(40824, //
						new UnsignedWordElement(40824,
								verMajor = new ModbusReadLongChannel("VersionInformationOfTheModel", this)), //
						new UnsignedWordElement(40825,
								verMinor = new ModbusReadLongChannel("VersionInformationOfTheModel", this)), //
						new UnsignedWordElement(40826,
								emsErrCode = new ModbusReadLongChannel("ErrorcodeFromEMS", this)), //
						// TODO reserved Pad values between 40827-40834 and SF for 40835-40837
						new DummyElement(40827, 40838), //
						// TODO 40838 register type string
						// TODO Rest of the registers which are in the grey part of the table that indicates the
						// repeating block parts
						new UnsignedWordElement(40839,
								batID = new ModbusReadLongChannel("SerialOfBatteryOrID", this).unit("%").multiplier(0)), //
						new UnsignedWordElement(40840,
								soc = new ModbusReadLongChannel("SoC", this).unit("%").multiplier(0)), //
						new UnsignedWordElement(40841,
								soh = new ModbusReadLongChannel("Soh", this).unit("%").multiplier(0)), //
						new SignedWordElement(40842,
								batteryTemperature = new ModbusReadLongChannel("AvgTemperatureOfBattery", this)
								.unit("C").multiplier(0)), //
						// TODO 408443-40844 registers type string and last one is pad
						new DummyElement(40843, 40851)//

						),

				// Device ID 64202
				new ModbusRegisterRange(40856, //
						new UnsignedWordElement(40856,
								verMajor1 = new ModbusReadLongChannel("VersionInformationOfTheModel", this)), //
						new UnsignedWordElement(40857,
								verMinor1 = new ModbusReadLongChannel("VersionInformationOfTheModel", this)), //
						// TODO there is no information about FeatureSet and Mode
						// new UnsignedDoublewordElement(40858, new ModbusBitWrappingChannel("Abnormity3", this,
						// this.thingState)),//
						// new UnsignedWordElement(40860, mode = new ModbusReadLongChannel("Mode", this),//
						new DummyElement(40858, 40860), //
						// TODO Needs to find values of All Scale factors; are not defined
						new SignedWordElement(40861, psetL1 = new ModbusReadLongChannel("PsetL1", this).unit("W")), //
						new SignedWordElement(40862, psetL2 = new ModbusReadLongChannel("PsetL2", this).unit("W")), //
						new SignedWordElement(40863, psetL3 = new ModbusReadLongChannel("PsetL3", this).unit("W")), //
						new SignedWordElement(40864, qsetL1 = new ModbusReadLongChannel("QsetL1", this).unit("var")), //
						new SignedWordElement(40865, qsetL2 = new ModbusReadLongChannel("QsetL2", this).unit("var")), //
						new SignedWordElement(40866, qsetL3 = new ModbusReadLongChannel("QsetL3", this).unit("var")), //
						new UnsignedWordElement(40867,
								uChDC1 = new ModbusReadLongChannel("MaximumChargeVoltageOfDC1", this).unit("V")), //
						new UnsignedWordElement(40868,
								uDisChDC1 = new ModbusReadLongChannel("MinimumDischargeVoltageOfDC1", this).unit("V")), //
						new SignedWordElement(40869,
								iMaxChDC1 = new ModbusReadLongChannel("MaximumChargeCurrentOfDC1", this).unit("A")), //
						new SignedWordElement(40870,
								iMaxDisChDC1 = new ModbusReadLongChannel("MaximumDischargeCurrentOfDC1", this)
								.unit("A")), //
						new UnsignedWordElement(40871,
								uChDC2 = new ModbusReadLongChannel("MaximumChargeVoltageOfDC2", this).unit("V")), //
						new UnsignedWordElement(40872,
								uDisChDC2 = new ModbusReadLongChannel("MinimumDischargeVoltageOfDC2", this).unit("V")), //
						new SignedWordElement(40873,
								iMaxChDC2 = new ModbusReadLongChannel("MaximumChargeCurrentOfDC2", this).unit("A")), //
						new SignedWordElement(40874,
								iMaxDisChDC2 = new ModbusReadLongChannel("MaximumDischargeCurrentOfDC2", this)
								.unit("A")), //
						new UnsignedWordElement(40875,
								transferSP = new ModbusReadLongChannel("TransferNewSetpointToDSP", this)), //
						new UnsignedWordElement(40876,
								modeLim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)), //
						new SignedWordElement(40877,
								psetL1Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this).unit("W")), //
						new SignedWordElement(40878,
								psetL2Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this).unit("W")), //
						new SignedWordElement(40879,
								psetL3Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this).unit("W")), //
						new SignedWordElement(40880,
								qsetL1Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("var")), //
						new SignedWordElement(40881,
								qsetL2Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("var")), //
						new SignedWordElement(40882,
								qsetL3Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("var")), //
						new UnsignedWordElement(40883,
								uChDC1Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this).unit("V")), //
						new UnsignedWordElement(40884,
								uDisChDC1Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("V")), //
						new SignedWordElement(40885,
								iMaxChDC1Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("A")), //
						new SignedWordElement(40886,
								iMaxDisChDC1Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("A")), //
						new UnsignedWordElement(40887,
								uChDC2Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this).unit("V")), //
						new UnsignedWordElement(40888,
								uDisChDC2Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("V")), //
						new SignedWordElement(40889,
								iMaxChDC2Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("A")), //
						new SignedWordElement(40890,
								iMaxDisChDC2Lim = new ModbusReadLongChannel("ReceivedFromDSPLimitedValues", this)
								.unit("A")), //
						new DummyElement(40891, 40895), //
						new UnsignedWordElement(40896,
								currentlyPermittedPUStates = new ModbusReadLongChannel(
										"CurrentlyPermittedPUStatesBitPattern", this)), //
						new UnsignedWordElement(40897,
								targetPUState = new ModbusReadLongChannel("DesiredTargetPUStateInputtedFromEMS", this)), //
						// TODO PU_State Pu_Event registers type; enum16
						new UnsignedWordElement(40898,
								pUState = new ModbusReadLongChannel("CurrentStateOfInverter", this)), //
						new UnsignedWordElement(40899,
								pUEvent = new ModbusReadLongChannel("NewEventToControlInverterPowerunit", this)), //
						new DummyElement(40900, 40903)//
						));

		this.power = new SymmetricPowerImpl(50000, setActivePower /* TODO */, setReactivePower /* TODO */,
				getParent().getBridge());
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
