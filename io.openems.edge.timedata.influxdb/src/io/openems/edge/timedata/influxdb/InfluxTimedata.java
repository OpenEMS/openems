package io.openems.edge.timedata.influxdb;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;

/**
 * Provides read and write access to InfluxDB.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Timedata.InfluxDB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class InfluxTimedata extends AbstractOpenemsComponent implements Timedata, OpenemsComponent, EventHandler {

	protected final static String MEASUREMENT = "data";

	private final Logger log = LoggerFactory.getLogger(InfluxTimedata.class);

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * FAULT: Unable to connect to InfluxDB
		 */
		STATE_0(new Doc().level(Level.FAULT).text("Unable to connect to InfluxDB (write)")), // refers to _influxDB1
		STATE_1(new Doc().level(Level.FAULT).text("Unable to connect to InfluxDB (read)")); // refers to _influxDB2

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private String ip;
	private int port;
	private String username;
	private String password;
	private String database;
	private InfluxDB _influxDB = null;

	public InfluxTimedata() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.AT_LEAST_ONE, //
			target = "(&(enabled=true)(!(service.factoryPid=Timedata.InfluxDB)))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.ip = config.ip();
		this.port = config.port();
		this.username = config.username();
		this.password = config.password();
		this.database = config.database();
		if (config.enabled()) {
			try {
				this.getConnection();
			} catch (OpenemsException e) {
				logWarn(this.log, e.getMessage());
			}
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this._influxDB != null) {
			this._influxDB.close();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.collectAndWriteChannelValues();
			break;
		}
	}

	/**
	 * Get InfluxDB Connection
	 * 
	 * @return
	 */
	protected InfluxDB getConnection() throws OpenemsException {
		if (this._influxDB == null) {
			try {
				InfluxDB influxDB = InfluxDBFactory.connect("http://" + this.ip + ":" + this.port, this.username,
						this.password);
				influxDB.setDatabase(this.database);
				influxDB.enableBatch(BatchOptions.DEFAULTS);
				this._influxDB = influxDB;
				this.channel(ChannelId.STATE_1).setNextValue(false);
			} catch (RuntimeException e) {
				this.channel(ChannelId.STATE_1).setNextValue(true);
				throw new OpenemsException("Unable to connect to InfluxDB (read): " + e.getMessage(), e);
			}
		}
		return this._influxDB;
	}

	/**
	 * copied from backend.timedata.influx.provider
	 * 
	 * @param influxdb
	 * @param database
	 * @param query
	 * @return
	 * @throws OpenemsException
	 */
	private QueryResult executeQuery(String query) throws OpenemsException {
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
	 * copied from backend.timedata.influx.provider
	 * 
	 * @param influxdb
	 * @param database
	 * @param influxIdOpt
	 * @param fromDate
	 * @param toDate
	 * @param channels
	 * @param resolution
	 * @return
	 * @throws OpenemsException
	 */
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution) throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(Utils.toChannelAddressList(channels));
		query.append(" FROM data WHERE ");
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s) fill(null)");

		QueryResult queryResult = executeQuery(query.toString());

		JsonArray j = new JsonArray();
		for (Result result : queryResult.getResults()) {
			List<Series> seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// create thing/channel index
					ArrayList<ChannelAddress> addressIndex = new ArrayList<>();
					for (String column : series.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column));
					}
					// first: create empty timestamp objects
					for (List<Object> values : series.getValues()) {
						JsonObject jTimestamp = new JsonObject();
						// get timestamp
						Instant timestampInstant = Instant.ofEpochMilli((long) ((Double) values.get(0)).doubleValue());
						ZonedDateTime timestamp = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
						String timestampString = timestamp.format(DateTimeFormatter.ISO_INSTANT);
						jTimestamp.addProperty("time", timestampString);
						// add empty channels by copying "channels" parameter
						JsonObject jChannels = new JsonObject();
						for (Entry<String, JsonElement> entry : channels.entrySet()) {
							String thingId = entry.getKey();
							JsonObject jThing = new JsonObject();
							JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
							for (JsonElement channelElement : channelIds) {
								String channelId = JsonUtils.getAsString(channelElement);
								jThing.add(channelId, JsonNull.INSTANCE);
							}
							jChannels.add(thingId, jThing);
						}
						jTimestamp.add("channels", jChannels);
						j.add(jTimestamp);
					}
					// then: add all data
					for (int columnIndex = 1; columnIndex < series.getColumns().size(); columnIndex++) {
						for (int timeIndex = 0; timeIndex < series.getValues().size(); timeIndex++) {
							Double value = (Double) series.getValues().get(timeIndex).get(columnIndex);
							ChannelAddress address = addressIndex.get(columnIndex - 1);
							j.get(timeIndex).getAsJsonObject().get("channels").getAsJsonObject()
									.get(address.getComponentId()).getAsJsonObject()
									.addProperty(address.getChannelId(), value);
						}
					}
				}
			}
		}
		return j;
	}

	protected synchronized void collectAndWriteChannelValues() {
		InfluxDB influxDB;
		try {
			influxDB = this.getConnection();
		} catch (OpenemsException e) {
			this.log.error("Not perisisting any data: " + e.getMessage());
			return;
		}

		long timestamp = System.currentTimeMillis() / 1000;
		final Builder point = Point.measurement(InfluxTimedata.MEASUREMENT).time(timestamp, TimeUnit.SECONDS);
		final AtomicBoolean addedAtLeastOneChannelValue = new AtomicBoolean(false);

		this.components.stream().filter(c -> c.isEnabled()).forEach(component -> {
			component.channels().forEach(channel -> {
				Optional<?> valueOpt = channel.value().asOptional();
				if (!valueOpt.isPresent()) {
					// ignore not available channels
					return;
				}
				Object value = valueOpt.get();
				String address = channel.address().toString();
				try {
					switch (channel.getType()) {
					case BOOLEAN:
						point.addField(address, (Boolean) value);
						break;
					case FLOAT:
						point.addField(address, (Float) value);
						break;
					case INTEGER:
						point.addField(address, (Integer) value);
						break;
					case LONG:
						point.addField(address, (Long) value);
						break;
					case SHORT:
						point.addField(address, (Short) value);
						break;
					case STRING:
						point.addField(address, (String) value);
						break;
					}
				} catch (IllegalArgumentException e) {
					this.log.warn("Unable to add Channel [" + address + "] value [" + value + "]: " + e.getMessage());
					return;
				}
				addedAtLeastOneChannelValue.set(true);
			});
		});

		if (addedAtLeastOneChannelValue.get()) {
			influxDB.write(point.build());
		}
	}
}
