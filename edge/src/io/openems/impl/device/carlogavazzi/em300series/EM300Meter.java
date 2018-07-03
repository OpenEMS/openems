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
package io.openems.impl.device.carlogavazzi.em300series;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.SignedWordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.range.ModbusInputRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "Socomec Meter")
public class EM300Meter extends ModbusDeviceNature implements SymmetricMeterNature, AsymmetricMeterNature {

	private ThingStateChannels thingState;

	/*
	 * Constructors
	 */
	public EM300Meter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		this.thingState = new ThingStateChannels(this);
	}

	/*
	 * Config
	 */
	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this);

	@Override
	public ConfigChannel<String> type() {
		return type;
	}

	private final ConfigChannel<Long> maxActivePower = new ConfigChannel<Long>("maxActivePower", this);

	@Override
	public ConfigChannel<Long> maxActivePower() {
		return maxActivePower;
	}

	private final ConfigChannel<Long> minActivePower = new ConfigChannel<Long>("minActivePower", this);

	@Override
	public ConfigChannel<Long> minActivePower() {
		return minActivePower;
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadLongChannel activePower;
	private ModbusReadLongChannel apparentPower;
	private ModbusReadLongChannel reactivePower;
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel reactivePowerL1;
	private ModbusReadLongChannel reactivePowerL2;
	private ModbusReadLongChannel reactivePowerL3;
	private ModbusReadLongChannel voltageL1;
	private ModbusReadLongChannel voltageL2;
	private ModbusReadLongChannel voltageL3;
	private ModbusReadLongChannel currentL1;
	private ModbusReadLongChannel currentL2;
	private ModbusReadLongChannel currentL3;
	private ModbusReadLongChannel frequency;

	@Override
	public ModbusReadLongChannel activePower() {
		return activePower;
	}

	@Override
	public ModbusReadLongChannel apparentPower() {
		return apparentPower;
	}

	@Override
	public ModbusReadLongChannel reactivePower() {
		return reactivePower;
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
	public ReadChannel<Long> reactivePowerL1() {
		return reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return reactivePowerL3;
	}

	@Override
	public ReadChannel<Long> currentL1() {
		return currentL1;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		return currentL2;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		return currentL3;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		return voltageL1;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		return voltageL2;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		return voltageL3;
	}

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltageL1;
	}

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel apparentPowerL1;
	public ModbusReadLongChannel apparentPowerL2;
	public ModbusReadLongChannel apparentPowerL3;
	public ModbusReadLongChannel activeNegativeEnergy;
	public ModbusReadLongChannel activePositiveEnergy;
	public ModbusReadLongChannel reactiveNegativeEnergy;
	public ModbusReadLongChannel reactivePositiveEnergy;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		final int OFFSET = 300000 + 1;
		/**
		 * See Modbus defintion: https://www.galoz.co.il/wp-content/uploads/2014/11/EM341-Modbus.pdf
		 */
		return new ModbusProtocol( //
				new ModbusInputRegisterRange(300001 - OFFSET, //
						new SignedDoublewordElement(300001 - OFFSET, //
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(2))
						.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300003 - OFFSET, //
								voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV").multiplier(2))
						.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300005 - OFFSET, //
								voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV").multiplier(2))
						.wordorder(WordOrder.LSWMSW)),
				new ModbusInputRegisterRange(300013 - OFFSET, //
						new SignedDoublewordElement(300013 - OFFSET, //
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA"))
						.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300015 - OFFSET, //
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA"))
						.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300017 - OFFSET, //
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA"))
						.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300019 - OFFSET, //
								activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300021 - OFFSET, //
								activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300023 - OFFSET, //
								activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300025 - OFFSET, //
								apparentPowerL1 = new ModbusReadLongChannel("ApparentPowerL1", this).unit("VA")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300027 - OFFSET, //
								apparentPowerL2 = new ModbusReadLongChannel("ApparentPowerL2", this).unit("VA")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300029 - OFFSET, //
								apparentPowerL3 = new ModbusReadLongChannel("ApparentPowerL3", this).unit("VA")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300031 - OFFSET, //
								reactivePowerL1 = new ModbusReadLongChannel("ReactivePowerL1", this).unit("var")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300033 - OFFSET, //
								reactivePowerL2 = new ModbusReadLongChannel("ReactivePowerL2", this).unit("var")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300035 - OFFSET, //
								reactivePowerL3 = new ModbusReadLongChannel("ReactivePowerL3", this)
								.unit("var").multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new DummyElement(300037 - OFFSET, 300040 - OFFSET), //
						new SignedDoublewordElement(300041 - OFFSET, //
								activePower = new ModbusReadLongChannel("ActivePower", this).unit("W").multiplier(-1))
						.wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300043 - OFFSET, //
								apparentPower = new ModbusReadLongChannel("ApparentPower", this).unit("VA")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW),
						new SignedDoublewordElement(300045 - OFFSET, //
								reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("var")
								.multiplier(-1)).wordorder(WordOrder.LSWMSW)),
				new ModbusInputRegisterRange(300052 - OFFSET, //
						new SignedWordElement(300052 - OFFSET, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHZ").multiplier(2)),
						new UnsignedDoublewordElement(300053 - OFFSET, //
								activePositiveEnergy = new ModbusReadLongChannel("ActivePositiveEnergy", this)
								.unit("kWh").multiplier(1)).wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(300055 - OFFSET, //
								reactivePositiveEnergy = new ModbusReadLongChannel("ReactivePositiveEnergy", this)
								.unit("kvarh").multiplier(-1)).wordOrder(WordOrder.LSWMSW)),
				new ModbusRegisterRange(300079 - OFFSET, //
						new UnsignedDoublewordElement(300079 - OFFSET, //
								activeNegativeEnergy = new ModbusReadLongChannel("ActiveNegativeEnergy", this)
								.unit("kWh").multiplier(-1)).wordOrder(WordOrder.LSWMSW),
						new UnsignedDoublewordElement(300081 - OFFSET, //
								reactiveNegativeEnergy = new ModbusReadLongChannel("ReactiveNegativeEnergy", this)
								.unit("kvarh").multiplier(-1)).wordOrder(WordOrder.LSWMSW)));
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
