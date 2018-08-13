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
package io.openems.api.channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import io.openems.api.device.nature.DeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;

public class StatusBitChannel extends ModbusReadChannel<Long> {

	public StatusBitChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/**
	 * Get labels for all set bits
	 * Example: Value is 5, Labels are 1=One; 2=Two; 4=Four -> this method returns [One, Four]
	 *
	 * @return
	 */
	public Set<String> labels() {
		Set<String> result = new HashSet<>();
		Optional<Long> valueOptional = valueOptional();
		if (valueOptional.isPresent() && !this.labels.isEmpty()) {
			long value = valueOptional.get();
			long max = Collections.max(this.labels.keySet());
			if (max * 2 <= value) {
				value = value & (max - 1);
			}
			for (Entry<Long, String> entry : this.labels.descendingMap().entrySet()) {
				if (entry.getKey() <= value) {
					result.add(entry.getValue());
					value -= entry.getKey();
				}
			}
		}
		return result;
	};

	@Override
	public Optional<String> labelOptional() {
		Set<String> labels = this.labels();
		if (labels.isEmpty()) {
			return Optional.empty();
		} else {
			StringJoiner joiner = new StringJoiner(",");
			for (String label : labels) {
				joiner.add(this.id() + "/" + label);
			}
			return Optional.of(joiner.toString());
		}
	};

	@Override
	public StatusBitChannel label(Long value, String label) {
		return (StatusBitChannel) super.label(value, label);
	}

	public StatusBitChannel label(int value, String label) {
		return (StatusBitChannel) super.label(Long.valueOf(value), label);
	}
}
