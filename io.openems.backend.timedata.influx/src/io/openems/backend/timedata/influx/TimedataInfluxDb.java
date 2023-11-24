package io.openems.backend.timedata.influx;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.OpenemsOEM;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.InfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_ONLINE //
})
public class TimedataInfluxDb extends AbstractOpenemsBackendComponent implements Timedata, EventHandler, DebugLoggable {

	private final Logger log = LoggerFactory.getLogger(TimedataInfluxDb.class);
	private final FieldTypeConflictHandler fieldTypeConflictHandler;

	@Reference
	protected volatile Metadata metadata;

	private Config config;
	private InfluxConnector influxConnector = null;
	private TimeFilter timeFilter;

	// edgeId, channelIds which are timestamped channels
	private final Multimap<Integer, String> timestampedChannelsForEdge = HashMultimap.create();

	public TimedataInfluxDb() {
		super("Timedata.InfluxDB");
		this.fieldTypeConflictHandler = new FieldTypeConflictHandler(this);
	}

	@Activate
	private void activate(Config config) throws OpenemsException, IllegalArgumentException {
		this.config = config;
		this.timeFilter = TimeFilter.from(config.startDate(), config.endDate());

		this.logInfo(this.log, "Activate [" //
				+ "url=" + config.url() + ";"//
				+ "bucket=" + config.bucket() + ";"//
				+ "apiKey=" + (config.apiKey() != null ? "ok" : "NOT_SET") + ";"//
				+ "measurement=" + config.measurement() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.influxConnector = new InfluxConnector(config.id(), config.queryLanguage(), URI.create(config.url()),
				config.org(), config.apiKey(), config.bucket(), config.isReadOnly(), config.poolSize(),
				config.maxQueueSize(), //
				(e) -> {
					this.fieldTypeConflictHandler.handleException(e);
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
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case Edge.Events.ON_SET_ONLINE:
			final var reader = new EventReader(event);
			final var edgeId = reader.getString(Edge.Events.OnSetOnline.EDGE_ID);
			final var isOnline = reader.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);
			if (!isOnline) {
				try {
					var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
					this.timestampedChannelsForEdge.removeAll(influxEdgeId);
				} catch (OpenemsException e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification notification) {
		if (this.config.isReadOnly()) {
			return;
		}

		// Write data to default location
		this.writeData(//
				edgeId, //
				notification, //
				(influxEdgeId, channel) -> {
					this.timestampedChannelsForEdge.put(influxEdgeId, channel);
					return true;
				});
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification notification) {
		if (this.config.isReadOnly()) {
			return;
		}

		// Write data to default location
		this.writeData(//
				edgeId, //
				notification, //
				(influxEdgeId, channel) -> !this.isTimestampedChannel(influxEdgeId, channel));
	}

	@Override
	public void write(String edgeId, ResendDataNotification data) {
		// TODO Auto-generated method stub
	}

	private boolean isTimestampedChannel(int edgeId, String channel) {
		final var channelSet = this.timestampedChannelsForEdge.get(edgeId);
		// if edge is not set the checked channel may be timestamped channel so
		// initially return true
		if (channelSet == null) {
			return true;
		}
		return channelSet.contains(channel);
	}

	/**
	 * Actually writes the data to InfluxDB.
	 *
	 * @param edgeId           the unique identifier of the Edge
	 * @param notification     the {@link AbstractDataNotification}
	 * @param shouldWriteValue the function which determines if the value should be
	 *                         written
	 * @throws OpenemsException on error
	 */
	private void writeData(//
			String edgeId, //
			AbstractDataNotification notification, //
			BiFunction<Integer, String, Boolean> shouldWriteValue //
	) {
		final int influxEdgeId;
		try {
			influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		} catch (OpenemsException e) {
			this.logWarn(this.log, "Unable to parse numeric Influx Edge-ID [" + edgeId + "] :" + e.getMessage());
			return;
		}

		final var data = notification.getData();
		var dataEntries = data.rowMap().entrySet();
		if (dataEntries.isEmpty()) {
			// no data to write
			return;
		}

		for (var dataEntry : dataEntries) {
			var channelEntries = dataEntry.getValue().entrySet();
			if (channelEntries.isEmpty()) {
				// no points to add
				continue;
			}

			var timestamp = dataEntry.getKey();

			if (!this.timeFilter.isValid(timestamp)) {
				// timestamp is not within the TimeFilter
				continue;
			}

			// this builds an InfluxDB record ("point") for a given timestamp
			var point = Point //
					.measurement(this.config.measurement()) //
					.addTag(OpenemsOEM.INFLUXDB_TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp, WritePrecision.MS);
			for (var channelEntry : channelEntries) {
				if (!shouldWriteValue.apply(influxEdgeId, channelEntry.getKey())) {
					continue;
				}
				this.addValue(//
						point, //
						channelEntry.getKey(), //
						channelEntry.getValue());
			}

			this.influxConnector.write(point);
		}
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		if (!this.timeFilter.isValid(fromDate, toDate)) {
			return null;
		}

		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(InfluxConnector.parseNumberFromName(edgeId));

		return this.influxConnector.queryHistoricData(influxEdgeId, fromDate, toDate, channels, resolution,
				this.config.measurement());
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		if (!this.timeFilter.isValid(fromDate, toDate)) {
			return null;
		}

		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(InfluxConnector.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergy(influxEdgeId, fromDate, toDate, channels,
				this.config.measurement());
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		if (!this.timeFilter.isValid(fromDate, toDate)) {
			return null;
		}

		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(InfluxConnector.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergyPerPeriod(influxEdgeId, fromDate, toDate, channels, resolution,
				this.config.measurement());
	}

	/**
	 * Adds the value in the correct data format for InfluxDB.
	 *
	 * @param builder the Influx PointBuilder
	 * @param field   the field name
	 * @param element the value
	 */
	private void addValue(Point builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull() //
				|| !isAllowed(field) // Channel-Address is not allowed/blacklisted
				// already handled by special case handling
				|| this.specialCaseFieldHandling(builder, field, element)) {
			return;
		}

		if (element.isJsonPrimitive()) {
			var p = (JsonPrimitive) element;
			if (p.isNumber()) {
				// Numbers can be directly converted
				var n = p.getAsNumber();
				if (n.getClass().getName().equals("com.google.gson.internal.LazilyParsedNumber")) {
					// Avoid 'discouraged access'
					// LazilyParsedNumber stores value internally as String
					if (StringUtils.matchesFloatPattern(n.toString())) {
						builder.addField(field, n.doubleValue());
						return;
					}
					builder.addField(field, n.longValue());
					return;

				} else if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
					builder.addField(field, n.longValue());
					return;

				}
				builder.addField(field, n.doubleValue());
				return;

			} else if (p.isBoolean()) {
				// Booleans are converted to integer (0/1)
				builder.addField(field, p.getAsBoolean());
				return;

			} else if (p.isString()) {
				// Strings are parsed if they start with a number or minus
				var s = p.getAsString();
				if (StringUtils.matchesFloatPattern(s)) {
					try {
						builder.addField(field, Double.parseDouble(s)); // try parsing to Double
						return;
					} catch (NumberFormatException e) {
						builder.addField(field, s);
						return;
					}

				} else if (StringUtils.matchesIntegerPattern(s)) {
					try {
						builder.addField(field, Long.parseLong(s)); // try parsing to Long
						return;
					} catch (NumberFormatException e) {
						builder.addField(field, s);
						return;
					}
				}
				builder.addField(field, s);
				return;
			}

		} else {
			builder.addField(field, element.toString());
			return;
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
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public String id() {
		return this.config.id();
	}

	private static final Predicate<String> SUNSPEC_PATTERN = //
			Pattern.compile("^S[0-9]+[A-Z][a-zA-Z0-9]*$").asPredicate();

	/**
	 * Pattern for Component-IDs.
	 * 
	 * <p>
	 * Either:
	 * 
	 * <ul>
	 * <li>starts with lower case letter
	 * <li>contains only ASCII letters and numbers
	 * <li>ends with a number
	 * </ul>
	 * 
	 * <p>
	 * Or:
	 * <ul>
	 * <li>starts with underscore (by convention for singleton Components)
	 * <li>continues with lower case letter
	 * <li>contains only ASCII letters and numbers
	 * <li>ends with a letter
	 * </ul>
	 */
	// TODO move to io.openems.common and validate pattern on Edge
	private static final Predicate<String> COMPONENT_ID_PATTERN = //
			Pattern.compile("^([a-z][a-zA-Z0-9]+[0-9]+|_[a-z][a-zA-Z0-9]+[a-zA-Z])$").asPredicate();

	protected static boolean isAllowed(String channelAddress) {
		if (channelAddress == null) {
			return false;
		}

		var c = channelAddress.split("/");
		if (c.length != 2) {
			return false;
		}

		// Valid Component-ID
		var componentId = c[0];
		if (!COMPONENT_ID_PATTERN.test(componentId)) {
			return false;
		}

		// Valid Channel-ID
		var channelId = c[1];
		if (SUNSPEC_PATTERN.test(channelId)) {
			// SunSpec Channels
			return false;
		}

		return true;
	}

	@Override
	public String debugLog() {
		return "[" + this.getName() + "] " + this.config.id() + " " + this.influxConnector.debugLog();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		return null;
	}

}
