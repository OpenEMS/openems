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
import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.profileclusteringmodel.training.TrainingError;
import io.openems.edge.timedata.api.Timedata;

public class TrainingDataService {

	private static final int MINUTES_PER_QUARTER = 15;

	private final Timedata timedata;
	private final Supplier<Clock> clockSupplier;
	private final ChannelAddress channelAddress;

	public TrainingDataService(//
			Timedata timedata, //
			Supplier<Clock> clockSupplier, //
			ChannelAddress channelAddress) {
		this.timedata = timedata;
		this.clockSupplier = clockSupplier;
		this.channelAddress = channelAddress;
	}

	/**
	 * Fetches a time series for the specified query window.
	 *
	 * @param queryWindow the query window defining the maximum and minimum range
	 * @return the fetched time series
	 * @throws TrainingException if fetching fails or data validation fails
	 */
	public Series<ZonedDateTime> fetchSeriesForWindow(QueryWindow queryWindow) throws TrainingException {
		var to = ZonedDateTime.now(this.clockSupplier.get()).truncatedTo(ChronoUnit.DAYS);
		var from = to.minus(queryWindow.maxWindowDays(), ChronoUnit.DAYS);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> rawChannelData;
		try {
			rawChannelData = this.timedata.queryHistoricData(//
					null, //
					from, //
					to, //
					Sets.newHashSet(this.channelAddress), //
					new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			throw new TrainingException(TrainingError.NO_CONSUMPTION_DATA, e);
		}

		var series = fromChannelDataToQuarterlySeries(//
				rawChannelData, //
				this.channelAddress, //
				from, //
				to);

		this.ensureMinCoverage(series, to, queryWindow);

		return series;
	}

	private void ensureMinCoverage(//
			Series<ZonedDateTime> series, //
			ZonedDateTime to, //
			QueryWindow queryWindow) throws TrainingException {
		for (var timestamp : series.getIndex()) {
			if (!series.get(timestamp).isNaN()) {
				var minRequiredStart = to.minusDays(queryWindow.minWindowDays());
				if (timestamp.isAfter(minRequiredStart)) {
					throw new TrainingException(TrainingError.INSUFFICIENT_TRAINING_DATA, String.format(//
							"Channel data too sparse: Need valid data since %s (min %d days), but earliest valid data is %s", //
							minRequiredStart, //
							queryWindow.minWindowDays(), //
							timestamp));
				}
				return;
			}
		}

		throw new TrainingException(TrainingError.INSUFFICIENT_TRAINING_DATA, String.format(
				"Channel data too sparse: Need valid data since %s (min %d days), but no valid data found at all", //
				to.minusDays(queryWindow.minWindowDays()), //
				queryWindow.minWindowDays()));
	}
}
