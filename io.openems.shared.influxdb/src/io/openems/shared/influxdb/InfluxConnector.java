package io.openems.shared.influxdb;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

public class InfluxConnector {

	public final static String MEASUREMENT = "data";

	private final String ip;
	private final int port;
	private final String username;
	private final String password;
	private final String database;

	public InfluxConnector(String ip, int port, String username, String password, String database) {
		super();
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
	}

	private InfluxDB _influxDB = null;

	public String getDatabase() {
		return database;
	}

	/**
	 * Get InfluxDB Connection
	 * 
	 * @return
	 */
	public InfluxDB getConnection() {
		if (this._influxDB == null) {
			InfluxDB influxDB = InfluxDBFactory.connect("http://" + this.ip + ":" + this.port, this.username,
					this.password);
			influxDB.setDatabase(this.database);
			influxDB.enableBatch(BatchOptions.DEFAULTS);
			this._influxDB = influxDB;
		}
		return this._influxDB;
	}

	public void deactivate() {
		if (this._influxDB != null) {
			this._influxDB.close();
		}
	}

	/**
	 * copied from backend.timedata.influx.provider
	 * 
	 * @param query
	 * @return
	 * @throws OpenemsException
	 */
	public QueryResult executeQuery(String query) throws OpenemsException {
		InfluxDB influxDB = this.getConnection();

		// Parse result
		QueryResult queryResult;
		try {
			queryResult = influxDB.query(new Query(query, this.database), TimeUnit.MILLISECONDS);
		} catch (RuntimeException e) {
			throw new OpenemsException("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
		}
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error. Query: " + query + ", Error: " + queryResult.getError());
		}
		return queryResult;
	}

	/**
	 * Queries historic data.
	 * 
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @return
	 * @throws OpenemsException on error
	 */
	public TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> queryHistoricData(Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(InfluxConnector.toChannelAddressString(channels));
		query.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			query.append(InfluxConstants.TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s) fill(null)");

		// Execute query
		QueryResult queryResult = executeQuery(query.toString());

		// Prepare result
		TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> result = InfluxConnector
				.convertHistoricDataQueryResult(queryResult, fromDate.getZone());

		return result;
	}

	/**
	 * Converts the QueryResult of a Historic-Data query to a properly typed Table.
	 * 
	 * @param queryResult the Query-Result
	 * @return
	 * @throws OpenemsException on error
	 */
	private static TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> convertHistoricDataQueryResult(
			QueryResult queryResult, ZoneId timezone) throws OpenemsNamedException {
		TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> table = TreeBasedTable.create();
		for (Result result : queryResult.getResults()) {
			List<Series> seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// create ChannelAddress index
					ArrayList<ChannelAddress> addressIndex = new ArrayList<>();
					for (String column : series.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column));
					}

					// add all data
					for (List<Object> values : series.getValues()) {
						// get timestamp
						Instant timestampInstant = Instant.ofEpochMilli((long) ((Double) values.get(0)).doubleValue());
						ZonedDateTime timestamp = ZonedDateTime.ofInstant(timestampInstant, timezone);
						for (int columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
							// Note: ignoring index '0' here as it is the 'timestamp'
							ChannelAddress address = addressIndex.get(columnIndex);
							Object valueObj = values.get(columnIndex + 1);
							JsonElement value;
							if (valueObj == null) {
								value = JsonNull.INSTANCE;
							} else if (valueObj instanceof Number) {
								value = new JsonPrimitive((Number) valueObj);
							} else {
								value = new JsonPrimitive(valueObj.toString());
							}
							table.put(timestamp, address, value);
						}
					}
				}
			}
		}
		return table;
	}

	/**
	 * 
	 * @param channels
	 * @return
	 * @throws OpenemsException
	 */
	protected static String toChannelAddressString(Set<ChannelAddress> channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (ChannelAddress channel : channels) {
			channelAddresses.add("MEAN(\"" + channel.toString() + "\") AS \"" + channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}
}
