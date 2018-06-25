package io.openems.edge.timedata.influxdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(InfluxTimedata c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
					// }), Arrays.stream(Timedata.ChannelId.values()).map(channelId -> {
					// switch (channelId) {
					// }
					// return null;
				}), Arrays.stream(InfluxTimedata.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_0:
					case STATE_1:
						return new BooleanReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}

	/**
	 * TODO: copied from backend.timedata.influx.provider
	 * 
	 * @param channels
	 * @return
	 * @throws OpenemsException
	 */
	protected static String toChannelAddressList(JsonObject channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (Entry<String, JsonElement> entry : channels.entrySet()) {
			String thingId = entry.getKey();
			JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
			for (JsonElement channelElement : channelIds) {
				String channelId = JsonUtils.getAsString(channelElement);
				channelAddresses
						.add("MEAN(\"" + thingId + "/" + channelId + "\") AS \"" + thingId + "/" + channelId + "\"");
			}
		}
		return String.join(", ", channelAddresses);
	}
}
