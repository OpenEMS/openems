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

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.Databus;

public class Channel {
	protected final Long delta;
	protected final Optional<Map<Long, String>> labels;
	protected final Logger log;
	protected final Long multiplier;
	private Optional<String> channelId = Optional.empty();
	private Optional<Databus> databus = Optional.empty();
	private Optional<Long> maxValue = Optional.empty();
	private Optional<Long> minValue = Optional.empty();
	private Optional<DeviceNature> nature = Optional.empty();
	private final String unit;
	private Optional<Long> value = Optional.empty();

	public Channel(DeviceNature nature, String unit, Long minValue, Long maxValue, Long multiplier, Long delta,
			Map<Long, String> labels) {
		log = LoggerFactory.getLogger(this.getClass());
		this.nature = Optional.ofNullable(nature);
		this.unit = unit;
		this.multiplier = multiplier;
		this.delta = delta;
		this.minValue = Optional.ofNullable(minValue);
		this.maxValue = Optional.ofNullable(maxValue);
		this.labels = Optional.ofNullable(labels);
	}

	public String getAddress() {
		String natureId;
		if (nature.isPresent()) {
			natureId = nature.get().getThingId();
		} else {
			natureId = "EMPTY";
		}
		return natureId + "/" + channelId.orElse("EMPTY");
	}

	public Optional<String> getChannelId() {
		return channelId;
	}

	public Long getMaxValue() throws InvalidValueException {
		return maxValue.orElseThrow(() -> new InvalidValueException("No Max-Value available."));
	}

	public Optional<Long> getMaxValueOptional() {
		return maxValue;
	}

	public Long getMinValue() throws InvalidValueException {
		return minValue.orElseThrow(() -> new InvalidValueException("No Min-Value available."));
	}

	public Optional<Long> getMinValueOptional() {
		return minValue;
	}

	public String getUnit() {
		return unit;
	}

	public Long getValue() throws InvalidValueException {
		return value.orElseThrow(() -> new InvalidValueException("No Value available."));
	};

	public Optional<Long> getValueOptional() {
		return value;
	};

	public Optional<String> getValueLabelOptional() {
		String label;
		if (value.isPresent() && labels.isPresent() && labels.get().containsKey(value.get())) {
			label = labels.get().get(value.get());
			return Optional.of(label);
		}
		return Optional.empty();
	};

	public void setAsRequired() throws ConfigException {
		if (this.nature.isPresent()) {
			this.nature.get().setAsRequired(this);
		} else {
			throw new ConfigException("DeviceNature is not set [" + this + "]");
		}
	}

	public void setChannelId(String channelId) {
		this.channelId = Optional.of(channelId);
	}

	public void setDatabus(Databus databus) {
		this.databus = Optional.of(databus);
	}

	@Override
	public String toString() {
		Optional<String> label = getValueLabelOptional();
		if (label.isPresent()) {
			return label.get();
		} else if (value.isPresent()) {
			return value.get() + " " + unit;
		} else {
			return "INVALID";
		}
	}

	/**
	 * Update value from the underlying {@link DeviceNature} and send an update event to {@link Databus}.
	 *
	 * @param value
	 */
	protected void updateValue(Long value) {
		updateValue(value, true);
	}

	/**
	 * Update value from the underlying {@link DeviceNature}
	 *
	 * @param value
	 * @param triggerDatabusEvent
	 *            true if an event should be forwarded to {@link Databus}
	 */
	protected void updateValue(Long value, boolean triggerDatabusEvent) {
		Optional<Long> optValue = Optional.ofNullable(value);
		if (optValue.isPresent()) {
			this.value = Optional.of(optValue.get() * multiplier - delta);
		} else {
			this.value = optValue;
		}
		if (databus.isPresent() && triggerDatabusEvent) {
			databus.get().channelValueUpdated(this);
		}
	}
}
