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
package io.openems.impl.protocol.modbus;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.DeviceNature;

public class ModbusReadChannel extends ReadChannel<Long> implements ModbusChannel {

	public ModbusReadChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/*
	 * Builder
	 */
	@Override public ModbusReadChannel unit(String unit) {
		return (ModbusReadChannel) super.unit(unit);
	}

	public ModbusReadChannel multiplier(int multiplier) {
		return multiplier(Long.valueOf(multiplier));
	}

	@Override public ModbusReadChannel multiplier(Long multiplier) {
		return (ModbusReadChannel) super.multiplier(multiplier);
	}

	@Override public ModbusReadChannel delta(Long delta) {
		return (ModbusReadChannel) super.delta(delta);
	}

	@Override public ModbusReadChannel interval(Long min, Long max) {
		return (ModbusReadChannel) super.interval(min, max);
	}

	public ModbusReadChannel interval(Integer min, Integer max) {
		return interval(min.longValue(), max.longValue());
	}

	@Override public ModbusReadChannel label(Long value, String label) {
		return (ModbusReadChannel) super.label(value, label);
	}

	public ModbusReadChannel label(int value, String label) {
		return label(Long.valueOf(value), label);
	}

	@Override protected void updateValue(Long value) {
		super.updateValue(value);
	}

	@Override public ReadChannel<Long> required() {
		super.required();
		if (parent() instanceof DeviceNature) {
			DeviceNature parent = (DeviceNature) parent();
			parent.setAsRequired(this);
		}
		return this;
	}
}
