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
import io.openems.api.security.OpenemsRole;

public class SimulatorReadChannel extends ReadChannel<Long> {

	public SimulatorReadChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/*
	 * Builder
	 */
	@Override public SimulatorReadChannel unit(String unit) {
		return (SimulatorReadChannel) super.unit(unit);
	}

	public SimulatorReadChannel multiplier(int multiplier) {
		return multiplier(Long.valueOf(multiplier));
	}

	@Override public SimulatorReadChannel multiplier(Long multiplier) {
		return (SimulatorReadChannel) super.multiplier(multiplier);
	}

	@Override public SimulatorReadChannel interval(Long min, Long max) {
		return (SimulatorReadChannel) super.interval(min, max);
	}

	public SimulatorReadChannel interval(Integer min, Integer max) {
		return interval(min.longValue(), max.longValue());
	}

	@Override public SimulatorReadChannel label(Long value, String label) {
		return (SimulatorReadChannel) super.label(value, label);
	}

	public SimulatorReadChannel label(int value, String label) {
		return label(Long.valueOf(value), label);
	}

	@Override public void updateValue(Long value) {
		super.updateValue(value);
	}

	@Override public SimulatorReadChannel required() {
		super.required();
		if (parent() instanceof DeviceNature) {
			DeviceNature parent = (DeviceNature) parent();
			parent.setAsRequired(this);
		}
		return this;
	}

	@Override public SimulatorReadChannel role(OpenemsRole... roles) {
		return (SimulatorReadChannel) super.role(roles);
	}
}
