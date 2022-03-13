package io.openems.backend.timedata.influx;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.influxdb.InfluxDBException.FieldTypeConflictException;
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
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.EdgeCache;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.StringUtils;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.InfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Influx extends AbstractOpenemsBackendComponent implements Timedata {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(Influx.class);
	private final Map<String, EdgeCache> edgeCacheMap = new HashMap<>();
	private final FieldTypeConflictHandler fieldTypeConflictHandler;

	private InfluxConnector influxConnector = null;

	public Influx() {
		super("Timedata.InfluxDB");
		this.fieldTypeConflictHandler = new FieldTypeConflictHandler(this);
	}

	@Reference
	protected volatile Metadata metadata;

	@Activate
	private void activate(Config config) throws OpenemsException {
		this.logInfo(this.log, "Activate [" //
				+ "url=" + config.url() + ";"//
				+ "port=" + config.port() + ";" //
				+ "database=" + config.database() + ";"//
				+ "retentionPolicy=" + config.retentionPolicy() + ";"//
				+ "username=" + config.username() + ";"//
				+ "password=" + (config.password() != null ? "ok" : "NOT_SET") + ";"//
				+ "measurement=" + config.measurement() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.influxConnector = new InfluxConnector(config.url(), config.port(), config.username(), config.password(),
				config.database(), config.retentionPolicy(), config.isReadOnly(), //
				(failedPoints, throwable) -> {
					if (throwable instanceof FieldTypeConflictException) {
						this.fieldTypeConflictHandler.handleException((FieldTypeConflictException) throwable);
					} else {
						this.logError(this.log,
								"Unable to write to InfluxDB. " + throwable.getClass().getSimpleName() + ": "
										+ throwable.getMessage() + " for "
										+ StringUtils.toShortString(failedPoints.toString(), 100));
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
		var edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

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
	private void writeData(int influxEdgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data)
			throws OpenemsException {
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
			var builder = Point //
					.measurement(InfluxConnector.MEASUREMENT) //
					.tag(OpenemsOEM.INFLUXDB_TAG, String.valueOf(influxEdgeId)) //
					.time(timestamp, TimeUnit.MILLISECONDS);
			for (Entry<ChannelAddress, JsonElement> channelEntry : channelEntries) {
				this.addValue(builder, channelEntry.getKey().toString(), channelEntry.getValue());
			}
			if (builder.hasFields()) {
				this.influxConnector.write(builder.build());
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

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
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
	private void addValue(Builder builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull()) {
			// do not add
			return;
		}
		if (this.specialCaseFieldHandling(builder, field, element)) {
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
	private boolean specialCaseFieldHandling(Builder builder, String field, JsonElement value) {
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
		var edge = this.metadata.getEdge(edgeId);
		if (!edge.isPresent()) {
			return Optional.empty();
		}
		if (edge.get().getVersion().isAtLeast(new SemanticVersion(2018, 11, 0))) {
			return Optional.empty();
		}
		// Old version: start compatibility mode
		var compatibility = this.getCompatibilityFormula(edge.get(), address);
		if (compatibility.length == 0) {
			return Optional.empty();
		}
		// handle compatibility with elder OpenEMS Edge version
		return this.getCompatibilityChannelValue(compatibility, cache);
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
		var value = 0;
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
		var config = edge.getConfig();

		if (address.getComponentId().equals("_sum")) {
			switch (address.getChannelId()) {

			case "EssSoc": {
				var ids = config.getComponentsImplementingNature("EssNature");
				if (ids.size() > 0) {
					// take first result
					return new ChannelFormula[] {
							new ChannelFormula(Function.PLUS, new ChannelAddress(ids.get(0), "Soc")) };
				}
				return new ChannelFormula[0];
			}

			case "EssActivePower": {
				var asymmetricIds = config.getComponentsImplementingNature("AsymmetricEssNature");
				var symmetricIds = config.getComponentsImplementingNature("SymmetricEssNature");
				symmetricIds.removeAll(asymmetricIds);
				var result = new ChannelFormula[asymmetricIds.size() * 3 + symmetricIds.size()];
				var i = 0;
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
				case "Kostal PIKO + B-Box HV":
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
				var ignoreIds = config.getComponentsImplementingNature("FeneconMiniConsumptionMeter");
				ignoreIds.add("meter0");

				var asymmetricIds = config.getComponentsImplementingNature("AsymmetricMeterNature");
				asymmetricIds.removeAll(ignoreIds);

				var symmetricIds = config.getComponentsImplementingNature("SymmetricMeterNature");
				symmetricIds.removeAll(ignoreIds);
				symmetricIds.removeAll(asymmetricIds);

				var result = new ChannelFormula[asymmetricIds.size() * 3 + symmetricIds.size()];
				var i = 0;
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
				var ids = config.getComponentsImplementingNature("ChargerNature");
				var result = new ChannelFormula[ids.size()];
				for (var i = 0; i < ids.size(); i++) {
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

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

}
