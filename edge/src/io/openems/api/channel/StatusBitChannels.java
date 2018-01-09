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
package io.openems.api.channel;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import io.openems.api.thing.Thing;

public class StatusBitChannels extends ReadChannel<Long> {
	private final Set<StatusBitChannel> channels = new HashSet<>();

	public StatusBitChannels(String id, Thing parent) {
		super(id, parent);
	}

	public StatusBitChannel channel(StatusBitChannel channel) {
		this.channels.add(channel);
		return channel;
	}

	public Set<String> labels() {
		Set<String> result = new HashSet<>();
		for (StatusBitChannel channel : channels) {
			result.addAll(channel.labels());
		}
		return result;
	}

	@Override public Optional<String> labelOptional() {
		Set<String> labels = this.labels();
		if (labels.isEmpty()) {
			return Optional.empty();
		} else {
			StringJoiner joiner = new StringJoiner(",");
			for (String label : labels) {
				joiner.add(label);
			}
			return Optional.of(joiner.toString());
		}
	};

	@Override public String toString() {
		Optional<String> string = labelOptional();
		if (string.isPresent()) {
			return string.get();
		} else {
			return "";
		}
	}
}
