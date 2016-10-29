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
package io.openems.core.databus;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelListener;
import io.openems.api.channel.numeric.WriteableNumericChannel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;

public class Databus {
	private final static Logger log = LoggerFactory.getLogger(Databus.class);

	/**
	 * holds bridgeId -> Bridge
	 */
	private final Map<String, Bridge> bridges = new HashMap<>();

	/**
	 * holds thingId -> channelId -> DataChannel
	 */
	private final Map<String, Map<String, DataChannel>> thingDataChannels = new HashMap<>();

	/**
	 * holds thingId -> Thing
	 */
	private final Map<String, Thing> things = new HashMap<>();

	/**
	 * holds WritableChannels
	 */
	private final List<DataChannel> writableChannels = new LinkedList<>();

	/**
	 * holds Channel-Updated listeners
	 */
	private final Map<Channel<?>, List<ChannelListener>> channelListeners = new ConcurrentHashMap<>();

	/**
	 * Adds a thing to the Databus and fills the local convenience maps
	 *
	 * @param thingId
	 * @param thing
	 */
	public synchronized void addThing(String thingId, Thing thing) {
		// add to central Thing-Map
		things.put(thingId, thing);
		// add to central ThingDataChannel-Map
		Map<String, DataChannel> dataChannels = DatabusFactory.getDataChannels(thing, this);
		thingDataChannels.put(thingId, dataChannels);
		// add to central WritableChannel-Map
		for (DataChannel dataChannel : dataChannels.values()) {
			if (dataChannel.channel instanceof WriteableNumericChannel) {
				this.writableChannels.add(dataChannel);
			}
		}
		// add to central Bridge-Map
		if (thing instanceof Bridge) {
			Bridge bridge = (Bridge) thing;
			bridges.put(thingId, bridge);
		}
	}

	/**
	 * Is getting triggered on update event of any {@link Channel} on the Databus
	 *
	 * @param channel
	 */
	public void channelValueUpdated(Channel<?> channel) {
		List<ChannelListener> listeners = channelListeners.get(channel);
		if (listeners != null) {
			for (ChannelListener listener : listeners) {
				listener.channelUpdated(channel);
			}
		}
	}

	public void addListener(Channel<?> channel, ChannelListener listener) {
		List<ChannelListener> listeners = channelListeners.get(channel);
		if (listeners == null) {
			listeners = new LinkedList<ChannelListener>();
			channelListeners.put(channel, listeners);
		}
		listeners.add(listener);
	}

	public Set<String> getChannelIds(String thingId) {
		return Collections.unmodifiableSet(thingDataChannels.get(thingId).keySet());
	};

	public Thing getThing(String thingId) {
		return things.get(thingId);
	}

	public Set<String> getThingIds() {
		return Collections.unmodifiableSet(things.keySet());
	}

	public Object getValue(String thingId, String channelId) throws InvalidValueException {
		return thingDataChannels.get(thingId).get(channelId).channel.getValue();
	}

	public List<DataChannel> getWritableChannels() {
		return Collections.unmodifiableList(this.writableChannels);
	}

	/**
	 * Nicely prints all {@link Thing}s and {@link DataChannelMapping}s to system output
	 *
	 * @param things
	 */
	public void printAll() {
		log.info("Databus:");
		log.info("--------");
		for (Entry<String, Map<String, DataChannel>> thingDataChannel : thingDataChannels.entrySet()) {
			log.info("Thing [" + thingDataChannel.getKey() + "]");
			for (Entry<String, DataChannel> dataChannel : thingDataChannel.getValue().entrySet()) {
				log.info("  Channel [" + dataChannel.getKey() + "]: " + dataChannel.getValue());
			}
		}
	}

	/**
	 * Triggers a write for all {@link WriteableNumericChannel} via their respective {@link Bridge}.
	 */
	public void writeAll() {
		for (Bridge bridge : this.bridges.values()) {
			bridge.triggerWrite();
		}
	}
}
