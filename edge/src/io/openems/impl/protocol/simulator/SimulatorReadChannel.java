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
package io.openems.impl.protocol.simulator;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.DeviceNature;

public class SimulatorReadChannel<T> extends ReadChannel<T> {

	public SimulatorReadChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/*
	 * Builder
	 */
	@Override
	public SimulatorReadChannel<T> unit(String unit) {
		return (SimulatorReadChannel<T>) super.unit(unit);
	}

	public SimulatorReadChannel<T> multiplier(int multiplier) {
		return multiplier(Long.valueOf(multiplier));
	}

	@Override
	public SimulatorReadChannel<T> multiplier(Long multiplier) {
		return (SimulatorReadChannel<T>) super.multiplier(multiplier);
	}

	@Override
	public SimulatorReadChannel<T> interval(T min, T max) {
		return (SimulatorReadChannel<T>) super.interval(min, max);
	}

	@Override
	public SimulatorReadChannel<T> label(T value, String label) {
		return (SimulatorReadChannel<T>) super.label(value, label);
	}

	@Override
	public void updateValue(T value) {
		super.updateValue(value);
	}

	@Override
	public SimulatorReadChannel<T> required() {
		super.required();
		if (parent() instanceof DeviceNature) {
			DeviceNature parent = (DeviceNature) parent();
			parent.setAsRequired(this);
		}
		return this;
	}
}
