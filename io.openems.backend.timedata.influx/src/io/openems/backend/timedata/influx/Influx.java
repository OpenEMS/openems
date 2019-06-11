package io.openems.backend.timedata.influx;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ObjectArrays;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;
import io.openems.backend.timedata.core.EdgeCache;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.StringUtils;
import io.openems.shared.influxdb.InfluxConnector;
import io.openems.shared.influxdb.InfluxConstants;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Timedata.InfluxDB", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Influx extends AbstractOpenemsBackendComponent implements Timedata {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(Influx.class);
	private final Map<String, EdgeCache> edgeCacheMap = new HashMap<>();

	private InfluxConnector influxConnector = null;

	public Influx() {
		super("Timedata.InfluxDB");
	}

	@Reference
	protected volatile Metadata metadata;

	@Activate
	void activate(Config config) throws OpenemsException {
		this.logInfo(this.log, "Activate [" + //
				"url=" + config.url() + //
				";port=" + config.port() + //
				";database=" + config.database() + //
				";retentionPolicy=" + config.retentionPolicy() + //
				";username=" + config.username() + //
				";password=" + (config.password() != null ? "ok" : "NOT_SET") + //
				";measurement=" + config.measurement() + //
				(config.isReadOnly() ? ";READ_ONLY_MODE" : "") + //
				"]");

		this.influxConnector = new InfluxConnector(config.url(), config.port(), config.username(), config.password(),
				config.database(), config.retentionPolicy(), config.isReadOnly(), //
				(failedPoints, throwable) -> {
					String pointsString = StreamSupport.stream(failedPoints.spliterator(), false)
							.map(Point::lineProtocol).collect(Collectors.joining(","));
					this.logError(this.log, "Unable to write to InfluxDB: " + throwable.getMessage() + " for "
							+ StringUtils.toShortString(pointsString, 100));
				});
	}

	@Deactivate
	void deactivate() {
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
		EdgeCache edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

		/*
		 * Prepare data table. Takes entries starting with eldest timestamp (ascending
		 * order)
		 */
		for (Entry<Long, Map<ChannelAddress, JsonElement>> entry : data.rowMap().entrySet()) {
			Long timestamp = entry.getKey();

			// Check if cache is valid (it is not elder than 5 minutes compared to this
			// timestamp)
			long cacheTimestamp = edgeCache.getTimestamp();
			if (timestamp < cacheTimestamp) {
				// incoming data is older than cache -> do not apply cache

			} else {
				// incoming data is more recent than cache
				if (timestamp < cacheTimestamp + 5 * 60 * 1000) {
					// cache is valid (not elder than 5 minutes)
					for (Entry<ChannelAddress, JsonElement> cacheEntry : edgeCache.getChannelCacheEntries()
							.entrySet()) {
						ChannelAddress channel = cacheEntry.getKey();
						// check if there is a current value for this timestamp + channel
						JsonElement existingValue = data.get(timestamp, channel);
						if (existingValue == null) {
							// if not -> add cache data to write data
							data.put(timestamp, channel, cacheEntry.getValue());
						}
					}
				} else {
					// cache is not anymore valid (elder than 5 minutes)
					if (cacheTimestamp != 0L) {
						this.logInfo(this.log, "Edge [" + edgeId + "]: invalidate cache for influxId [" + influxEdgeId
								+ "]. This timestamp [" + timestamp + "]. Cache timestamp [" + cacheTimestamp + "]");
					}
					// clear cache
					edgeCache.clear();
				}

				// update cache
				edgeCache.setTimestamp(timestamp);
				for (Entry<ChannelAddress, JsonElement> channelEntry : entry.getValue().entrySet()) {
					edgeCache.putToChannelCache(channelEntry.getKey(), channelEntry.getValue());
				}
			}
		}

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
	private void writeData(int influxEdgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data)
			throws OpenemsException {
		Set<Entry<Long, Map<ChannelAddress, JsonElement>>> dataEntries = data.rowMap().entrySet();
		if (dataEntries.isEmpty()) {
			// no data to write
			return;
		}

		for (Entry<Long, Map<ChannelAddress, JsonElement>> dataEntry : dataEntries) {
			Set<Entry<ChannelAddress, JsonElement>> channelEntries = dataEntry.getValue().entrySet();
			if (channelEntries.isEmpty()) {
				// no points to add
				continue;
			}

			Long timestamp = dataEntry.getKey();
			// this builds an InfluxDB record ("point") for a given timestamp
			Point.Builder builder = Point //
					.measurement(InfluxConnector.MEASUREMENT) //
					.tag(InfluxConstants.TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp, TimeUnit.MILLISECONDS);
			for (Entry<ChannelAddress, JsonElement> channelEntry : channelEntries) {
				Influx.addValue(builder, channelEntry.getKey().toString(), channelEntry.getValue());
			}
			if (builder.hasFields()) {
				this.influxConnector.write(builder.build());
			}
		}
	}

	public static Integer parseNumberFromName(String name) throws OpenemsException {
		try {
			Matcher matcher = NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				String nameNumberString = matcher.group(1);
				return Integer.parseInt(nameNumberString);
			}else {
				return Integer.parseInt(name);
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		throw new OpenemsException("Unable to parse number from name [" + name + "]");
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
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

	/**
	 * Adds the value in the correct data format for InfluxDB.
	 *
	 * @param builder the Influx PointBuilder
	 * @param field   the field name
	 * @param element the value
	 * @return
	 */
	private static void addValue(Builder builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull()) {
			// do not add
			return;
		}
		if (element.isJsonPrimitive()) {
			JsonPrimitive value = element.getAsJsonPrimitive();
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

	@Override
	public Optional<JsonElement> getChannelValue(String edgeId, ChannelAddress address) {
		EdgeCache cache = this.edgeCacheMap.get(edgeId);
		if (cache != null) {
			Optional<JsonElement> value = cache.getChannelValue(address);
			if (value.isPresent()) {
				return value;
			}
			Optional<Edge> edge = this.metadata.getEdge(edgeId);
			if (!edge.isPresent()) {
				return Optional.empty();
			}
			if (edge.get().getVersion().isAtLeast(new SemanticVersion(2018, 11, 0))) {
				return Optional.empty();
			}
			// Old version: start compatibility mode
			ChannelFormula[] compatibility = this.getCompatibilityFormula(edge.get(), address);
			if (compatibility.length == 0) {
				return Optional.empty();
			}
			// handle compatibility with elder OpenEMS Edge version
			return this.getCompatibilityChannelValue(compatibility, cache);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Handles compatibility with elder OpenEMS Edge version, e.g. calculate the
	 * '_sum' Channels.
	 * 
	 * @param compatibility the formula to calculate the channel value
	 * @param cache         the EdgeCache
	 * @return the value as an Optional
	 */
	@Deprecated
	private Optional<JsonElement> getCompatibilityChannelValue(ChannelFormula[] compatibility, EdgeCache cache) {
		int value = 0;
		for (ChannelFormula formula : compatibility) {
			switch (formula.getFunction()) {
			case PLUS:
				value += formula.getValue(cache);
			}
		}
		return Optional.of(new JsonPrimitive(value));
	}

	/**
	 * Gets the formula to calculate a '_sum' Channel value.
	 * 
	 * @param edge    the Edge
	 * @param address the ChannelAddress
	 * @return the formula to calculate the channel value
	 */
	@Deprecated
	private ChannelFormula[] getCompatibilityFormula(Edge edge, ChannelAddress address) {
		EdgeConfig config = edge.getConfig();

		if (address.getComponentId().equals("_sum")) {
			switch (address.getChannelId()) {

			case "EssSoc": {
				List<String> ids = config.getComponentsImplementingNature("EssNature");
				if (ids.size() > 0) {
					// take first result
					return new ChannelFormula[] {
							new ChannelFormula(Function.PLUS, new ChannelAddress(ids.get(0), "Soc")) };
				} else {
					return new ChannelFormula[0];
				}
			}

			case "EssActivePower": {
				List<String> asymmetricIds = config.getComponentsImplementingNature("AsymmetricEssNature");
				List<String> symmetricIds = config.getComponentsImplementingNature("SymmetricEssNature");
				symmetricIds.removeAll(asymmetricIds);
				ChannelFormula[] result = new ChannelFormula[asymmetricIds.size() * 3 + symmetricIds.size()];
				int i = 0;
				for (String id : asymmetricIds) {
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePowerL1"));
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePowerL2"));
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePowerL3"));
				}
				for (String id : symmetricIds) {
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePower"));
				}
				return result;
			}

			case "EssMaxApparentPower":
				switch (edge.getProducttype()) {
				case "Pro 9-12":
				case "PRO Hybrid 9-10":
					return new ChannelFormula[] { //
							new ChannelFormula(Function.PLUS, 9_000), //
					};
				case "Pro Hybrid 10-Serie":
					return new ChannelFormula[] { //
							new ChannelFormula(Function.PLUS, 10_000), //
					};
				case "MiniES 3-3":
					return new ChannelFormula[] { //
							new ChannelFormula(Function.PLUS, 3_000), //
					};
				case "Commercial 50-Serie":
					return new ChannelFormula[] { //
							new ChannelFormula(Function.PLUS, 50_000), //
					};
				case "COMMERCIAL 40-45":
				case "INDUSTRIAL":
				case "":
					return new ChannelFormula[] { //
							new ChannelFormula(Function.PLUS, 40_000), //
					};
				default:
					this.logWarn(this.log,
							"No formula for " + address + " [" + edge.getId() + "|" + edge.getProducttype() + "]");
					return new ChannelFormula[] { //
							new ChannelFormula(Function.PLUS, 40_000) //
					};
				}

			case "GridActivePower":
				return new ChannelFormula[] { //
						new ChannelFormula(Function.PLUS, new ChannelAddress("meter0", "ActivePower")) //
				};

			case "GridMinActivePower":
				return new ChannelFormula[] { //
						new ChannelFormula(Function.PLUS, new ChannelAddress("meter0", "minActivePower")) //
				};

			case "GridMaxActivePower":
				return new ChannelFormula[] { //
						new ChannelFormula(Function.PLUS, new ChannelAddress("meter0", "maxActivePower")) //
				};

			case "ProductionActivePower":
				return ObjectArrays.concat(//
						this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "ProductionAcActivePower")), //
						this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "ProductionDcActualPower")), //
						ChannelFormula.class);

			case "ProductionAcActivePower": {
				List<String> ignoreIds = config.getComponentsImplementingNature("FeneconMiniConsumptionMeter");
				ignoreIds.add("meter0");

				List<String> asymmetricIds = config.getComponentsImplementingNature("AsymmetricMeterNature");
				asymmetricIds.removeAll(ignoreIds);

				List<String> symmetricIds = config.getComponentsImplementingNature("SymmetricMeterNature");
				symmetricIds.removeAll(ignoreIds);
				symmetricIds.removeAll(asymmetricIds);

				ChannelFormula[] result = new ChannelFormula[asymmetricIds.size() * 3 + symmetricIds.size()];
				int i = 0;
				for (String id : asymmetricIds) {
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePowerL1"));
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePowerL2"));
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePowerL3"));
				}
				for (String id : symmetricIds) {
					result[i++] = new ChannelFormula(Function.PLUS, new ChannelAddress(id, "ActivePower"));
				}
				return result;
			}

			case "ProductionDcActualPower": {
				List<String> ids = config.getComponentsImplementingNature("ChargerNature");
				ChannelFormula[] result = new ChannelFormula[ids.size()];
				for (int i = 0; i < ids.size(); i++) {
					result[i] = new ChannelFormula(Function.PLUS, new ChannelAddress(ids.get(i), "ActualPower"));
				}
				return result;
			}

			case "ProductionMaxActivePower":
				return new ChannelFormula[] { //
						new ChannelFormula(Function.PLUS, new ChannelAddress("meter1", "maxActivePower")) //
				};

			case "ConsumptionActivePower":
				return ObjectArrays.concat(//
						ObjectArrays.concat(//
								this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "EssActivePower")), //
								this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "GridActivePower")), //
								ChannelFormula.class),
						this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "ProductionAcActivePower")), //
						ChannelFormula.class);

			case "ConsumptionMaxActivePower":
				return ObjectArrays.concat(//
						ObjectArrays.concat(//
								this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "EssMaxApparentPower")), //
								this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "GridMaxActivePower")), //
								ChannelFormula.class),
						this.getCompatibilityFormula(edge, new ChannelAddress("_sum", "ProductionMaxActivePower")), //
						ChannelFormula.class);
			}
		}
		return new ChannelFormula[0];
	}

}
