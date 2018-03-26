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

public class ModbusReadLongChannel extends ModbusReadChannel<Long> {

	public ModbusReadLongChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/*
	 * Builder
	 */
	@Override
	public ModbusReadLongChannel unit(String unit) {
		return (ModbusReadLongChannel) super.unit(unit);
	}

	public ModbusReadLongChannel multiplier(int multiplier) {
		return multiplier(Long.valueOf(multiplier));
	}

	@Override
	public ModbusReadLongChannel multiplier(Long multiplier) {
		return (ModbusReadLongChannel) super.multiplier(multiplier);
	}

	@Override
	public ModbusReadLongChannel negate() {
		super.negate();
		return this;
	}

	@Override
	public ModbusReadLongChannel delta(Long delta) {
		return (ModbusReadLongChannel) super.delta(delta);
	}

	@Override
	public ModbusReadLongChannel ignore(Long value) {
		return (ModbusReadLongChannel) super.ignore(value);
	}

	@Override
	public ModbusReadLongChannel interval(Long min, Long max) {
		return (ModbusReadLongChannel) super.interval(min, max);
	}

	public ModbusReadLongChannel interval(Integer min, Integer max) {
		return interval(min.longValue(), max.longValue());
	}

	@Override
	public ModbusReadLongChannel label(Long value, String label) {
		return (ModbusReadLongChannel) super.label(value, label);
	}

	public ModbusReadLongChannel label(int value, String label) {
		return label(Long.valueOf(value), label);
	}

	@Override
	protected void updateValue(Long value) {
		super.updateValue(value);
	}

	@Override
	public ReadChannel<Long> required() {
		super.required();
		return this;
	}
}
