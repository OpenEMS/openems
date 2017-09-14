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
package io.openems.core;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.common.types.ChannelAddress;

public class Databus implements ChannelUpdateListener, ChannelChangeListener {
	private final static Logger log = LoggerFactory.getLogger(Databus.class);

	private static Databus instance;

	public static synchronized Databus getInstance() {
		if (Databus.instance == null) {
			Databus.instance = new Databus();
		}
		return Databus.instance;
	}

	private final ThingRepository thingRepository;

	private Databus() {
		thingRepository = ThingRepository.getInstance();
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		log.debug("Channel [" + channel.address() + "] updated: " + newValue);
		// Call Persistence-Workers
		if (channel instanceof ReadChannel<?> && !(channel instanceof ConfigChannel<?>)) {
			thingRepository.getPersistences().forEach(persistence -> {
				if (persistence instanceof ChannelUpdateListener) {
					((ChannelUpdateListener) persistence).channelUpdated(channel, newValue);
				}
			});
		}

		// Update min/max values of meter
		if (channel.parent() instanceof AsymmetricMeterNature && (channel.id().equals("ActivePowerL1")
				|| channel.id().equals("ActivePowerL2") || channel.id().equals("ActivePowerL3"))) {
			((AsymmetricMeterNature) channel.parent()).updateMinMaxAsymmetricActivePower();
		} else if (channel.parent() instanceof SymmetricMeterNature && channel.id().equals("ActivePower")) {
			((SymmetricMeterNature) channel.parent()).updateMinMaxSymmetricActivePower();
		} else if (channel.parent() instanceof ChargerNature && channel.id().equals("ActualPower")) {
			((ChargerNature) channel.parent()).updateMaxChargerActualPower();
		}

	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		log.debug("Channel [" + channel.address() + "] changed from " + oldValue + " to " + newValue);
		// Call Persistence-Workers
		if (!(channel instanceof ConfigChannel<?>)) {
			thingRepository.getPersistences().forEach(persistence -> {
				if (persistence instanceof ChannelChangeListener) {
					((ChannelChangeListener) persistence).channelChanged(channel, newValue, oldValue);
				}
			});
		}
	}

	public Optional<?> getValue(String thingId, String channelId) {
		Optional<Channel> channel = thingRepository.getChannel(thingId, channelId);
		if (channel.isPresent() && channel.get() instanceof ReadChannel<?>) {
			return ((ReadChannel<?>) channel.get()).valueOptional();
		} else {
			return Optional.empty();
		}
	}

	public Optional<?> getValue(ChannelAddress channelAddress) {
		return this.getValue(channelAddress.getThingId(), channelAddress.getChannelId());
	}
}
