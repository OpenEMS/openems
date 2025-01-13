package io.openems.backend.timedata.aggregatedinflux;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.influxdb.client.domain.WriteConsistency;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.timedata.InternalTimedataException;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.timedata.aggregatedinflux.AllowedChannels.ChannelType;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.oem.OpenemsBackendOem;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;
import io.openems.shared.influxdb.DbDataUtils;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.AggregatedInfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		Edge.Events.ON_SET_VERSION //
})
public class AggregatedInflux extends AbstractOpenemsBackendComponent implements Timedata, DebugLoggable, EventHandler {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private WriteParameters writeParametersAvgPoints;
	private WriteParameters writeParametersMaxPoints;

	private Config config;
	private InfluxConnector influxConnector;

	private final Map<ZoneId, String> zoneToMeasurement = new HashMap<>();

	// map from edgeId to channelName and availableSince time stamp
	private final Map<Integer, Map<String, Long>> availableSinceForEdge = new ConcurrentHashMap<>();

	@Reference
	private OpenemsBackendOem oem;

	@Reference(//
			cardinality = ReferenceCardinality.OPTIONAL, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private volatile QueryWithCurrentData queryWithCurrentData;

	public AggregatedInflux() {
		super("AggregatedInflux");
	}

	@Activate
	private void activate(Config config) throws OpenemsNamedException, IllegalArgumentException {
		this.config = config;
		this.logInfo(this.log, "Activate [" //
				+ "url=" + config.url() + ";"//
				+ "bucket=" + config.bucket() + ";"//
				+ "apiKey=" + (config.apiKey() != null ? "ok" : "NOT_SET") + ";"//
				+ "measurementAvg=" + config.measurementAvg() //
				+ "measurementMax=" + config.measurementsMax() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.zoneToMeasurement.clear();
		this.zoneToMeasurement.putAll(parseMeasurementsByZone(config.measurementsMax()));

		this.writeParametersAvgPoints = new WriteParameters(config.bucket() + "/" + config.retentionPolicyAvg(),
				config.org(), WritePrecision.S, WriteConsistency.ALL);
		this.writeParametersMaxPoints = new WriteParameters(config.bucket() + "/" + config.retentionPolicyMax(),
				config.org(), WritePrecision.S, WriteConsistency.ALL);

		this.influxConnector = new InfluxConnector(config.id(), config.queryLanguage(), URI.create(config.url()),
				config.org(), config.apiKey(), config.bucket(), this.oem.getInfluxdbTag(), config.isReadOnly(),
				config.poolSize(), config.maxQueueSize(), //
				(throwable) -> {
					this.logError(this.log, "Unable to write to InfluxDB. " + throwable.getClass().getSimpleName()
							+ ": " + throwable.getMessage());
				}, true /* enable safe write */, this.writeParametersAvgPoints, this.writeParametersMaxPoints);

		// load available since for edges which already wrote in the new database
		this.availableSinceForEdge.clear();
		this.availableSinceForEdge.putAll(this.influxConnector.queryAvailableSince().entrySet().stream().map(entry -> {
			return Map.entry(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
		}).collect(toMap(Entry::getKey, Entry::getValue)));
	}

	@Deactivate
	private void deactivate() {
		this.influxConnector.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		final var reader = new EventReader(event);
		switch (event.getTopic()) {
		case Edge.Events.ON_SET_VERSION -> {
			this.handleEdgeVersionUpdateEvent(//
					reader.<Edge>getProperty(Edge.Events.OnSetVersion.EDGE).getId(), //
					reader.getProperty(Edge.Events.OnSetVersion.OLD_VERSION), //
					reader.getProperty(Edge.Events.OnSetVersion.VERSION) //
			);
		}
		}
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			String edgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);

		this.checkDataAvailable(influxEdgeId, fromDate, channels);

		return this.influxConnector.queryHistoricData(Optional.of(influxEdgeId), //
				fromDate, toDate, channels, resolution,
				this.config.retentionPolicyAvg() + "." + this.config.measurementAvg());
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			String edgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		this.checkDataAvailable(influxEdgeId, fromDate, channels);

		if (isTodayOrAfter(toDate)) {
			if (this.queryWithCurrentData == null) {
				throw new InternalTimedataException("Missing 'queryWithCurrentData' object");
			}
			return this.queryWithCurrentData.queryHistoricEnergy(edgeId, fromDate, toDate, channels);
		}
		final var measurement = this.getMeasurement(fromDate.getZone());
		return this.influxConnector.queryHistoricEnergySingleValueInDay(Optional.of(influxEdgeId), //
				fromDate, toDate, channels, this.config.retentionPolicyMax() + "." + measurement);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			String edgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {
		// parse the numeric EdgeId
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		this.checkDataAvailable(influxEdgeId, fromDate, channels);
		final var measurement = this.getMeasurement(fromDate.getZone());
		final var rawData = this.influxConnector.queryRawHistoricEnergyPerPeriodSinglePerDay(Optional.of(influxEdgeId), //
				fromDate, toDate, channels, resolution, this.config.retentionPolicyMax() + "." + measurement);
		if (isTodayOrAfter(toDate)) {
			if (this.queryWithCurrentData == null) {
				throw new InternalTimedataException("Missing 'queryWithCurrentData' object");
			}
			return this.queryWithCurrentData.queryHistoricEnergyPerPeriod(edgeId, fromDate, toDate, channels,
					resolution, rawData);
		}
		final var result = DbDataUtils.calculateLastMinusFirst(rawData, fromDate);
		return DbDataUtils.normalizeTable(result, channels, resolution, fromDate, toDate);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(//
			final String edgeId, //
			final ZonedDateTime date, //
			final Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		final var measurement = this.getMeasurement(date.getZone());
		// TODO determine measurement based on channel or separate in two methods
		return this.influxConnector.queryFirstValueBefore(Optional.of(influxEdgeId), //
				date, channels, this.config.retentionPolicyMax() + "." + measurement);
	}

	@Override
	public String id() {
		return this.config.id();
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification notification) {
		this.writeNotificationData(edgeId, notification);
	}

	@Override
	public void write(String edgeId, ResendDataNotification notification) {
		this.writeNotificationData(edgeId, notification);
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification notification) {
		// empty
	}

	private void writeNotificationData(String edgeId, AbstractDataNotification notification) {
		final var data = notification.getData().rowMap();
		if (data.isEmpty()) {
			return;
		}
		int influxEdgeId;
		try {
			influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		} catch (OpenemsException e) {
			e.printStackTrace();
			return;
		}

		for (var dataEntry : data.entrySet()) {
			var channelEntries = dataEntry.getValue().entrySet();
			if (channelEntries.isEmpty()) {
				// no points to add
				continue;
			}

			final var channelPerType = channelEntries.stream() //
					.collect(groupingBy(//
							entry -> AllowedChannels.getChannelType(entry.getKey()), //
							() -> new EnumMap<>(ChannelType.class), //
							toList() //
					));

			final var timestamp = dataEntry.getKey();
			final var timestampSeconds = timestamp / 1_000;
			record AddValuesToPoint(String channel, JsonElement value) {
			}

			// update available-since values
			channelPerType.getOrDefault(ChannelType.AVG, emptyList()).forEach(entry -> {
				this.setMissingAvailableSince(influxEdgeId, entry.getKey(), timestampSeconds);
			});
			channelPerType.getOrDefault(ChannelType.MAX, emptyList()).forEach(entry -> {
				this.setMissingAvailableSince(influxEdgeId, entry.getKey(), timestampSeconds - 86400 /* 1 day */);
			});

			BiConsumer<AddValuesToPoint, Point> addEntryToPoint = (entry, point) -> {
				AllowedChannels.addWithSpecificChannelType(point, entry.channel(), entry.value());
			};

			final var point = Point //
					.measurement(this.config.measurementAvg()) //
					.addTag(this.oem.getInfluxdbTag(), String.valueOf(influxEdgeId)) //
					.time(timestampSeconds, WritePrecision.S);

			channelPerType.getOrDefault(ChannelType.AVG, emptyList()).stream() //
					.forEach(entry -> addEntryToPoint.accept(new AddValuesToPoint(entry.getKey(), entry.getValue()),
							point));
			this.influxConnector.write(point, this.writeParametersAvgPoints);

			for (final var measurementEntry : this.getDayChangeMeasurements(timestamp).entrySet()) {
				final var zonedDateTime = ZonedDateTime
						.ofInstant(Instant.ofEpochMilli(timestamp), measurementEntry.getKey()) //
						.truncatedTo(DurationUnit.ofDays(1));
				final var truncatedTimestamp = zonedDateTime.toEpochSecond();

				final var maxPoint = Point //
						.measurement(measurementEntry.getValue()) //
						.addTag(this.oem.getInfluxdbTag(), String.valueOf(influxEdgeId)) //
						.time(truncatedTimestamp, WritePrecision.S);

				channelPerType.getOrDefault(ChannelType.MAX, emptyList()).stream() //
						.forEach(entry -> addEntryToPoint.accept(new AddValuesToPoint(entry.getKey(), entry.getValue()),
								maxPoint));
				this.influxConnector.write(maxPoint, this.writeParametersMaxPoints);
			}
		}
	}

	private final void setMissingAvailableSince(int influxEdgeId, String channel, long timestmap) {
		if (this.hasAvailableSince(influxEdgeId, channel)) {
			return;
		}
		this.setAvailableSince(influxEdgeId, channel, timestmap);
	}

	private Map<ZoneId, String> getDayChangeMeasurements(long timestamp) {
		final var instant = Instant.ofEpochMilli(timestamp);
		return this.zoneToMeasurement.entrySet().stream() //
				.filter(entry -> {
					final var time = instant.atZone(entry.getKey());
					return time.getHour() == 23 && time.getMinute() >= 55;
				}) //
				.collect(toMap(Entry::getKey, Entry::getValue));
	}

	private void setAvailableSince(int influxEdgeId, String channelName, long availableSince) {
		this.influxConnector.write(InfluxConnector.buildUpdateAvailableSincePoint(this.oem.getInfluxdbTag(),
				influxEdgeId, channelName, availableSince));
		this.availableSinceForEdge.computeIfAbsent(influxEdgeId, key -> new ConcurrentHashMap<>()) //
				.put(channelName, availableSince);
	}

	private void checkDataAvailable(//
			int influxEdgeId, //
			ZonedDateTime time, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		final var errorMessage = this.checkDataAvailableOrErrorMessage(influxEdgeId, time, channels);
		if (errorMessage == null) {
			return;
		}
		throw new InternalTimedataException(errorMessage);
	}

	/**
	 * Checks if the data is available. If true returns null otherwise returns the
	 * reason why the data is not available.
	 * 
	 * @param influxEdgeId the id of the edge
	 * @param time         the requested time
	 * @param channels     the requested channels
	 * @return the reason why no data is available or null if the data is available
	 */
	private String checkDataAvailableOrErrorMessage(//
			int influxEdgeId, //
			ZonedDateTime time, //
			Set<ChannelAddress> channels //
	) {
		final var edgeChannels = this.availableSinceForEdge.get(influxEdgeId);
		if (edgeChannels == null) {
			return null;
		}
		final var seconds = time.toEpochSecond();
		for (var channel : channels) {
			var availableSince = edgeChannels.get(channel.toString());
			if (availableSince == null) {
				return "No availableSince %5d for %sdefined channel %s".formatted(//
						influxEdgeId, AllowedChannels.isChannelDefined(channel.toString()) ? "" : "un",
						channel.toString());
			}
			// TODO remove
			// available since is in milliseconds
			if (availableSince > 999_999_999_999L) {
				availableSince /= 1_000;
			}
			if (seconds < availableSince) {
				return "AvailableSince %5d for channel %s too early got: %d, needed %d".formatted(//
						influxEdgeId, channel.toString(), availableSince, seconds);
			}
		}
		return null;
	}

	private OptionalLong getAvailableSince(int influxEdgeId, String channelName) {
		final var channelMap = this.availableSinceForEdge.get(influxEdgeId);
		if (channelMap == null) {
			return OptionalLong.empty();
		}
		final var value = channelMap.get(channelName);
		if (value == null) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(value);
	}

	private boolean hasAvailableSince(int influxEdgeId, String channelName) {
		return this.getAvailableSince(influxEdgeId, channelName).isPresent();
	}

	private String getMeasurement(ZoneId zoneId) throws OpenemsNamedException {
		final var measurement = this.zoneToMeasurement.get(zoneId);
		if (measurement != null) {
			return measurement;
		}
		// this.log.warn("No measurement provided for zoneId " + zoneId);

		// TODO: add more zoneToMeasurement. Logs from 2024-07-21:
		// - +00:00:02
		// - +01:00
		// - +02:00
		// - -07:00
		// - Africa/Cairo
		// - Africa/Casablanca
		// - Africa/Johannesburg
		// - Africa/Nairobi
		// - Africa/Windhoek
		// - America/Chicago
		// - America/Detroit
		// - America/Edmonton
		// - America/Los_Angeles
		// - America/New_York
		// - America/Sao_Paulo
		// - America/Toronto
		// - America/Vancouver
		// - Asia/Calcutta
		// - Asia/Colombo
		// - Asia/Jerusalem
		// - Asia/Makassar
		// - Asia/Nicosia
		// - Asia/Shanghai
		// - Asia/Tbilisi
		// - Atlantic/Canary
		// - Atlantic/Reykjavik
		// - Australia/Adelaide
		// - Etc/GMT-2
		// - Europe/Amsterdam
		// - Europe/Athens
		// - Europe/Bratislava
		// - Europe/Brussels
		// - Europe/Bucharest
		// - Europe/Budapest
		// - Europe/Copenhagen
		// - Europe/Dublin
		// - Europe/Helsinki
		// - Europe/Istanbul
		// - Europe/Lisbon
		// - Europe/Ljubljana
		// - Europe/London
		// - Europe/Luxembourg
		// - Europe/Madrid
		// - Europe/Malta
		// - Europe/Oslo
		// - Europe/Paris
		// - Europe/Podgorica
		// - Europe/Prague
		// - Europe/Riga
		// - Europe/Rome
		// - Europe/Sarajevo
		// - Europe/Sofia
		// - Europe/Stockholm
		// - Europe/Tallinn
		// - Europe/Tirane
		// - Europe/Vaduz
		// - Europe/Vienna
		// - Europe/Vilnius
		// - Europe/Warsaw
		// - Europe/Zagreb
		// - Europe/Zurich
		// - Indian/Maldives
		for (var entry : this.zoneToMeasurement.entrySet()) {
			return entry.getValue();
		}
		throw new InternalTimedataException("No measurement provided for zoneId " + zoneId);
	}

	private static boolean isTodayOrAfter(ZonedDateTime time) {
		return ZonedDateTime.now(time.getZone()).truncatedTo(DurationUnit.ofDays(1)).isBefore(time);
	}

	private static Map<ZoneId, String> parseMeasurementsByZone(String[] strings) {
		return Arrays.stream(strings) //
				.map(t -> t.split("=")) //
				.collect(toMap(//
						parts -> ZoneId.of(parts[0]), //
						parts -> parts[1]));
	}

	@Override
	public String debugLog() {
		return "[" + this.getName() + "] " + this.config.id() + " " + this.influxConnector.debugLog();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		return null;
	}

	private void handleEdgeVersionUpdateEvent(//
			final String edgeId, //
			final SemanticVersion lastVersion, //
			final SemanticVersion currentVersion //
	) {
		// Version ZERO indicates that this edge got connected for the first time
		if (!lastVersion.equals(SemanticVersion.ZERO)) {
			return;
		}
		Integer influxEdgeId;
		try {
			influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		} catch (OpenemsException e) {
			return;
		}

		for (var channel : AllowedChannels.ALLOWED_AVERAGE_CHANNELS.keySet()) {
			this.setAvailableSince(influxEdgeId, channel, 0);
		}
		for (var channel : AllowedChannels.ALLOWED_CUMULATED_CHANNELS.keySet()) {
			this.setAvailableSince(influxEdgeId, channel, 0);
		}
	}

}
