package io.openems.backend.timedata.api;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

@ProviderType
public interface TimedataService {
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
	
	/**
	 * Queries the database and returns a JsonArray of the form
	 *
	 * <pre>
	 *	[{
	 *  	timestamp: "2017-03-21T08:55:20Z",
	 *  	channels: {
	 *			'thing': {
	 *				'channel': 'value'
	 *			}
	 *		}
	 * 	}]
	 * </pre>
	 *
	 * @param edgeId
	 * @param fromDate
	 * @param toDate
	 * @param channels
	 * @param resolution
	 * @return
	 * @throws OpenemsException
	 */
	public JsonArray queryHistoricData(int edgeId, ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution/* , JsonObject kWh */) throws OpenemsException;
}
