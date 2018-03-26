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
package io.openems.impl.protocol.modbus;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.DeviceNature;

public class ModbusReadDoubleChannel extends ModbusReadChannel<Double> {

	public ModbusReadDoubleChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/*
	 * Builder
	 */
	@Override
	public ModbusReadDoubleChannel unit(String unit) {
		return (ModbusReadDoubleChannel) super.unit(unit);
	}

	public ModbusReadDoubleChannel multiplier(int multiplier) {
		return multiplier(Long.valueOf(multiplier));
	}

	@Override
	public ModbusReadDoubleChannel multiplier(Long multiplier) {
		return (ModbusReadDoubleChannel) super.multiplier(multiplier);
	}

	@Override
	public ModbusReadDoubleChannel delta(Long delta) {
		return (ModbusReadDoubleChannel) super.delta(delta);
	}

	@Override
	public ModbusReadDoubleChannel interval(Double min, Double max) {
		return (ModbusReadDoubleChannel) super.interval(min, max);
	}

	@Override
	public ModbusReadDoubleChannel label(Double value, String label) {
		return (ModbusReadDoubleChannel) super.label(value, label);
	}

	public ModbusReadDoubleChannel label(int value, String label) {
		return label(Double.valueOf(value), label);
	}

	@Override
	protected void updateValue(Double value) {
		super.updateValue(value);
	}

	@Override
	public ReadChannel<Double> required() {
		super.required();
		return this;
	}
}
