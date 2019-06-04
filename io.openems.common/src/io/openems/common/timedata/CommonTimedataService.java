package io.openems.common.timedata;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.jsonrpc.request.GetHistoryDataExportXlxsRequest;

public interface CommonTimedataService {
	
	public default Map<ChannelAddress, JsonElement> exportHistoryData(GetHistoryDataExportXlxsRequest request)
			throws OpenemsNamedException {
		return this.queryHistoricEnergy(null, request.getFromDate(), request.getToDate(), request.getDataChannels());
	}

	public default TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> exportEnergyData(
			GetHistoryDataExportXlxsRequest request) throws OpenemsNamedException {
		ZonedDateTime fromDate = request.getFromDate();
		ZonedDateTime toDate = request.getToDate();
		
		int resolution = calculateResolution(fromDate, toDate);
		return this.queryHistoricData(null, fromDate, toDate, request.getEnergyChannels(),
				resolution);
	}

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
		int resolution = calculateResolution( fromDate,  toDate);
		return this.queryHistoricData(edgeId, fromDate, toDate, channels, resolution);
	}

	public default int calculateResolution(ZonedDateTime fromDate, ZonedDateTime toDate) {
		
		int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		int resolution = 10 * 60; // default: 10 Minutes
		if (days > 25) {
			resolution = 24 * 60 * 60; // 1 Day
		} else if (days > 6) {
			resolution = 3 * 60 * 60; // 3 Hours
		} else if (days > 2) {
			resolution = 60 * 60; // 60 Minutes
		}
		return resolution;
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

	/**
	 * Queries historic energy.
	 * 
	 * @param edgeId   the Edge-ID; or null query all
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @param channels the Channels
	 */

	public Map<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException;
}
