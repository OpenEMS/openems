package io.openems.edge.timedata.rrd4j;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.Rrd4j", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class TimedataRrd4jImpl extends AbstractOpenemsComponent
		implements TimedataRrd4j, Timedata, OpenemsComponent, EventHandler {

	protected static final String DEFAULT_DATASOURCE_NAME = "value";
	protected static final int DEFAULT_STEP_SECONDS = 300;
	protected static final int DEFAULT_HEARTBEAT_SECONDS = DEFAULT_STEP_SECONDS;

	private static final String RRD4J_PATH = "rrd4j";

	private final Logger log = LoggerFactory.getLogger(TimedataRrd4jImpl.class);
	private final RecordWorker worker;
	private final RrdRandomAccessFileBackendFactory factory;

	@Reference
	protected ComponentManager componentManager;

	protected PersistencePriority persistencePriority = PersistencePriority.MEDIUM;

	public TimedataRrd4jImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Timedata.ChannelId.values(), //
				TimedataRrd4j.ChannelId.values() //
		);
		this.worker = new RecordWorker(this);
		this.factory = new RrdRandomAccessFileBackendFactory();
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws Exception {
		this.persistencePriority = config.persistencePriority();
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (config.enabled()) {
			this.worker.activate(config.id());
		}
	}

	@Deactivate
	protected void deactivate() {
		this.worker.deactivate();
		super.deactivate();
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		var timezone = fromDate.getZone();
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();

		RrdDb database = null;
		try {
			var fromTimestamp = fromDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
			var toTimeStamp = toDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();

			var errorCounter = 0;
			for (ChannelAddress channelAddress : channels) {
				try {
					Channel<?> channel = this.componentManager.getChannel(channelAddress);
					database = this.getExistingRrdDb(channel.address());
					if (database == null) {
						throw new OpenemsException("RRD4j Database for " + channelAddress + " is missing");
					}
					var chDef = this.getDsDefForChannel(channel.channelDoc().getUnit());
					var request = database.createFetchRequest(chDef.consolFun, fromTimestamp, toTimeStamp,
							resolution.toSeconds());

					// Post-Process data
					var result = postProcessData(request, resolution.toSeconds());
					database.close();

					for (var i = 0; i < result.length; i++) {
						var timestamp = fromTimestamp + (i * resolution.toSeconds());

						// Prepare result table row
						var timestampInstant = Instant.ofEpochSecond(timestamp);
						var dateTime = ZonedDateTime.ofInstant(timestampInstant, ZoneOffset.UTC)
								.withZoneSameInstant(timezone);
						var tableRow = table.get(dateTime);
						if (tableRow == null) {
							tableRow = new TreeMap<>();
						}

						var value = result[i];
						if (Double.isNaN(value)) {
							tableRow.put(channelAddress, JsonNull.INSTANCE);
						} else {
							tableRow.put(channelAddress, new JsonPrimitive(value));
						}

						table.put(dateTime, tableRow);
					}

				} catch (Exception e) {
					this.logWarn(this.log, "Unable to query RRD4j: " + e.getMessage());
					errorCounter++;
				}
			}

			// If no Channel can be read successfully: throw exception; otherwise return the
			// available data
			if (errorCounter == channels.size()) {
				throw new OpenemsException("No valid Channel available");
			}

		} catch (Exception e) {
			throw new OpenemsException("Unable to read historic data: " + e.getMessage());
		} finally {
			if (database != null && !database.isClosed()) {
				try {
					database.close();
				} catch (IOException e) {
					this.logWarn(this.log, "Unable to close database: " + e.getMessage());
				}
			}
		}
		return table;
	}

	/**
	 * Post-Process the received data.
	 * 
	 * <p>
	 * This mainly makes sure the data has the correct resolution.
	 * 
	 * @param request    the RRD4j {@link FetchRequest}
	 * @param resolution the resolution in seconds
	 * @return the result array
	 * @throws IOException              on error
	 * @throws IllegalArgumentException on error
	 */
	protected static double[] postProcessData(FetchRequest request, long resolution)
			throws IOException, IllegalArgumentException {
		var data = request.fetchData();
		var step = data.getStep();
		var input = data.getValues()[0];

		// Initialize result array
		final var result = new double[(int) ((request.getFetchEnd() - request.getFetchStart()) / resolution)];
		Arrays.fill(result, Double.NaN);

		if (step < resolution) {
			// Merge multiple entries to resolution
			if (resolution % step != 0) {
				throw new IllegalArgumentException(
						"Requested resolution [" + resolution + "] is not dividable by RRD4j Step [" + step + "]");
			}
			var merge = (int) (resolution / step);
			var buffer = new double[merge];
			for (var i = 1; i < input.length; i += merge) {
				for (var j = 0; j < merge; j++) {
					if (i + j < input.length) {
						buffer[j] = input[i + j];
					} else {
						buffer[j] = Double.NaN;
					}
				}

				// put in result; avoid index rounding error
				var resultIndex = (i - 1) / merge;
				if (resultIndex >= result.length) {
					break;
				}
				result[resultIndex] = TypeUtils.average(buffer);
			}

		} else if (step > resolution) {
			// Split each entry to multiple values
			var resultTimestamp = 0;
			for (int i = 0, inputIndex = 0; i < result.length; i++) {
				inputIndex = Math.min(input.length - 1, (int) (resultTimestamp / step));
				resultTimestamp += resolution;
				result[i] = input[inputIndex];
			}

		} else {
			// Data already matches resolution
			for (var i = 1; i < result.length + 1 && i < input.length; i++) {
				result[i - 1] = input[i];
			}
		}
		return result;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		SortedMap<ChannelAddress, JsonElement> table = new TreeMap<>();
		var fromTimestamp = fromDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
		var toTimeStamp = toDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();

		RrdDb database = null;
		try {
			var errorCounter = 0;
			for (ChannelAddress channelAddress : channels) {
				try {
					Channel<?> channel = this.componentManager.getChannel(channelAddress);
					database = this.getExistingRrdDb(channel.address());
					if (database == null) {
						throw new OpenemsException("RRD4j Database for " + channelAddress + " is missing");
					}

					var chDef = this.getDsDefForChannel(channel.channelDoc().getUnit());
					var request = database.createFetchRequest(chDef.consolFun, fromTimestamp, toTimeStamp);
					var data = request.fetchData();
					database.close();

					// Find first and last energy value != null
					var first = Double.NaN;
					var last = Double.NaN;
					for (Double tmp : data.getValues(0)) {
						if (Double.isNaN(first) && !Double.isNaN(tmp)) {
							first = tmp;
						}
						if (!Double.isNaN(tmp)) {
							last = tmp;
						}
					}

					// Calculate difference between last and first value
					var value = last - first;

					if (Double.isNaN(value)) {
						table.put(channelAddress, JsonNull.INSTANCE);
					} else {
						table.put(channelAddress, new JsonPrimitive(value));
					}
				} catch (Exception e) {
					this.logWarn(this.log, "Unable to query RRD4j: " + e.getMessage());
					errorCounter++;
				}
			}

			// If no Channel can be read successfully: throw exception; otherwise return the
			// available data
			if (errorCounter == channels.size()) {
				throw new OpenemsException("No valid Channel available");
			}

		} catch (Exception e) {
			throw new OpenemsException("Unable to read historic data: " + e.getMessage());
		} finally {
			if (database != null && !database.isClosed()) {
				try {
					database.close();
				} catch (IOException e) {
					this.logWarn(this.log, "Unable to close database: " + e.getMessage());
				}
			}
		}
		return table;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();

		var timezone = fromDate.getZone();

		var fromTimestamp = fromDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
		var toTimeStamp = toDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();

		var nextStamp = fromTimestamp + resolution.toSeconds();
		var timeStamp = fromTimestamp;

		while (nextStamp <= toTimeStamp) {

			var timestampInstantFrom = Instant.ofEpochSecond(timeStamp);
			var dateTimeFrom = ZonedDateTime.ofInstant(timestampInstantFrom, ZoneOffset.UTC)
					.withZoneSameInstant(timezone);

			var timestampInstantTo = Instant.ofEpochSecond(nextStamp);
			var dateTimeTo = ZonedDateTime.ofInstant(timestampInstantTo, ZoneOffset.UTC).withZoneSameInstant(timezone);

			var tableRow = this.queryHistoricEnergy(null, dateTimeFrom, dateTimeTo, channels);

			table.put(dateTimeFrom, tableRow);

			timeStamp = nextStamp;
			nextStamp += resolution.toSeconds();
		}

		return table;
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		// Prepare result
		final var result = new CompletableFuture<Optional<Object>>();

		CompletableFuture.runAsync(() -> {
			var database = this.getExistingRrdDb(channelAddress);
			if (database == null) {
				result.complete(Optional.empty());
			}
			try {
				result.complete(Optional.of(database.getLastDatasourceValues()[0]));
			} catch (Exception e) {
				result.complete(Optional.empty());
			} finally {
				try {
					database.close();
				} catch (IOException e) {
					this.logWarn(this.log, "Unable to close Database for [" + channelAddress + "]: " + e.getMessage());
				}
			}
		});

		return result;
	}

	/**
	 * Gets the RRD4j database for the given Channel-Address.
	 * 
	 * <p>
	 * The predefined RRD4J archives match the requirements of
	 * {@link CommonTimedataService#calculateResolution(ZonedDateTime, ZonedDateTime)}
	 * 
	 * @param channelAddress the Channel-Address
	 * @param channelUnit    the {@link Unit}
	 * @param startTime      the starttime for newly created RrdDbs
	 * @return the RrdDb
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	protected synchronized RrdDb getRrdDb(ChannelAddress channelAddress, Unit channelUnit, long startTime)
			throws IOException, URISyntaxException {
		var rrdDb = this.getExistingRrdDb(channelAddress);
		if (rrdDb != null) {
			// Database exists

			return this.updateRrdDbToLatestDefinition(rrdDb, channelAddress, channelUnit);

		}
		// Create new database
		return this.createNewDb(channelAddress, channelUnit, startTime);
	}

	/**
	 * Creates new DB.
	 * 
	 * @param channelAddress the {@link ChannelAddress}
	 * @param channelUnit    the {@link Unit} of the Channel
	 * @param startTime      the timestamp of the newly added data
	 * @return the {@link RrdDb}
	 * @throws IOException on error
	 */
	private synchronized RrdDb createNewDb(ChannelAddress channelAddress, Unit channelUnit, long startTime)
			throws IOException {
		var channelDef = this.getDsDefForChannel(channelUnit);
		var rrdDef = new RrdDef(//
				this.getDbFile(channelAddress).toURI(), //
				startTime, // Start-Time
				DEFAULT_STEP_SECONDS // Step in [s], default: 300 = 5 minutes
		);
		rrdDef.addDatasource(//
				new DsDef(DEFAULT_DATASOURCE_NAME, //
						channelDef.dsType, //
						DEFAULT_HEARTBEAT_SECONDS, // Heartbeat in [s], default 300 = 5 minutes
						channelDef.minValue, channelDef.maxValue));
		// detailed recordings
		rrdDef.addArchive(channelDef.consolFun, 0.5, 1, 8_928); // 1 step (5 minutes), 8928 rows (31 days)
		rrdDef.addArchive(channelDef.consolFun, 0.5, 12, 8_016); // 12 steps (60 minutes), 8016 rows (334 days)

		return RrdDb.getBuilder() //
				.setBackendFactory(this.factory) //
				.usePool() //
				.setRrdDef(rrdDef) //
				.build();
	}

	/**
	 * Gets an existing RrdDb.
	 * 
	 * @param channelAddress the ChannelAddress
	 * @return the RrdDb or null
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	protected synchronized RrdDb getExistingRrdDb(ChannelAddress channelAddress) {
		var file = this.getDbFile(channelAddress);
		if (!file.exists()) {
			return null;
		}
		try {
			return RrdDb.getBuilder() //
					.setBackendFactory(this.factory) //
					.usePool() //
					.setPath(file.toURI()) //
					.build();
		} catch (IOException e) {
			this.logError(this.log, "Unable to open existing RrdDb: " + e.getMessage());
			return null;
		}
	}

	private File getDbFile(ChannelAddress channelAddress) {
		var file = Paths.get(//
				OpenemsConstants.getOpenemsDataDir(), //
				RRD4J_PATH, //
				this.id(), //
				channelAddress.getComponentId(), //
				channelAddress.getChannelId()) //
				.toFile();
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

	private static class ChannelDef {
		private final DsType dsType;
		private final double minValue;
		private final double maxValue;
		private final ConsolFun consolFun;

		public ChannelDef(DsType dsType, double minValue, double maxValue, ConsolFun consolFun) {
			this.dsType = dsType;
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.consolFun = consolFun;
		}
	}

	/**
	 * Defines the datasource properties for a given Channel, i.e. min/max allowed
	 * value and GAUGE vs. COUNTER type.
	 * 
	 * @param channelUnit the {@link Unit}
	 * @return the {@link DsDef}
	 */
	private ChannelDef getDsDefForChannel(Unit channelUnit) {
		return switch (channelUnit) {
		case AMPERE, AMPERE_HOURS, DEGREE_CELSIUS, DEZIDEGREE_CELSIUS, EUROS_PER_MEGAWATT_HOUR, HERTZ, HOUR,
				KILOAMPERE_HOURS, KILOOHM, KILOVOLT_AMPERE, KILOVOLT_AMPERE_REACTIVE, KILOWATT, MICROOHM, MICROAMPERE,
				MICROVOLT, MILLIAMPERE_HOURS, MILLIAMPERE, MILLIHERTZ, MILLIOHM, MILLISECONDS, MILLIVOLT, MILLIWATT,
				MINUTE, NONE, WATT, VOLT, VOLT_AMPERE, VOLT_AMPERE_REACTIVE, WATT_HOURS_BY_WATT_PEAK, OHM, SECONDS,
				THOUSANDTH, WATT_HOURS, KILOWATT_HOURS, VOLT_AMPERE_HOURS, VOLT_AMPERE_REACTIVE_HOURS,
				KILOVOLT_AMPERE_REACTIVE_HOURS -> //
			new ChannelDef(DsType.GAUGE, Double.NaN, Double.NaN, ConsolFun.AVERAGE);

		case PERCENT -> //
			new ChannelDef(DsType.GAUGE, 0, 100, ConsolFun.AVERAGE);

		case ON_OFF -> //
			new ChannelDef(DsType.GAUGE, 0, 1, ConsolFun.AVERAGE);

		case CUMULATED_SECONDS, CUMULATED_WATT_HOURS -> //
			new ChannelDef(DsType.GAUGE, Double.NaN, Double.NaN, ConsolFun.MAX);
		};
	}

	/**
	 * Migrates between different versions of the OpenEMS-RRD4j Definition.
	 * 
	 * @param oldDb          the old {@link RrdDb} database
	 * @param channelAddress the {@link ChannelAddress}
	 * @param channelUnit    the {@link Unit} of the Channel
	 * @return new {@link RrdDb}
	 * @throws IOException on error
	 */
	private RrdDb updateRrdDbToLatestDefinition(RrdDb oldDb, ChannelAddress channelAddress, Unit channelUnit)
			throws IOException {
		if (oldDb.getArcCount() <= 2 && oldDb.getRrdDef().getStep() != 60) {
			// No Update required
			return oldDb;
		}
		/*
		 * This is an old OpenEMS-RRD4j Definition -> migrate to latest version
		 */
		// Read data of last month
		var lastTimestamp = oldDb.getLastUpdateTime();
		var firstTimestamp = lastTimestamp - 60 /* minute */ * 60 /* hour */ * 24 /* day */ * 31;
		var fetchRequest = oldDb.createFetchRequest(oldDb.getArchive(0).getConsolFun(), firstTimestamp, lastTimestamp);
		var fetchData = fetchRequest.fetchData();
		final var values = postProcessData(fetchRequest, DEFAULT_HEARTBEAT_SECONDS);
		if (fetchData.getTimestamps().length > 0) {
			firstTimestamp = fetchData.getTimestamps()[0];
		}
		oldDb.close();

		// Delete old file
		Files.delete(Paths.get(oldDb.getCanonicalPath()));

		// Create new database
		var newDb = this.createNewDb(channelAddress, channelUnit, firstTimestamp - 1);

		// Migrate data
		var sample = newDb.createSample();
		for (var i = 0; i < values.length; i++) {
			sample.setTime(firstTimestamp + i * DEFAULT_HEARTBEAT_SECONDS);
			sample.setValue(0, values[i]);
			sample.update();
		}

		this.logInfo(this.log,
				"Migrate RRD4j Database [" + channelAddress.toString() + "] to latest OpenEMS Definition");
		return newDb;
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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.worker.collectData();
			break;
		}
	}

}
