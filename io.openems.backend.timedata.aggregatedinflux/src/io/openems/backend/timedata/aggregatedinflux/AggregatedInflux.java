package io.openems.backend.timedata.aggregatedinflux;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySortedMap;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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
import io.openems.backend.common.timedata.InternalTimedataException;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.timedata.aggregatedinflux.AllowedChannels.ChannelType;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.oem.OpenemsBackendOem;
import io.openems.common.timedata.DbDataUtils;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.AggregatedInfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AggregatedInflux extends AbstractOpenemsBackendComponent implements Timedata, DebugLoggable {

	private final Logger log = LoggerFactory.getLogger(AggregatedInflux.class);

	private WriteParameters writeParametersAvgPoints;
	private WriteParameters writeParametersMaxPoints;

	private Config config;
	private InfluxConnector influxConnector;

	private final Map<ZoneId, String> zoneToMeasurement = new HashMap<>();

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
	}

	@Deactivate
	private void deactivate() {
		this.influxConnector.deactivate();
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

		final var availableChannels = this.getPossibleChannels(influxEdgeId, channels, fromDate);

		return this.influxConnector.queryHistoricData(Optional.of(influxEdgeId), //
				fromDate, toDate, availableChannels, resolution,
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

		final var availableChannels = this.getPossibleChannels(influxEdgeId, channels, fromDate);

		if (availableChannels.isEmpty()) {
			return emptySortedMap();
		}

		if (isTodayOrAfter(toDate)) {
			if (this.queryWithCurrentData == null) {
				throw new InternalTimedataException("Missing 'queryWithCurrentData' object");
			}
			return this.queryWithCurrentData.queryHistoricEnergy(edgeId, fromDate, toDate, availableChannels);
		}
		final var measurement = this.getMeasurement(fromDate.getZone());
		return this.influxConnector.queryHistoricEnergySingleValueInDay(Optional.of(influxEdgeId), //
				fromDate, toDate, availableChannels, this.config.retentionPolicyMax() + "." + measurement);
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

		final var availableChannels = this.getPossibleChannels(influxEdgeId, channels, fromDate);

		final var measurement = this.getMeasurement(fromDate.getZone());
		final var rawData = this.influxConnector.queryRawHistoricEnergyPerPeriodSinglePerDay(Optional.of(influxEdgeId), //
				fromDate, toDate, availableChannels, resolution, this.config.retentionPolicyMax() + "." + measurement);
		if (isTodayOrAfter(toDate)) {
			if (this.queryWithCurrentData == null) {
				throw new InternalTimedataException("Missing 'queryWithCurrentData' object");
			}
			return this.queryWithCurrentData.queryHistoricEnergyPerPeriod(edgeId, fromDate, toDate, availableChannels,
					resolution, rawData);
		}
		final var result = DbDataUtils.calculateLastMinusFirst(rawData, fromDate);
		return DbDataUtils.normalizeTable(result, availableChannels, resolution, fromDate, toDate);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(//
			final String edgeId, //
			final ZonedDateTime date, //
			final Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);

		final var availableChannels = this.getPossibleChannels(influxEdgeId, channels, date);

		final var measurement = this.getMeasurement(date.getZone());
		// TODO determine measurement based on channel or separate in two methods
		return this.influxConnector.queryFirstValueBefore(Optional.of(influxEdgeId), //
				date, availableChannels, this.config.retentionPolicyMax() + "." + measurement);
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

	private Map<ZoneId, String> getDayChangeMeasurements(long timestamp) {
		final var instant = Instant.ofEpochMilli(timestamp);
		return this.zoneToMeasurement.entrySet().stream() //
				.filter(entry -> {
					final var time = instant.atZone(entry.getKey());
					return time.getHour() == 23 && time.getMinute() >= 55;
				}) //
				.collect(toMap(Entry::getKey, Entry::getValue));
	}

	private Set<ChannelAddress> getPossibleChannels(//
			int influxEdgeId, //
			Set<ChannelAddress> channels, //
			ZonedDateTime time //
	) {
		return channels.stream().filter(channel -> {
			final var channelType = AllowedChannels.getChannelType(channel.toString());

			if (channelType == ChannelType.UNDEFINED) {
				return false;
			}

			return true;
		}).collect(Collectors.toSet());
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

}
