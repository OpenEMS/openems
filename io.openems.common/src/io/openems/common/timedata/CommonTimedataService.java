package io.openems.common.timedata;

import java.io.IOException;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.SortedMap;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse;
import io.openems.common.types.ChannelAddress;

public interface CommonTimedataService {

	/**
	 * Handles a {@link QueryHistoricTimeseriesExportXlxsRequest}. Exports historic
	 * data to an Excel file.
	 *
	 * @param edgeId  the Edge-ID
	 * @param request the {@link QueryHistoricTimeseriesExportXlxsRequest} request
	 * @return the {@link QueryHistoricTimeseriesExportXlsxResponse}
	 * @throws OpenemsNamedException on error
	 */
	public default QueryHistoricTimeseriesExportXlsxResponse handleQueryHistoricTimeseriesExportXlxsRequest(
			String edgeId, QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
		var powerData = this.queryHistoricData(edgeId,
				request.getFromDate(), request.getToDate(), QueryHistoricTimeseriesExportXlsxResponse.POWER_CHANNELS,
				15 * 60 /* 15 Minutes */);
		var energyData = this.queryHistoricEnergy(edgeId, request.getFromDate(),
				request.getToDate(), QueryHistoricTimeseriesExportXlsxResponse.ENERGY_CHANNELS);

		try {
			return new QueryHistoricTimeseriesExportXlsxResponse(request.getId(), edgeId, request.getFromDate(),
					request.getToDate(), powerData, energyData);
		} catch (IOException e) {
			throw new OpenemsException("QueryHistoricTimeseriesExportXlxsRequest failed: " + e.getMessage());
		}
	}

	/**
	 * Calculates the time resolution for the period in seconds.
	 *
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @return the resolution in seconds
	 */
	public static int calculateResolution(ZonedDateTime fromDate, ZonedDateTime toDate) {
		var days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		int resolution;
		if (days <= 1) {
			resolution = 5 * 60; // 5 Minutes
		} else if (days == 2) {
			resolution = 10 * 60; // 10 Minutes
		} else if (days == 3) {
			resolution = 15 * 60; // 15 Minutes
		} else if (days == 4) {
			resolution = 20 * 60; // 20 Minutes
		} else if (days <= 6) {
			resolution = 30 * 60; // 30 Minutes
		} else if (days <= 12) {
			resolution = 1 * 60 * 60; // 1 Hour
		} else if (days <= 24) {
			resolution = 2 * 60 * 60; // 2 Hours
		} else if (days <= 48) {
			resolution = 4 * 60 * 60; // 4 Hours
		} else if (days <= 96) {
			resolution = 8 * 60 * 60; // 8 Hours
		} else if (days <= 144) {
			resolution = 12 * 60 * 60; // 12 Hours
		} else {
			resolution = 24 * 60 * 60; // 1 Day
		}
		return resolution;
	}

	/**
	 * Queries historic data. The 'resolution' of the query is calculated
	 * dynamically according to the length of the period.
	 *
	 * @param edgeId  the Edge-ID
	 * @param request the {@link QueryHistoricTimeseriesDataRequest}
	 * @return the query result
	 */
	public default SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		// calculate resolution based on the length of the period
		int resolution = request.getResolution()
				.orElse(CommonTimedataService.calculateResolution(request.getFromDate(), request.getToDate()));
		return this.queryHistoricData(edgeId, request.getFromDate(), request.getToDate(), request.getChannels(),
				resolution);
	}

	/**
	 * Queries historic data.
	 *
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the Resolution in seconds
	 * @return the query result
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
	 * @return the query result
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException;

	/**
	 * Queries historic energy per period.
	 *
	 * <p>
	 * This is for use-cases where you want to get the energy for each period (with
	 * length 'resolution') per Channel, e.g. to visualize energy in a histogram
	 * chart. For each period the energy is calculated by subtracting first value of
	 * the period from the last value of the period.
	 *
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the Resolution in seconds
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException;
}
