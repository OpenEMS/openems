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
package io.openems.impl.device.janitza;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;

public class JanitzaUMG96RMEMeter extends ModbusDeviceNature implements SymmetricMeterNature {

	public JanitzaUMG96RMEMeter(String thingId) throws ConfigException {
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
	// // TODO multiplier 0.1
	// private final ModbusChannel _activeNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	// private final ModbusChannel _activePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	// private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").build();
	// private final ModbusChannel _apparentEnergy = new ModbusChannelBuilder().nature(this).unit("kVAh").build();
	// private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").build();
	// private final ModbusChannel _reactiveNegativeEnergy = new
	// ModbusChannelBuilder().nature(this).unit("kvarh").build();
	// private final ModbusChannel _reactivePositiveEnergy = new
	// ModbusChannelBuilder().nature(this).unit("kvarh").build();
	// private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").build();
	//
	// public JanitzaUMG96RMEMeter(String thingId) {
	// super(thingId);
	// }
	//
	// @Override public NumericChannel activeNegativeEnergy() {
	// return _activeNegativeEnergy;
	// }
	//
	// @Override public NumericChannel activePositiveEnergy() {
	// return _activePositiveEnergy;
	// }
	//
	// @Override public NumericChannel activePower() {
	// return _activePower;
	// }
	//
	// @Override public NumericChannel apparentEnergy() {
	// return _apparentEnergy;
	// }
	//
	// @Override public NumericChannel apparentPower() {
	// return _apparentPower;
	// }
	//
	// @Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
	// return new ModbusProtocol( //
	// new ModbusRange(874, //
	// new ElementBuilder().address(874).channel(_activePower).floatingPoint().doubleword().signed()
	// .build(), //
	// new ElementBuilder().address(876).dummy(882 - 876).build(), //
	// new ElementBuilder().address(882).channel(_reactivePower).floatingPoint().doubleword().signed()
	// .build(), //
	// new ElementBuilder().address(884).dummy(890 - 884).build(), //
	// new ElementBuilder().address(890).channel(_apparentPower).floatingPoint().doubleword().signed()
	// .build()) //
	// );
	// }
	//
	// @Override public NumericChannel reactiveNegativeEnergy() {
	// return _reactiveNegativeEnergy;
	// }
	//
	// @Override public NumericChannel reactivePositiveEnergy() {
	// return _reactivePositiveEnergy;
	// }
	//
	// @Override public NumericChannel reactivePower() {
	// return _reactivePower;
	// }

}
