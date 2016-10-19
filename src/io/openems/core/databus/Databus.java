package io.openems.core.databus;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;
import io.openems.core.bridge.Bridge;

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
			if (dataChannel.channel instanceof WriteableChannel) {
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
	public void channelValueUpdated(Channel channel) {
		// log.info("Channel update: " + channel);
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

	public BigInteger getValue(String thingId, String channelId) throws InvalidValueException {
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
	 * Triggers a write for all {@link WriteableChannel} via their respective {@link Bridge}.
	 */
	public void writeAll() {
		for (Bridge bridge : this.bridges.values()) {
			bridge.triggerWrite();
		}
	}
}
