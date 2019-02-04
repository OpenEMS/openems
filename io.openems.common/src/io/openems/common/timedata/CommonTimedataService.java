package io.openems.common.timedata;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Set;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

public interface CommonTimedataService {

	/**
	 * Queries historic data. The 'resolution' of the query is calculated
	 * dynamically according to the length of the period.
	 * 
	 * @param edgeId   the Edge-ID
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @param channels the Channels
	 */
	public default TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// calculate resolution based on the length of the period
		int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		int resolution = 10 * 60; // default: 10 Minutes
		if (days > 25) {
			resolution = 24 * 60 * 60; // 1 Day
		} else if (days > 6) {
			resolution = 3 * 60 * 60; // 3 Hours
		} else if (days > 2) {
			resolution = 60 * 60; // 60 Minutes
		}

		return this.queryHistoricData(edgeId, fromDate, toDate, channels, resolution);
	}

	/**
	 * Queries historic data.
	 * 
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the Resolution in seconds
	 */
	public TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException;

}
