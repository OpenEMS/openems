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
import io.openems.api.channel.StatusBitChannels;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "FENECON Mini ESS")
public class FeneconMiniEss extends ModbusDeviceNature implements AsymmetricEssNature {

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
	// Dummies
	private StaticValueChannel<Long> allowedCharge = new StaticValueChannel<Long>("AllowedCharge", this, 0l);
	private StaticValueChannel<Long> allowedDischarge = new StaticValueChannel<Long>("AllowedDischarge", this, 0l);
	private StaticValueChannel<Long> allowedApparent = new StaticValueChannel<Long>("AllowedApparent", this, 0l);
	private StaticValueChannel<Long> gridMode = new StaticValueChannel<Long>("GridMode", this, 0l);
	private StaticValueChannel<Long> systemState = new StaticValueChannel<Long>("SystemState", this, 0l);
	private StaticValueChannel<Long> reactivePowerL1 = new StaticValueChannel<Long>("ReactivePowerL1", this, 0l);
	private StaticValueChannel<Long> reactivePowerL2 = new StaticValueChannel<Long>("ReactivePowerL2", this, 0l);
	private StaticValueChannel<Long> reactivePowerL3 = new StaticValueChannel<Long>("ReactivePowerL3", this, 0l);
	private StatusBitChannels warning = new StatusBitChannels("Warning", this);

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
	public StatusBitChannels warning() {
		return this.warning;
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
				new ModbusRegisterRange(2007, //
						new UnsignedWordElement(2007, //
								this.activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(2107, //
						new UnsignedWordElement(2107, //
								this.activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(2207, //
						new UnsignedWordElement(2207, //
								this.activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
										.delta(10000l))),
				new ModbusRegisterRange(4812, //
						new UnsignedWordElement(4812, //
								this.soc = new ModbusReadLongChannel("Soc", this).unit("%"))));
		return protocol;
	}
}
