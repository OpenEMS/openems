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
package io.openems.impl.device.pro;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.MeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;

//
// import io.openems.api.channel.IsChannel;
// import io.openems.api.channel.numeric.NumericChannel;
// import io.openems.api.channel.numeric.NumericChannelBuilder;
// import io.openems.api.channel.numeric.NumericChannelBuilder.Aggregation;
// import io.openems.api.controller.IsThingMap;
// import io.openems.api.device.nature.MeterNature;
// import io.openems.api.exception.ConfigException;
// import io.openems.impl.protocol.modbus.ModbusChannel;
// import io.openems.impl.protocol.modbus.ModbusDeviceNature;
// import io.openems.impl.protocol.modbus.internal.ElementBuilder;
// import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
// import io.openems.impl.protocol.modbus.internal.ModbusRange;
// import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;
//
// @IsThingMap(type = MeterNature.class)
public class FeneconProPvMeter extends ModbusDeviceNature implements MeterNature {

	public FeneconProPvMeter(String thingId) throws ConfigException {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override public ReadChannel<Long> activeNegativeEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> activePositiveEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> activePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> apparentEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> apparentPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> reactiveNegativeEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> reactivePositiveEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public ReadChannel<Long> reactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		// TODO Auto-generated method stub
		return null;
	}
	//
	// @IsChannel(id = "ActivePowerPhaseA")
	// private final ModbusChannel _activePowerPhaseA = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
	// .build();
	// @IsChannel(id = "ActivePowerPhaseB")
	// private final ModbusChannel _activePowerPhaseB = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
	// .build();
	// @IsChannel(id = "ActivePowerPhaseC")
	// private final ModbusChannel _activePowerPhaseC = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
	// .build();
	// private final NumericChannel _activePower = new NumericChannelBuilder<>().nature(this).unit("W")
	// .channel(_activePowerPhaseA, _activePowerPhaseB, _activePowerPhaseC).aggregate(Aggregation.SUM).build();
	//
	// public FeneconProPvMeter(String thingId) {
	// super(thingId);
	// }
	//
	// @Override
	// public NumericChannel activeNegativeEnergy() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public NumericChannel activePositiveEnergy() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public NumericChannel activePower() {
	// return _activePower;
	// }
	//
	// @Override
	// public NumericChannel apparentEnergy() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public NumericChannel apparentPower() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public NumericChannel reactiveNegativeEnergy() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public NumericChannel reactivePositiveEnergy() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public NumericChannel reactivePower() {
	// return null;
	// }
	//
	// @Override
	// protected ModbusProtocol defineModbusProtocol() throws ConfigException {
	// return new ModbusProtocol( //
	// new ModbusRange(143, //
	// new ElementBuilder().address(143).channel(_activePowerPhaseA).build(),
	// new ElementBuilder().address(144).channel(_activePowerPhaseB).build(),
	// new ElementBuilder().address(145).channel(_activePowerPhaseC).build()));
	// }
	//
}
