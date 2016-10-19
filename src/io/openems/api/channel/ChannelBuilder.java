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

import java.util.HashMap;
import java.util.Map;

import io.openems.api.device.nature.DeviceNature;

public class ChannelBuilder<B extends ChannelBuilder<?>> {
	protected Long delta = 0L;
	protected Map<Long, String> labels;
	protected Long maxValue = null;
	protected Long minValue = null;
	protected Long multiplier = 1L;
	protected DeviceNature nature = null;
	protected String unit = "";

	public Channel build() {
		return new Channel(nature, unit, minValue, maxValue, multiplier, delta, labels);
	}

	public B delta(int delta) {
		return delta(Long.valueOf(delta));
	}

	@SuppressWarnings("unchecked")
	public B delta(Long delta) {
		this.delta = delta;
		return (B) this;
	}

	public B label(int value, String label) {
		return label(Long.valueOf(value), label);
	}

	@SuppressWarnings("unchecked")
	public B label(Long value, String label) {
		if (this.labels == null) {
			this.labels = new HashMap<>();
		}
		this.labels.put(value, label);
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B maxValue(int maxValue) {
		this.maxValue = Long.valueOf(maxValue);
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B maxValue(Long maxValue) {
		this.maxValue = maxValue;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minValue(int minValue) {
		this.minValue = Long.valueOf(minValue);
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minValue(Long minValue) {
		this.minValue = minValue;
		return (B) this;
	}

	public B multiplier(int multiplier) {
		return multiplier(Long.valueOf(multiplier));
	}

	@SuppressWarnings("unchecked")
	public B multiplier(Long multiplier) {
		this.multiplier = multiplier;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B nature(DeviceNature nature) {
		this.nature = nature;
		return (B) this;
	}

	public B percentType() {
		maxValue(100);
		minValue(0);
		return unit("%");
	}

	@SuppressWarnings("unchecked")
	public B unit(String unit) {
		this.unit = unit;
		return (B) this;
	}

}
