package io.openems.edge.predictor.profileclusteringmodel.services;

import static io.openems.edge.predictor.profileclusteringmodel.utils.Utils.fromChannelDataToQuarterlySeries;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.SortedMap;
import java.util.function.Supplier;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionError;
import io.openems.edge.timedata.api.Timedata;

public class PredictionDataService {

	private static final int MINUTES_PER_QUARTER = 15;

	private final Timedata timedata;
	private final Supplier<Clock> clockSupplier;
	private final ChannelAddress channelAddress;

	public PredictionDataService(//
			Timedata timedata, //
			Supplier<Clock> clockSupplier, //
			ChannelAddress channelAddress) {
		this.timedata = timedata;
		this.clockSupplier = clockSupplier;
		this.channelAddress = channelAddress;
	}

	/**
	 * Fetches a time series for the given query window.
	 *
	 * @param queryWindowInDays the query window in days
	 * @return the fetched time series
	 * @throws PredictionException if fetching fails
	 */
	public Series<ZonedDateTime> fetchSeriesForWindow(int queryWindowInDays) throws PredictionException {
		var to = ZonedDateTime.now(this.clockSupplier.get()).truncatedTo(ChronoUnit.DAYS);
		var from = to.minus(queryWindowInDays, ChronoUnit.DAYS);

		return this.fetchSeries(from, to);
	}

	/**
	 * Fetches a time series from the start of today until now.
	 *
	 * @return the fetched time series
	 * @throws PredictionException if fetching fails
	 */
	public Series<ZonedDateTime> fetchSeriesForToday() throws PredictionException {
		var now = ZonedDateTime.now(this.clockSupplier.get());
		var from = now.truncatedTo(ChronoUnit.DAYS);

		return this.fetchSeries(from, now);
	}

	private Series<ZonedDateTime> fetchSeries(//
			ZonedDateTime from, //
			ZonedDateTime to) throws PredictionException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> rawChannelData;
		try {
			rawChannelData = this.timedata.queryHistoricData(//
					null, //
					from, //
					to, //
					Sets.newHashSet(this.channelAddress), //
					new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			throw new PredictionException(PredictionError.NO_CONSUMPTION_DATA, e);
		}

		return fromChannelDataToQuarterlySeries(//
				rawChannelData, //
				this.channelAddress, //
				from, //
				to);
	}
}
