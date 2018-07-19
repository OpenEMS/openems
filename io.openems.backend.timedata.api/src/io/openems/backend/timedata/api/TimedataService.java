package io.openems.backend.timedata.api;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;

@ProviderType
public interface TimedataService extends CommonTimedataService {
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
	public void write(int edgeId, JsonObject jData) throws OpenemsException;

	/**
	 * Gets the latest value for the given ChannelAddress
	 * 
	 * @param edgeId
	 * @param channelAddress
	 * @return
	 */
	public Optional<Object> getChannelValue(int edgeId, ChannelAddress channelAddress);

}
