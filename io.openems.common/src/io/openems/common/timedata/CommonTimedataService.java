package io.openems.common.timedata;

import java.io.IOException;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.SortedMap;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse;
import io.openems.common.session.Language;
import io.openems.common.types.ChannelAddress;

public interface CommonTimedataService {

	/**
	 * Handles a {@link QueryHistoricTimeseriesExportXlxsRequest}. Exports historic
	 * data to an Excel file.
	 *
	 * @param edgeId   the Edge-ID
	 * @param request  the {@link QueryHistoricTimeseriesExportXlxsRequest} request
	 * @param language the {@link Language}
	 * @return the {@link QueryHistoricTimeseriesExportXlsxResponse}
	 * @throws OpenemsNamedException on error
	 */
	public default QueryHistoricTimeseriesExportXlsxResponse handleQueryHistoricTimeseriesExportXlxsRequest(
			String edgeId, QueryHistoricTimeseriesExportXlxsRequest request, Language language)
			throws OpenemsNamedException {
		var powerData = this.queryHistoricData(edgeId, request.getFromDate(), request.getToDate(),
				QueryHistoricTimeseriesExportXlsxResponse.POWER_CHANNELS, new Resolution(15, ChronoUnit.MINUTES));

		var energyData = this.queryHistoricEnergy(edgeId, request.getFromDate(), request.getToDate(),
				QueryHistoricTimeseriesExportXlsxResponse.ENERGY_CHANNELS);

		if (powerData == null || energyData == null) {
			return null;
		}

		try {
			return new QueryHistoricTimeseriesExportXlsxResponse(request.getId(), edgeId, request.getFromDate(),
					request.getToDate(), powerData, energyData, language);
		} catch (IOException e) {
			throw new OpenemsException("QueryHistoricTimeseriesExportXlxsRequest failed: " + e.getMessage());
		}
	}

	/**
	 * Calculates the time {@link Resolution} for the period.
	 *
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @return the resolution
	 */
	public static Resolution calculateResolution(ZonedDateTime fromDate, ZonedDateTime toDate) {
		var days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		if (days <= 1) {
			return new Resolution(5, ChronoUnit.MINUTES);
		} else if (days == 2) {
			return new Resolution(10, ChronoUnit.MINUTES);
		} else if (days == 3) {
			return new Resolution(15, ChronoUnit.MINUTES);
		} else if (days == 4) {
			return new Resolution(20, ChronoUnit.MINUTES);
		} else if (days <= 6) {
			return new Resolution(30, ChronoUnit.MINUTES);
		} else if (days <= 12) {
			return new Resolution(1, ChronoUnit.HOURS);
		} else if (days <= 24) {
			return new Resolution(2, ChronoUnit.HOURS);
		} else if (days <= 48) {
			return new Resolution(4, ChronoUnit.HOURS);
		} else if (days <= 96) {
			return new Resolution(8, ChronoUnit.HOURS);
		} else if (days <= 144) {
			return new Resolution(12, ChronoUnit.HOURS);
		} else {
			return new Resolution(1, ChronoUnit.DAYS);
		}
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
		var resolution = request.getResolution() //
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
	 * @param resolution the {@link Resolution}
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
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
	 * {@link Resolution}) per Channel, e.g. to visualize energy in a histogram
	 * chart. For each period the energy is calculated by subtracting first value of
	 * the period from the last value of the period.
	 *
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException;
}
