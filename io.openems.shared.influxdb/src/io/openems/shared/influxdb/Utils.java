package io.openems.shared.influxdb;

import java.util.ArrayList;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class Utils {
	/**
	 * 
	 * @param channels
	 * @return
	 * @throws OpenemsException
	 */
	protected static String toChannelAddressList(JsonObject channels, boolean cumulative) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (Entry<String, JsonElement> entry : channels.entrySet()) {
			String thingId = entry.getKey();
			JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
			for (JsonElement channelElement : channelIds) {
				String channelId = JsonUtils.getAsString(channelElement);
				if (cumulative) {
					channelAddresses
					.add("CUMULATIVE_SUM(MEAN(\"" + thingId + "/" + channelId + "\")) AS \"" + thingId + "/" + channelId + "\"");
				} else {
					channelAddresses.add(
							"MEAN(\"" + thingId + "/" + channelId + "\") AS \"" + thingId + "/" + channelId + "\"");
				}

			}
		}
		return String.join(", ", channelAddresses);
	}

}
