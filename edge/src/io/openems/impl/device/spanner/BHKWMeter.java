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
package io.openems.impl.device.spanner;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.FunctionalReadChannelFunction;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "BHKW Meter")
public class BHKWMeter extends ModbusDeviceNature implements AsymmetricMeterNature, SymmetricMeterNature {

	/*
	 * Constructors
	 */
	public BHKWMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
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
	private FunctionalReadChannel<Long> activePower;
	private FunctionalReadChannel<Long> reactivePower;
	private FunctionalReadChannel<Long> apparentPower;
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	private FunctionalReadChannel<Long> reactivePowerL1;
	private FunctionalReadChannel<Long> reactivePowerL2;
	private FunctionalReadChannel<Long> reactivePowerL3;
	private ModbusReadLongChannel voltageL1;
	private ModbusReadLongChannel voltageL2;
	private ModbusReadLongChannel voltageL3;
	private ModbusReadLongChannel currentL1;
	private ModbusReadLongChannel currentL2;
	private ModbusReadLongChannel currentL3;
	public ModbusReadLongChannel frequencyL1;
	public ModbusReadLongChannel frequencyL2;
	public ModbusReadLongChannel frequencyL3;
	public ModbusReadLongChannel cosPhiL1;
	public ModbusReadLongChannel cosPhiL2;
	public ModbusReadLongChannel cosPhiL3;

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

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(370, new UnsignedDoublewordElement(370, //
						voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(2))),
				new ModbusRegisterRange(371, new UnsignedDoublewordElement(371, //
						voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV").multiplier(2))),
				new ModbusRegisterRange(372, new UnsignedDoublewordElement(372, //
						voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV").multiplier(2))),
				new ModbusRegisterRange(373, new UnsignedDoublewordElement(373, //
						frequencyL1 = new ModbusReadLongChannel("FrequencyL1", this).unit("mHZ").multiplier(1))),
				new ModbusRegisterRange(374, new UnsignedDoublewordElement(374, //
						frequencyL1 = new ModbusReadLongChannel("FrequencyL2", this).unit("mHZ").multiplier(1))),
				new ModbusRegisterRange(375, new UnsignedDoublewordElement(375, //
						frequencyL1 = new ModbusReadLongChannel("FrequencyL3", this).unit("mHZ").multiplier(1))),
				new ModbusRegisterRange(376, new UnsignedDoublewordElement(376, //
						currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA").multiplier(2))),
				new ModbusRegisterRange(377, new UnsignedDoublewordElement(377, //
						currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA").multiplier(2))),
				new ModbusRegisterRange(378, new UnsignedDoublewordElement(378, //
						currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA").multiplier(2))),
				new ModbusRegisterRange(379, new SignedDoublewordElement(379, //
						cosPhiL1 = new ModbusReadLongChannel("CosPhiL1", this))),
				new ModbusRegisterRange(380, new SignedDoublewordElement(380, //
						cosPhiL2 = new ModbusReadLongChannel("CosPhiL2", this))),
				new ModbusRegisterRange(381, new SignedDoublewordElement(381, //
						cosPhiL3 = new ModbusReadLongChannel("SocPhiL3", this))),
				new ModbusRegisterRange(382, new SignedDoublewordElement(382, //
						activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W").multiplier(-2))),
				new ModbusRegisterRange(383, new SignedDoublewordElement(383, //
						activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W").multiplier(-2))),
				new ModbusRegisterRange(384, new SignedDoublewordElement(384, //
						activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W").multiplier(-2))));
		activePower = new FunctionalReadChannel<Long>("ActivePower", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) throws InvalidValueException {
				Long value = 0L;
				for(ReadChannel<Long> channel: channels) {
					if(channel.valueOptional().isPresent()) {
						value += channel.valueOptional().get();
					}else {
						return null;
					}
				}
				return value;
			}

		}, activePowerL1,activePowerL2,activePowerL3);
		reactivePowerL1 = new FunctionalReadChannel<Long>("ReactivePowerL1", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) throws InvalidValueException {
				if(channels.length == 2 && channels[0].valueOptional().isPresent()&& channels[1].valueOptional().isPresent()) {
					return ControllerUtils.calculateReactivePower(channels[0].valueOptional().get(), ((double)channels[1].valueOptional().get())/100.0);
				}
				return null;
			}
		}, activePowerL1,cosPhiL1);
		reactivePowerL2 = new FunctionalReadChannel<Long>("ReactivePowerL2", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) throws InvalidValueException {
				if(channels.length == 2 && channels[0].valueOptional().isPresent()&& channels[1].valueOptional().isPresent()) {
					return ControllerUtils.calculateReactivePower(channels[0].valueOptional().get(), ((double)channels[1].valueOptional().get())/100.0);
				}
				return null;
			}
		}, activePowerL2,cosPhiL2);
		reactivePowerL3 = new FunctionalReadChannel<Long>("ReactivePowerL3", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) throws InvalidValueException {
				if(channels.length == 2 && channels[0].valueOptional().isPresent()&& channels[1].valueOptional().isPresent()) {
					return ControllerUtils.calculateReactivePower(channels[0].valueOptional().get(), ((double)channels[1].valueOptional().get())/100.0);
				}
				return null;
			}
		}, activePowerL3,cosPhiL3);
		reactivePower = new FunctionalReadChannel<Long>("ReactivePower", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) throws InvalidValueException {
				Long value = 0L;
				for(ReadChannel<Long> channel: channels) {
					if(channel.valueOptional().isPresent()) {
						value += channel.valueOptional().get();
					}else {
						return null;
					}
				}
				return value;
			}

		}, reactivePowerL1,reactivePowerL2,reactivePowerL3);
		apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this, new FunctionalReadChannelFunction<Long>() {

			@Override
			public Long handle(@SuppressWarnings("unchecked") ReadChannel<Long>... channels) throws InvalidValueException {
				if(channels.length == 2 && channels[0].valueOptional().isPresent()&& channels[1].valueOptional().isPresent()) {
					return ControllerUtils.calculateApparentPower(channels[0].valueOptional().get(), channels[1].valueOptional().get());
				}
				return null;
			}

		}, activePower,reactivePower);
		return protocol;
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
	public ReadChannel<Long> frequency() {
		return frequencyL1;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltageL1;
	}
}
