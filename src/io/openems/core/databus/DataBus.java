package io.openems.core.databus;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;

public class DataBus {
	private final static Logger log = LoggerFactory.getLogger(DataBus.class);

	/**
	 * holds thingId -> channelId -> DataChannel
	 */
	private Map<String, Map<String, DataChannel>> thingDataChannels = new HashMap<>();

	/**
	 * holds thingId -> thing
	 */
	private Map<String, Thing> things = new HashMap<>();

	public void addThing(String thingId, Thing thing) {
		things.put(thingId, thing);
		Map<String, DataChannel> dataChannels = DataBusFactory.getDataChannels(thing, this);
		thingDataChannels.put(thingId, dataChannels);
	}

	public void channelValueUpdated(Channel channel) {
		log.info("Channel update: " + channel);
	};

	public Set<String> getChannelIds(String thingId) {
		return thingDataChannels.get(thingId).keySet();
	}

	public Set<String> getThingIds() {
		return things.keySet();
	}

	public BigInteger getValue(String thingId, String channelId) throws InvalidValueException {
		return thingDataChannels.get(thingId).get(channelId).getChannel().getValue();
	}

	/**
	 * Nicely prints all {@link Thing}s and {@link DataChannel}s to system output
	 *
	 * @param things
	 */
	public void printAll() {
		log.info("DataBus:");
		log.info("--------");
		for (Entry<String, Map<String, DataChannel>> thingDataChannel : thingDataChannels.entrySet()) {
			log.info("Thing [" + thingDataChannel.getKey() + "]");
			for (Entry<String, DataChannel> dataChannel : thingDataChannel.getValue().entrySet()) {
				log.info("  Channel [" + dataChannel.getKey() + "]: " + dataChannel.getValue());
			}
		}
	}
}
