package io.openems.backend.timedata.influx;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.BadRequestException;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.EdgeCache;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.InfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"service.ranking:Integer=1" // ranking order (highest first)
		} //
)
public class Influx extends AbstractOpenemsBackendComponent implements Timedata {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(Influx.class);
	private final ConcurrentHashMap<String, EdgeCache> edgeCacheMap = new ConcurrentHashMap<>();
	private final FieldTypeConflictHandler fieldTypeConflictHandler;

	private InfluxConnector influxConnector = null;

	public Influx() {
		super("Timedata.InfluxDB");
		this.fieldTypeConflictHandler = new FieldTypeConflictHandler(this);
	}

	@Reference
	protected volatile Metadata metadata;

	@Activate
	private void activate(Config config) throws OpenemsException, IllegalArgumentException {
		this.logInfo(this.log, "Activate [" //
				+ "url=" + config.url() + ";"//
				+ "bucket=" + config.bucket() + ";"//
				+ "apiKey=" + (config.apiKey() != null ? "ok" : "NOT_SET") + ";"//
				+ "measurement=" + config.measurement() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.influxConnector = new InfluxConnector(URI.create(config.url()), config.org(), config.apiKey(),
				config.bucket(), config.isReadOnly(), //
				(throwable) -> {
					if (throwable instanceof BadRequestException) {
						this.fieldTypeConflictHandler.handleException((BadRequestException) throwable);

					} else {
						this.logError(this.log, "Unable to write to InfluxDB. " + throwable.getClass().getSimpleName()
								+ ": " + throwable.getMessage());
					}
				});
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		if (this.influxConnector != null) {
			this.influxConnector.deactivate();
		}
	}

	@Override
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException {
		// parse the numeric EdgeId
		int influxEdgeId = Influx.parseNumberFromName(edgeId);

		// get existing or create new DeviceCache
		var edgeCache = this.edgeCacheMap.computeIfAbsent(edgeId, (ignore) -> new EdgeCache());

		// Complement incoming data with data from Cache, because only changed values
		// are transmitted
		edgeCache.complementDataFromCache(edgeId, data.rowMap());

		// Write data to default location
		this.writeData(influxEdgeId, data);
	}

	/**
	 * Actually writes the data to InfluxDB.
	 *
	 * @param influxEdgeId the unique, numeric identifier of the Edge
	 * @param data         the data
	 * @throws OpenemsException on error
	 */
	private void writeData(int influxEdgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) {
		var dataEntries = data.rowMap().entrySet();
		if (dataEntries.isEmpty()) {
			// no data to write
			return;
		}

		for (Entry<Long, Map<ChannelAddress, JsonElement>> dataEntry : dataEntries) {
			var channelEntries = dataEntry.getValue().entrySet();
			if (channelEntries.isEmpty()) {
				// no points to add
				continue;
			}

			var timestamp = dataEntry.getKey();
			// this builds an InfluxDB record ("point") for a given timestamp
			var point = Point //
					.measurement(InfluxConnector.MEASUREMENT) //
					.addTag(OpenemsOEM.INFLUXDB_TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp, WritePrecision.MS);
			for (Entry<ChannelAddress, JsonElement> channelEntry : channelEntries) {
				this.addValue(point, channelEntry.getKey().toString(), channelEntry.getValue());
			}
			if (point.hasFields()) {
				this.influxConnector.write(point);
			}
		}
	}

	/**
	 * Parses the number of an Edge from its name string.
	 *
	 * <p>
	 * e.g. translates "edge0" to "0".
	 *
	 * @param name the edge name
	 * @return the number
	 * @throws OpenemsException on error
	 */
	public static Integer parseNumberFromName(String name) throws OpenemsException {
		try {
			var matcher = Influx.NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				var nameNumberString = matcher.group(1);
				return Integer.parseInt(nameNumberString);
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		throw new OpenemsException("Unable to parse number from name [" + name + "]");
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(Influx.parseNumberFromName(edgeId));

		return this.influxConnector.queryHistoricData(influxEdgeId, fromDate, toDate, channels, resolution);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(Influx.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergy(influxEdgeId, fromDate, toDate, channels);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(Influx.parseNumberFromName(edgeId));

		return this.influxConnector.queryHistoricEnergyPerPeriod(influxEdgeId, fromDate, toDate, channels, resolution);
	}

	/**
	 * Adds the value in the correct data format for InfluxDB.
	 *
	 * @param builder the Influx PointBuilder
	 * @param field   the field name
	 * @param element the value
	 */
	private void addValue(Point builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull() || this.specialCaseFieldHandling(builder, field, element)) {
			// already handled by special case handling
			return;
		}
		if (element.isJsonPrimitive()) {
			var value = element.getAsJsonPrimitive();
			if (value.isNumber()) {
				try {
					builder.addField(field, Long.parseLong(value.toString()));
				} catch (NumberFormatException e1) {
					try {
						builder.addField(field, Double.parseDouble(value.toString()));
					} catch (NumberFormatException e2) {
						builder.addField(field, value.getAsNumber());
					}
				}
			} else if (value.isBoolean()) {
				builder.addField(field, value.getAsBoolean());
			} else if (value.isString()) {
				builder.addField(field, value.getAsString());
			} else {
				builder.addField(field, value.toString());
			}
		} else {
			builder.addField(field, element.toString());
		}
	}

	/**
	 * Handles some special cases for fields.
	 *
	 * <p>
	 * E.g. to avoid errors like "field type conflict: input field XYZ on
	 * measurement "data" is type integer, already exists as type string"
	 *
	 * @param builder the InfluxDB Builder
	 * @param field   the fieldName, i.e. the ChannelAddress
	 * @param value   the value, guaranteed to be not-null and not JsonNull.
	 * @return true if field was handled; false otherwise
	 */
	private boolean specialCaseFieldHandling(Point builder, String field, JsonElement value) {
		var handler = this.fieldTypeConflictHandler.getHandler(field);
		if (handler == null) {
			// no special handling exists for this field
			return false;
		}
		// call special handler
		handler.accept(builder, value);
		return true;
	}

	@Override
	public Optional<JsonElement> getChannelValue(String edgeId, ChannelAddress address) {
		var cache = this.edgeCacheMap.get(edgeId);
		if (cache == null) {
			return Optional.empty();
		}
		var value = cache.getChannelValue(address);
		if (value.isPresent()) {
			return value;
		}
		return Optional.empty();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

}
