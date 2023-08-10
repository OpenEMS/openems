package io.openems.shared.influxdb.proxy;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

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
	public static final String AVAILABLE_SINCE_MEASUREMENT = "availableSince";
	public static final String AVAILABLE_SINCE_COLUMN_NAME = "available_since";

	/**
	 * Builds a {@link QueryProxy} from a {@link QueryLanguageConfig}.
	 * 
	 * @param config a {@link QueryLanguageConfig}
	 * @return a {@link QueryProxy} instance
	 */
	public static QueryProxy from(QueryLanguageConfig config) {
		switch (config) {
		case FLUX:
			return flux();
		case INFLUX_QL:
			return influxQl();
		}
		// Will never happen
		return null;
	}

	/**
	 * Builds a {@link FluxProxy}.
	 * 
	 * @return a FluxProxy
	 */
	public static QueryProxy flux() {
		return new FluxProxy();
	}

	/**
	 * Builds a {@link InfluxQlProxy}.
	 * 
	 * @return a InfluxQlProxy
	 */
	public static QueryProxy influxQl() {
		return new InfluxQlProxy();
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
	 * Queries the available since fields from the database.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @return the map where the key is the edgeId and the value the timestamp of
	 *         available since
	 */
	public abstract Map<Integer, Map<String, Long>> queryAvailableSince(//
			InfluxConnection influxConnection, //
			String bucket //
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

	public static class RandomLimit {
		private static final double MAX_LIMIT = 0.95;
		private static final double MIN_LIMIT = 0;
		private static final double STEP_UP = 0.10;
		private static final double STEP_DOWN = 0.01;

		private double limit = 0;

		/**
		 * Increases the current limit.
		 */
		public synchronized void increase() {
			this.limit += STEP_UP;
			if (this.limit > MAX_LIMIT) {
				this.limit = MAX_LIMIT;
			}
		}

		/**
		 * Decreases the current limit.
		 */
		public synchronized void decrease() {
			this.limit -= STEP_DOWN;
			if (this.limit <= MIN_LIMIT) {
				this.limit = MIN_LIMIT;
			}
		}

		protected double getLimit() {
			return this.limit;
		}

		@Override
		public String toString() {
			return String.format("%.3f", this.limit);
		}
	}

	public final RandomLimit queryLimit = new RandomLimit();

	public boolean isLimitReached() {
		return Math.random() < this.queryLimit.getLimit();
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

	protected abstract String buildFetchAvailableSinceQuery(//
			String bucket //
	);

	protected abstract String buildFetchFirstValueBefore(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels //
	);

}
