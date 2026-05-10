package io.openems.shared.influxdb.proxy;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector.InfluxConnection;
import io.openems.shared.influxdb.QueryLanguageConfig;

public abstract class QueryProxy {

	public static final String CHANNEL_TAG = "channel";

	/**
	 * Builds a {@link QueryProxy} from a {@link QueryLanguageConfig}.
	 * 
	 * @param config a {@link QueryLanguageConfig}
	 * @param tag    the InfluxDB tag
	 * @return a {@link QueryProxy} instance
	 */
	public static QueryProxy from(QueryLanguageConfig config, String tag) {
		return switch (config) {
		case FLUX -> flux(tag);
		case INFLUX_QL -> influxQl(tag);
		};
	}

	/**
	 * Builds a {@link FluxProxy}.
	 * 
	 * @param tag the InfluxDB tag
	 * @return a FluxProxy
	 */
	public static QueryProxy flux(String tag) {
		return new FluxProxy(tag);
	}

	/**
	 * Builds a {@link InfluxQlProxy}.
	 * 
	 * @param tag the InfluxDB tag
	 * @return a InfluxQlProxy
	 */
	public static QueryProxy influxQl(String tag) {
		return new InfluxQlProxy(tag);
	}

	protected final String tag;

	protected QueryProxy(String tag) {
		this.tag = tag;
	}

	/**
	 * {@link CommonTimedataService#queryHistoricEnergy(String, ZonedDateTime, ZonedDateTime, Set)}.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param measurement      the influx measurement
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException;

	/**
	 * Queries the historic energy values with a measurement which only has one
	 * value saved per day.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param measurement      the influx measurement
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress ChannelAddresses}
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ChannelAddress, JsonElement> queryHistoricEnergySingleValueInDay(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException;

	/**
	 * {@link CommonTimedataService#queryHistoricData(String, io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest)}.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param measurement      the influx measurement
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @param resolution       the {@link Resolution}
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException;

	/**
	 * {@link CommonTimedataService#queryHistoricEnergyPerPeriod(String, ZonedDateTime, ZonedDateTime, Set, Resolution)}.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param measurement      the influx measurement
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @param resolution       the {@link Resolution}
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException;

	/**
	 * Queries the raw historic values without calculating the difference between
	 * two values also includes the first value before the time range to help
	 * calculating the differences.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param measurement      the influx measurement
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @param resolution       the {@link Resolution}
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryRawHistoricEnergyPerPeriodSingleValueInDay(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException;

	/**
	 * Queries the first values before the given date.
	 * 
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param influxConnection a Influx-Connection
	 * @param measurement      the influx measurement
	 * @param influxEdgeId     the Edge-ID
	 * @param date             the bounding date exclusive
	 * @param channels         the {@link ChannelAddress ChannelAddresses}
	 * @return the values
	 */
	public abstract SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(//
			String bucket, //
			InfluxConnection influxConnection, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException;

	public static final class RandomLimit {
		private static final double MAX_LIMIT = 0.95;
		private static final double MIN_LIMIT = 0;
		private static final double STEP_UP = 0.10;
		private static final double STEP_DOWN = 0.01;

		private double probability = 0;

		/**
		 * Increases the current probability by 10% up to a maximum of 95%.
		 */
		public synchronized void increase() {
			this.probability = Math.min(this.probability + STEP_UP, MAX_LIMIT);
		}

		/**
		 * Decreases the current probability by 1% down to a minimum of 0%.
		 */
		public synchronized void decrease() {
			this.probability = Math.max(this.probability - STEP_DOWN, MIN_LIMIT);
		}

		/**
		 * Checks if the limit is reached based on the current probability.
		 * @return true if the limit is reached, false otherwise
		 */
		public synchronized boolean limitReached() {
			return ThreadLocalRandom.current().nextDouble() < this.probability;
		}

		@Override
		public String toString() {
			return String.format("%.3f", this.probability);
		}
	}

	public final RandomLimit queryLimit = new RandomLimit();

	public boolean isLimitReached() {
		return this.queryLimit.limitReached();
	}

	protected void assertQueryLimit() throws OpenemsException {
		if (this.isLimitReached()) {
			throw new OpenemsException("InfluxDB read is temporarily blocked [" + this.queryLimit + "].");
		}
	}

	// TODO refactor to single parameter
	protected abstract String buildHistoricDataQuery(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsException;

	protected abstract String buildHistoricEnergyQuery(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsException;

	protected abstract String buildHistoricEnergyQuerySingleValueInDay(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsException;

	protected abstract String buildHistoricEnergyPerPeriodQuery(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsException;

	protected abstract String buildHistoricEnergyPerPeriodQuerySingleValueInDay(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsException;

	protected abstract String buildFetchFirstValueBefore(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels //
	);

}
