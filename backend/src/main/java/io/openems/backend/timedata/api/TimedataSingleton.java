package io.openems.backend.timedata.api;

import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.device.MetadataDevices;
import io.openems.common.api.TimedataSource;
import io.openems.common.types.ChannelAddress;

public interface TimedataSingleton extends TimedataSource {
	/**
	 * Takes a JsonObject and writes the points to database.
	 *
	 * <pre>
	 * 	{
	 * 		"timestamp1" {
	 * 			"channel1": value,
	 * 			"channel2": value
	 * 		},
	 * 		"timestamp2" {
	 * 			"channel1": value,
	 * 			"channel2": value
	 *		}
	 *	}
	 * </pre>
	 */
	public void write(MetadataDevices devices, JsonObject jData);

	public Optional<Object> getChannelValue(int deviceId, ChannelAddress channelAddress);
}
