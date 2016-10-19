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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.Databus;

public class Channel {
	protected final Long delta;
	protected final Map<Long, String> labels;
	protected final Logger log;
	protected Long maxValue = null;
	protected Long minValue = null;
	protected final Long multiplier;
	protected Long value = null;
	private String channelId = null;
	private Databus databus = null;
	private DeviceNature nature = null;
	private final String unit;

	public Channel(DeviceNature nature, @NonNull String unit, Long minValue, Long maxValue, Long multiplier, Long delta,
			Map<Long, String> labels) {
		log = LoggerFactory.getLogger(this.getClass());
		this.nature = nature;
		this.unit = unit;
		this.multiplier = multiplier;
		this.delta = delta;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.labels = labels;
	}

	@NonNull
	public String getAddress() {
		String natureId = null;
		if (nature != null) {
			natureId = nature.getThingId();
		}
		return natureId + "/" + channelId;
	}

	@Nullable
	public String getChannelId() {
		return channelId;
	}

	@Nullable
	public Long getMaxValue() {
		return maxValue;
	}

	@Nullable
	public Long getMinValue() {
		return minValue;
	}

	@SuppressWarnings("null")
	@NonNull
	public String getUnit() {
		return unit;
	}

	@SuppressWarnings("null")
	@NonNull
	public Long getValue() throws InvalidValueException {
		if (value != null) {
			return value;
		} else {
			throw new InvalidValueException("Channel value is invalid.");
		}
	};

	@NonNull
	public String getValueLabel() throws InvalidValueException {
		String label = getValueLabelOrNull();
		if (label != null) {
			return label;
		} else {
			throw new InvalidValueException("Channel value is invalid.");
		}
	};

	@Nullable
	public String getValueLabelOrNull() {
		if (value != null) {
			if (labels != null && labels.containsKey(value)) {
				return labels.get(value);
			} else {
				return "UNKNOWN";
			}
		}
		return null;
	}

	@Nullable
	public Long getValueOrNull() {
		return value;
	};

	public void setAsRequired() throws ConfigException {
		if (this.nature == null) {
			throw new ConfigException("DeviceNature is not set [" + this + "]");
		} else {
			this.nature.setAsRequired(this);
		}
	}

	public void setChannelId(@NonNull String channelId) {
		this.channelId = channelId;
	}

	public void setDatabus(@NonNull Databus databus) {
		this.databus = databus;
	}

	@Override
	public String toString() {
		if (value != null) {
			if (labels != null) {
				return getValueLabelOrNull();
			} else {
				return value + " " + unit;
			}
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
		if (value == null) {
			this.value = null;
		} else {
			this.value = value * multiplier - delta;
		}
		if (databus != null && triggerDatabusEvent) {
			databus.channelValueUpdated(this);
		}
	}
}
