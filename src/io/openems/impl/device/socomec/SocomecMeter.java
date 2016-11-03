/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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
package io.openems.impl.device.socomec;

import io.openems.api.channel.IsChannel;
import io.openems.api.channel.numeric.NumericChannel;
import io.openems.api.device.nature.MeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

public class SocomecMeter extends ModbusDeviceNature implements MeterNature {

	private final ModbusChannel _activeNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _activePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _apparentEnergy = new ModbusChannelBuilder().nature(this).unit("kVAh").build();
	private final ModbusChannel _reactiveNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(10)
			.build();
	private final ModbusChannel _reactivePowerPhaseA = new ModbusChannelBuilder().nature(this).unit("var")
			.multiplier(10).build();
	private final ModbusChannel _reactivePowerPhaseB = new ModbusChannelBuilder().nature(this).unit("var")
			.multiplier(10).build();
	private final ModbusChannel _reactivePowerPhaseC = new ModbusChannelBuilder().nature(this).unit("var")
			.multiplier(10).build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(10)
			.build();
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10).build();
	private final ModbusChannel _activePowerPhaseA = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
			.build();
	private final ModbusChannel _activePowerPhaseB = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
			.build();
	private final ModbusChannel _activePowerPhaseC = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
			.build();

	public SocomecMeter(String thingId) {
		super(thingId);
	}

	@Override
	public NumericChannel activeNegativeEnergy() {
		return _activeNegativeEnergy;
	}

	@Override
	public NumericChannel activePositiveEnergy() {
		return _activePositiveEnergy;
	}

	@Override
	public NumericChannel activePower() {
		return _activePower;
	}

	@Override
	public NumericChannel apparentEnergy() {
		return _apparentEnergy;
	}

	@Override
	public NumericChannel apparentPower() {
		return _apparentPower;
	}

	@Override
	public NumericChannel reactiveNegativeEnergy() {
		return _reactiveNegativeEnergy;
	}

	@Override
	public NumericChannel reactivePositiveEnergy() {
		return _reactivePositiveEnergy;
	}

	@Override
	public NumericChannel reactivePower() {
		return _reactivePower;
	}

	@IsChannel(id = "ActivePowerPhaseA")
	public NumericChannel activePowerPhaseA() {
		return _activePowerPhaseA;
	}

	@IsChannel(id = "ActivePowerPhaseB")
	public NumericChannel activePowerPhaseB() {
		return _activePowerPhaseB;
	}

	@IsChannel(id = "ActivePowerPhaseC")
	public NumericChannel activePowerPhaseC() {
		return _activePowerPhaseC;
	}

	@IsChannel(id = "RectivePowerPhaseA")
	public NumericChannel reactivePowerPhaseA() {
		return _reactivePowerPhaseA;
	}

	@IsChannel(id = "RectivePowerPhaseB")
	public NumericChannel reactivePowerPhaseB() {
		return _reactivePowerPhaseB;
	}

	@IsChannel(id = "RectivePowerPhaseC")
	public NumericChannel reactivePowerPhaseC() {
		return _reactivePowerPhaseC;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(0xc568, //
						new ElementBuilder().address(0xc568).channel(_activePower).doubleword().signed().build(), //
						new ElementBuilder().address(0xc56A).channel(_reactivePower).doubleword().signed().build(), //
						new ElementBuilder().address(0xc56C).channel(_apparentPower).doubleword().build(),
						new ElementBuilder().address(0xc56E).dummy(0xc570 - 0xc56E).build(),
						new ElementBuilder().address(0xc570).channel(_activePowerPhaseA).doubleword().signed().build(),
						new ElementBuilder().address(0xc572).channel(_activePowerPhaseB).doubleword().signed().build(),
						new ElementBuilder().address(0xc574).channel(_activePowerPhaseC).doubleword().signed().build(),
						new ElementBuilder().address(0xc576).channel(_reactivePowerPhaseA).doubleword().signed()
								.build(),
						new ElementBuilder().address(0xc578).channel(_reactivePowerPhaseB).doubleword().signed()
								.build(),
						new ElementBuilder().address(0xc57A).channel(_reactivePowerPhaseC).doubleword().signed()
								.build()), //
				new ModbusRange(0xc652, //
						new ElementBuilder().address(0xc652).channel(_activePositiveEnergy).doubleword().build(),
						new ElementBuilder().address(0xc654).channel(_reactivePositiveEnergy).doubleword().build(),
						new ElementBuilder().address(0xc656).channel(_apparentEnergy).doubleword().signed().build(),
						new ElementBuilder().address(0xc658).channel(_activeNegativeEnergy).doubleword().build(),
						new ElementBuilder().address(0xc65a).channel(_reactiveNegativeEnergy).doubleword().build()));
	}

}
