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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.Databus;

public abstract class Channel<T> {
	protected final Logger log;
	private Optional<String> channelId = Optional.empty();
	private Optional<Databus> databus = Optional.empty();
	private Optional<DeviceNature> nature = Optional.empty();
	private Optional<T> value = Optional.empty();

	public Channel(DeviceNature nature, Optional<String> channelId) {
		log = LoggerFactory.getLogger(this.getClass());
		this.nature = Optional.ofNullable(nature);
		this.channelId = channelId;
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

	public T getValue() throws InvalidValueException {
		return getValueOptional().orElseThrow(() -> new InvalidValueException("No Value available."));
	};

	public Optional<T> getValueOptional() {
		return value;
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

	/**
	 * Update value from the underlying {@link DeviceNature} and send an update event to {@link Databus}.
	 *
	 * @param value
	 */
	protected void updateValue(T value) {
		updateValue(value, true);
	}

	/**
	 * Update value from the underlying {@link DeviceNature}
	 *
	 * @param value
	 * @param triggerDatabusEvent
	 *            true if an event should be forwarded to {@link Databus}
	 */
	protected void updateValue(T value, boolean triggerDatabusEvent) {
		this.value = Optional.ofNullable(value);
		if (databus.isPresent() && triggerDatabusEvent) {
			databus.get().channelValueUpdated(this);
		}
	}
}
