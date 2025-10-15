package io.openems.edge.predictor.profileclusteringmodel.services;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.function.Supplier;

import com.google.common.collect.Sets;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.timedata.api.Timedata;

public class RawTimeSeriesService {

	private static final int MINUTES_PER_QUARTER = 15;

	private final Timedata timedata;
	private final Supplier<Clock> clockSupplier;
	private final ChannelAddress channelAddress;

	public RawTimeSeriesService(//
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
	 * @throws OpenemsNamedException if fetching fails or data validation fails
	 */
	public Series<ZonedDateTime> fetchSeriesForWindow(QueryWindow queryWindow) throws OpenemsNamedException {
		var to = ZonedDateTime.now(this.clockSupplier.get()).truncatedTo(ChronoUnit.DAYS);
		var from = to.minus(queryWindow.maxWindowDays(), ChronoUnit.DAYS);
		var series = this.fetchSeries(from, to);

		this.ensureMinCoverage(series, to, queryWindow);

		return series;
	}

	/**
	 * Fetches a time series from the start of today until now.
	 *
	 * @return the fetched time series
	 * @throws OpenemsNamedException if fetching fails
	 */
	public Series<ZonedDateTime> fetchSeriesForToday() throws OpenemsNamedException {
		var now = ZonedDateTime.now(this.clockSupplier.get());
		var from = now.truncatedTo(ChronoUnit.DAYS);

		return this.fetchSeries(from, now);
	}

	private Series<ZonedDateTime> fetchSeries(ZonedDateTime from, ZonedDateTime to) throws OpenemsNamedException {
		var rawChannelData = this.timedata.queryHistoricData(//
				null, //
				from, //
				to, //
				Sets.newHashSet(this.channelAddress), //
				new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));

		var timestamps = new ArrayList<ZonedDateTime>();
		for (var ts = from; ts.isBefore(to); ts = ts.plusMinutes(MINUTES_PER_QUARTER)) {
			timestamps.add(ts);
		}

		var values = timestamps.stream()//
				.map(ts -> { //
					var element = rawChannelData.get(ts);
					if (element == null //
							|| element.get(this.channelAddress) == null //
							|| element.get(this.channelAddress).isJsonNull()) {
						return Double.NaN;
					}
					return element.get(this.channelAddress).getAsDouble();
				}).toList();

		return new Series<>(timestamps, values);
	}

	private void ensureMinCoverage(//
			Series<ZonedDateTime> series, //
			ZonedDateTime to, //
			QueryWindow queryWindow) {
		for (var timestamp : series.getIndex()) {
			if (!series.get(timestamp).isNaN()) {
				var minRequiredStart = to.minusDays(queryWindow.minWindowDays());
				if (timestamp.isAfter(minRequiredStart)) {
					throw new IllegalStateException(String.format(//
							"Channel data too sparse: Need valid data since %s (min %d days), but earliest valid data is %s", //
							minRequiredStart, queryWindow.minWindowDays(), timestamp));
				}
				return;
			}
		}

		throw new IllegalStateException(String.format(
				"Channel data too sparse: Need valid data since %s (min %d days), but no valid data found at all",
				to.minusDays(queryWindow.minWindowDays()), queryWindow.minWindowDays()));
	}
}
