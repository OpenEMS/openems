package io.openems.backend.timedata.aggregatedinflux;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.influxdb.client.domain.WriteConsistency;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.timedata.aggregatedinflux.AllowedChannels.ChannelType;
import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.AggregatedInfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AggregatedInflux extends AbstractOpenemsBackendComponent implements Timedata {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final ZoneId ZONE_BERLIN = ZoneId.of("Europe/Berlin");

	// TODO remove
	private static final ChannelAddress INFLUX_TEST_QUERY_CHANNEL = new ChannelAddress("influx", "query");

	private WriteParameters writeParametersAvgPoints;
	private WriteParameters writeParametersMaxPoints;

	private Config config;
	private InfluxConnector influxConnector;

	// map from edgeId to channelName and availableSince time stamp
	private final Map<Integer, Map<String, Long>> availableSinceForEdge = new ConcurrentHashMap<>();

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
				+ "measurementMax=" + config.measurementMax() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.writeParametersAvgPoints = new WriteParameters(config.bucket() + "/" + config.retentionPolicyAvg(),
				config.org(), WritePrecision.S, WriteConsistency.ALL);
		this.writeParametersMaxPoints = new WriteParameters(config.bucket() + "/" + config.retentionPolicyMax(),
				config.org(), WritePrecision.S, WriteConsistency.ALL);

		this.influxConnector = new InfluxConnector(config.queryLanguage(), URI.create(config.url()), config.org(),
				config.apiKey(), config.bucket(), config.isReadOnly(), config.poolSize(), config.maxQueueSize(), //
				(throwable) -> {
					this.logError(this.log, "Unable to write to InfluxDB. " + throwable.getClass().getSimpleName()
							+ ": " + throwable.getMessage());
				}, this.writeParametersAvgPoints, this.writeParametersMaxPoints);

		// load available since for edges which already wrote in the new database
		this.availableSinceForEdge.clear();
		this.availableSinceForEdge.putAll(this.influxConnector.queryAvailableSince().entrySet().stream().map(entry -> {
			return Map.entry(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	@Deactivate
	private void deactivate() {
		this.influxConnector.deactivate();
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// parse the numeric EdgeId
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		if (!this.isDataAvailable(influxEdgeId, fromDate, channels)) {
			return null;
		}
		return this.influxConnector.queryHistoricData(Optional.of(influxEdgeId), //
				fromDate, toDate, getQueryChannelAddresses(channels), resolution,
				this.config.retentionPolicyAvg() + "." + this.config.measurementAvg());
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// parse the numeric EdgeId
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		if (!this.isDataAvailable(influxEdgeId, fromDate, channels)) {
			return null;
		}
		return this.influxConnector.queryHistoricEnergy(Optional.of(influxEdgeId), //
				fromDate, toDate, getQueryChannelAddresses(channels),
				this.config.retentionPolicyMax() + "." + this.config.measurementMax());
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// parse the numeric EdgeId
		var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
		if (!this.isDataAvailable(influxEdgeId, fromDate, channels)) {
			return null;
		}
		return this.influxConnector.queryHistoricEnergyPerPeriod(Optional.of(influxEdgeId), //
				fromDate, toDate, getQueryChannelAddresses(channels), resolution,
				this.config.retentionPolicyMax() + "." + this.config.measurementMax());
	}

	private static Set<ChannelAddress> getQueryChannelAddresses(Set<ChannelAddress> channels) {
		final var actualQueryChannels = new TreeSet<>(channels);
		actualQueryChannels.remove(INFLUX_TEST_QUERY_CHANNEL);
		return actualQueryChannels;
	}

	@Override
	public String id() {
		return this.config.id();
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification notification) {
		var data = notification.getData().rowMap();

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
					.collect(Collectors.groupingBy(entry -> AllowedChannels.getChannelType(entry.getKey())));

			final var timestamp = dataEntry.getKey();
			BiConsumer<Entry<String, JsonElement>, Point> addEntryToPoint = (entry, point) -> {
				if (!this.hasAvailableSince(influxEdgeId, entry.getKey())) {
					this.setAvailableSince(influxEdgeId, entry.getKey(), timestamp);
				}
				AllowedChannels.addWithSpecificChannelType(point, entry.getKey(), entry.getValue());
			};

			final var point = Point //
					.measurement(this.config.measurementAvg()) //
					.addTag(OpenemsOEM.INFLUXDB_TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp / 1000, WritePrecision.S);

			channelPerType.getOrDefault(ChannelType.AVG, Collections.emptyList()).stream() //
					.forEach(entry -> addEntryToPoint.accept(entry, point));
			if (point.hasFields()) {
				this.influxConnector.write(point, this.writeParametersAvgPoints);
			}

			if (!AggregatedInflux.isDayChange(timestamp)) {
				continue;
			}
			var maxPoint = Point //
					.measurement(this.config.measurementMax()) //
					.addTag(OpenemsOEM.INFLUXDB_TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp / 1000, WritePrecision.S);

			channelPerType.getOrDefault(ChannelType.MAX, Collections.emptyList()).stream() //
					.forEach(entry -> addEntryToPoint.accept(entry, maxPoint));
			if (maxPoint.hasFields()) {
				this.influxConnector.write(maxPoint, this.writeParametersMaxPoints);
			}
		}
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification data) {
		// empty
	}

	// TODO currently only for Europe/Berlin
	private static boolean isDayChange(long timestamp) {
		var time = Instant.ofEpochMilli(timestamp).atZone(ZONE_BERLIN);
		return time.getHour() == 23 && time.getMinute() >= 55;
	}

	private void setAvailableSince(int influxEdgeId, String channelName, long availableSince) {
		this.influxConnector
				.write(this.influxConnector.buildUpdateAvailableSincePoint(influxEdgeId, channelName, availableSince));
		this.availableSinceForEdge.computeIfAbsent(influxEdgeId, key -> new ConcurrentHashMap<>()) //
				.put(channelName, availableSince);
	}

	private boolean isDataAvailable(int influxEdgeId, ZonedDateTime time, Set<ChannelAddress> channels) {
		// TODO remove
		if (!channels.contains(new ChannelAddress("influx", "query"))) {
			return false;
		}
		final var edgeChannels = this.availableSinceForEdge.get(influxEdgeId);
		if (edgeChannels == null) {
			return false;
		}
		final var seconds = time.toEpochSecond() * 1000;
		return channels.stream() //
				.allMatch(channel -> {
					if (channel.equals(INFLUX_TEST_QUERY_CHANNEL)) {
						return true;
					}
					final var availableSince = edgeChannels.get(channel.toString());
					if (availableSince == null) {
						this.logInfo(this.log, "No availableSince for " + channel.toString());
						return false;
					}
					return seconds >= availableSince;
				});
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

}
