package io.openems.impl.persistence.influxdb;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

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
	public JsonArray queryHistoricData(Optional<Integer> edgeIdOpt, ZonedDateTime fromDate, ZonedDateTime toDate,
			JsonObject channels, int resolution/* , JsonObject kWh */) throws OpenemsException;

	public default JsonArray queryHistoricData(int edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
			JsonObject channels, int resolution/* , JsonObject kWh */) throws OpenemsException {
		return this.queryHistoricData(Optional.of(edgeId), fromDate, toDate, channels, resolution);
	}

	public default JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution/* , JsonObject kWh */) throws OpenemsException {
		return this.queryHistoricData(Optional.empty(), fromDate, toDate, channels, resolution);
	}

	public default JsonArray queryHistoricData(int edgeId, JsonObject jHistoricData) throws OpenemsException {
		return this.queryHistoricData(Optional.of(edgeId), jHistoricData);
	}

	public default JsonArray queryHistoricData(JsonObject jHistoricData) throws OpenemsException {
		return this.queryHistoricData(Optional.empty(), jHistoricData);
	}

	public default JsonArray queryHistoricData(Optional<Integer> edgeIdOpt, JsonObject jHistoricData)
			throws OpenemsException {
		int timezoneDiff = JsonUtils.getAsInt(jHistoricData, "timezone");
		ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
		ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(jHistoricData, "fromDate", timezone);
		ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(jHistoricData, "toDate", timezone).plusDays(1);
		JsonObject channels = JsonUtils.getAsJsonObject(jHistoricData, "channels");
		// TODO check if role is allowed to read these channels
		// JsonObject kWh = JsonUtils.getAsJsonObject(jQuery, "kWh");
		int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		// TODO better calculation of sensible resolution
		int resolution = 10 * 60; // 10 Minutes
		if (days > 25) {
			resolution = 24 * 60 * 60; // 1 Day
		} else if (days > 6) {
			resolution = 3 * 60 * 60; // 3 Hours
		} else if (days > 2) {
			resolution = 60 * 60; // 60 Minutes
		}
		return this.queryHistoricData(edgeIdOpt, fromDate, toDate, channels, resolution);
	}
}
