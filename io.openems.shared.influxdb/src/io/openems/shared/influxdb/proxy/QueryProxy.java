package io.openems.shared.influxdb.proxy;

import java.time.ZonedDateTime;
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

	/**
	 * Builds a {@link QueryProxy} from a {@link QueryLanguageConfig}.
	 * 
	 * @param config a {@link QueryLanguageConfig}
	 * @return a {@link QueryProxy} instance
	 */
	public static QueryProxy from(QueryLanguageConfig config) {
		return switch (config) {
	       case FLUX -> flux();
	       case INFLUX_QL -> influxQl();	       
	       default -> null; // Will never happen
	};
	
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
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(InfluxConnection influxConnection,
			String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
			Set<ChannelAddress> channels) throws OpenemsNamedException;

	/**
	 * {@link CommonTimedataService#queryHistoricData(String, io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest)}.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @param resolution       the {@link Resolution}
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(
			InfluxConnection influxConnection, String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsNamedException;

	/**
	 * {@link CommonTimedataService#queryHistoricEnergyPerPeriod(String, ZonedDateTime, ZonedDateTime, Set, Resolution)}.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param bucket           the bucket name; 'database/retentionPolicy' for
	 *                         InfluxDB v1
	 * @param influxEdgeId     the Edge-ID
	 * @param fromDate         the From-Date
	 * @param toDate           the To-Date
	 * @param channels         the {@link ChannelAddress}es
	 * @param resolution       the {@link Resolution}
	 * @return the query result
	 * @throws OpenemsNamedException on error
	 */
	public abstract SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(
			InfluxConnection influxConnection, String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsNamedException;

	protected static class RandomLimit {
		private static final double MAX_LIMIT = 0.95;
		private static final double MIN_LIMIT = 0;
		private static final double STEP = 0.01;

		private double limit = 0;

		protected synchronized void increase() {
			this.limit += STEP;
			if (this.limit > MAX_LIMIT) {
				this.limit = MAX_LIMIT;
			}
		}

		protected synchronized void decrease() {
			this.limit -= STEP;
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

	protected final RandomLimit queryLimit = new RandomLimit();

	protected void assertQueryLimit() throws OpenemsException {
		if (Math.random() < this.queryLimit.getLimit()) {
			throw new OpenemsException("InfluxDB read is temporarily blocked [" + this.queryLimit + "].");
		}
	}

	protected abstract String buildHistoricDataQuery(String bucket, Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsException;

	protected abstract String buildHistoricEnergyQuery(String bucket, Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsException;

	protected abstract String buildHistoricEnergyPerPeriodQuery(String bucket, Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsException;

}
