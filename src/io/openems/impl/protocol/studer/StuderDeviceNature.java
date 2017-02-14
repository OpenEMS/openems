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
package io.openems.impl.protocol.studer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.impl.protocol.studer.internal.StuderProtocol;
import io.openems.impl.protocol.studer.internal.property.ReadProperty;
import io.openems.impl.protocol.studer.internal.property.WriteProperty;

public abstract class StuderDeviceNature implements DeviceNature, ChannelChangeListener {
	protected final Logger log;
	private StuderProtocol protocol = null;
	private final String thingId;
	private List<ThingChannelsUpdatedListener> listeners;

	public StuderDeviceNature(String thingId) throws ConfigException {
		this.thingId = thingId;
		log = LoggerFactory.getLogger(this.getClass());
		// this.protocol = defineModbusProtocol();
		this.listeners = new ArrayList<>();
	}

	private StuderProtocol getProtocol() {
		if (protocol == null) {
			createStuderProtocol();
		}
		return this.protocol;
	}

	@Override
	public void addListener(ThingChannelsUpdatedListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ThingChannelsUpdatedListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public void init() {
		DeviceNature.super.init();
		createStuderProtocol();
	}

	@Override
	public String id() {
		return thingId;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to StuderProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) {
		getProtocol().setAsRequired(channel);
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		createStuderProtocol();
	}

	private void createStuderProtocol() {
		try {
			this.protocol = defineStuderProtocol();
			for (ThingChannelsUpdatedListener listener : this.listeners) {
				listener.thingChannelsUpdated(this);
			}
		} catch (ConfigException e) {
			log.error("Failed to define modbus protocol!", e);
		}
	}

	protected abstract StuderProtocol defineStuderProtocol() throws ConfigException;

	protected void update(int srcAddress, int dstAddress, StuderBridge bridge) throws OpenemsException {
		/**
		 * Update required properties
		 */
		for (ReadProperty<?> property : getProtocol().getRequiredProperties()) {
			property.updateValue(srcAddress, dstAddress, bridge);
		}
		/**
		 * Update other properties
		 */
		Optional<ReadProperty<?>> propertyOptional = getProtocol().getNextOtherProperty();
		if (propertyOptional.isPresent()) {
			propertyOptional.get().updateValue(srcAddress, dstAddress, bridge);
		}
	}

	protected void write(int srcAddress, int dstAddress, StuderBridge bridge) throws OpenemsException {
		for (WriteProperty<?> property : getProtocol().getWritableProperties()) {
			property.writeValue(srcAddress, dstAddress, bridge);
		}
	}
}
