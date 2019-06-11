package io.openems.common.timedata;

import java.io.IOException;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.SortedMap;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse;
import io.openems.common.types.ChannelAddress;

public interface CommonTimedataService {

	/**
	 * Handles a QueryHistoricTimeseriesExportXlxsRequest request. Exports historic
	 * data to an Excel file.
	 * 
	 * @param request the QueryHistoricTimeseriesExportXlxsRequest request
	 * @return the QueryHistoricTimeseriesExportXlsxResponse on error
	 * @throws OpenemsNamedException
	 */
	public default QueryHistoricTimeseriesExportXlsxResponse handleQueryHistoricTimeseriesExportXlxsRequest(
			String edgeId, QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData = this.queryHistoricData(edgeId,
				request.getFromDate(), request.getToDate(), request.getDataChannels(), 15 * 60 /* 15 Minutes */);
		SortedMap<ChannelAddress, JsonElement> historicEnergy = this.queryHistoricEnergy(edgeId, request.getFromDate(),
				request.getToDate(), request.getEnergyChannels());
		try {
			return new QueryHistoricTimeseriesExportXlsxResponse(request.getId(), request.getFromDate(),
					request.getToDate(), historicData, historicEnergy);
		} catch (IOException e) {
			throw new OpenemsException("QueryHistoricTimeseriesExportXlxsRequest failed: " + e.getMessage());
		}
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
	public default SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// calculate resolution based on the length of the period
		int resolution = calculateResolution(fromDate, toDate);
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

	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
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

	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException;
}
