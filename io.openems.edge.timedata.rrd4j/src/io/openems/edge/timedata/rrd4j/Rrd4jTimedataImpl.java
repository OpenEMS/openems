package io.openems.edge.timedata.rrd4j;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchData;
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
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Timedata.Rrd4j", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class Rrd4jTimedataImpl extends AbstractOpenemsComponent
		implements Rrd4jTimedata, Timedata, OpenemsComponent, EventHandler {

	private static final String RRD4J_PATH = "rrd4j";
	private static final String DEFAULT_DATASOURCE_NAME = "value";
	private static final int DEFAULT_STEP_SECONDS = 60;
	private static final int DEFAULT_HEARTBEAT_SECONDS = DEFAULT_STEP_SECONDS;

	private final Logger log = LoggerFactory.getLogger(Rrd4jTimedataImpl.class);

	private final RecordWorker worker;
	private final RrdRandomAccessFileBackendFactory factory;

	public Rrd4jTimedataImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Timedata.ChannelId.values(), //
				Rrd4jTimedata.ChannelId.values() //
		);
		this.worker = new RecordWorker(this);
		this.factory = new RrdRandomAccessFileBackendFactory();
	}

	@Reference
	protected ComponentManager componentManager;

	@Activate
	void activate(ComponentContext context, Config config) throws Exception {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (config.enabled()) {
			this.worker.setNoOfCycles(config.noOfCycles());
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
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		ZoneId timezone = fromDate.getZone();
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();

		RrdDb database = null;
		try {
			long fromTimestamp = fromDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
			long toTimeStamp = toDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();

			for (ChannelAddress channelAddress : channels) {
				Channel<?> channel = this.componentManager.getChannel(channelAddress);
				database = this.getExistingRrdDb(channel.address());
				if (database == null) {
					continue; // not existing -> abort
				}

				FetchRequest request = database.createFetchRequest(ConsolFun.AVERAGE, fromTimestamp, toTimeStamp,
						resolution);
				FetchData data = request.fetchData();
				database.close();

				for (int i = 0; i < data.getTimestamps().length; i++) {
					Instant timestampInstant = Instant.ofEpochSecond(data.getTimestamps()[i]);
					ZonedDateTime dateTime = ZonedDateTime.ofInstant(timestampInstant, ZoneOffset.UTC)
							.withZoneSameInstant(timezone);
					SortedMap<ChannelAddress, JsonElement> tableRow = table.get(dateTime);
					if (tableRow == null) {
						tableRow = new TreeMap<>();
					}
					double value = data.getValues(0)[i];
					if (Double.isNaN(value)) {
						tableRow.put(channelAddress, JsonNull.INSTANCE);
					} else {
						tableRow.put(channelAddress, new JsonPrimitive(value));
					}
					table.put(dateTime, tableRow);
				}
			}
		} catch (IOException | IllegalArgumentException e) {
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
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		SortedMap<ChannelAddress, JsonElement> table = new TreeMap<>();
		long fromTimestamp = fromDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
		long toTimeStamp = toDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();

		RrdDb database = null;
		try {
			for (ChannelAddress channelAddress : channels) {
				Channel<?> channel = this.componentManager.getChannel(channelAddress);
				database = this.getExistingRrdDb(channel.address());
				if (database == null) {
					continue; // not existing -> abort
				}

				ChannelDef chDef = this.getDsDefForChannel(channel.channelDoc().getUnit());
				FetchRequest request = database.createFetchRequest(chDef.consolFun, fromTimestamp, toTimeStamp);
				FetchData data = request.fetchData();
				database.close();

				// Find first and last energy value != null
				double first = Double.NaN;
				double last = Double.NaN;
				for (Double tmp : data.getValues(0)) {
					if (Double.isNaN(first) && !Double.isNaN(tmp)) {
						first = tmp;
					}
					if (!Double.isNaN(tmp)) {
						last = tmp;
					}
				}

				// Calculate difference between last and first value
				double value = last - first;

				if (Double.isNaN(value)) {
					table.put(channelAddress, JsonNull.INSTANCE);
				} else {
					table.put(channelAddress, new JsonPrimitive(value));
				}

			}
		} catch (IOException | IllegalArgumentException e) {
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
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		// Prepare result
		final CompletableFuture<Optional<Object>> result = new CompletableFuture<>();

		CompletableFuture.runAsync(() -> {
			RrdDb database = this.getExistingRrdDb(channelAddress);
			if (database == null) {
				result.complete(Optional.empty());
			}
			try {
				result.complete(Optional.of(database.getLastDatasourceValues()[0]));
			} catch (IOException | ArrayIndexOutOfBoundsException e) {
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
	 * @param startTime      the starttime for newly created RrdDbs
	 * @return the RrdDb
	 * @throws IOException        on error
	 * @throws URISyntaxException on error
	 */
	protected synchronized RrdDb getRrdDb(ChannelAddress channelAddress, Unit channelUnit, long startTime)
			throws IOException, URISyntaxException {
		RrdDb rrdDb = this.getExistingRrdDb(channelAddress);
		if (rrdDb != null) {
			/*
			 * Open existing DB
			 */
			return rrdDb;

		} else {
			/*
			 * Create new DB
			 */
			ChannelDef channelDef = this.getDsDefForChannel(channelUnit);
			RrdDef rrdDef = new RrdDef(//
					this.getDbFile(channelAddress).toURI(), //
					startTime, // Start-Time
					DEFAULT_STEP_SECONDS // Step in [s], default: 60 = 1 minute
			);
			rrdDef.addDatasource(//
					new DsDef(DEFAULT_DATASOURCE_NAME, //
							channelDef.dsType, //
							DEFAULT_HEARTBEAT_SECONDS, // Heartbeat in [s], default 60 = 1 minute
							channelDef.minValue, channelDef.maxValue));
			// detailed recordings
			rrdDef.addArchive(channelDef.consolFun, 0.5, 1, 1_440); // 1 step (1 minute), 1440 rows (1 day)
			rrdDef.addArchive(channelDef.consolFun, 0.5, 5, 2_880); // 5 steps (5 minutes), 2880 rows (10 days)
			// hourly values for a very long time
			rrdDef.addArchive(channelDef.consolFun, 0.5, 60, 87_600); // 60 steps (1 hour), 87600 rows (10 years)

			return RrdDb.getBuilder() //
					.setBackendFactory(this.factory) //
					.usePool() //
					.setRrdDef(rrdDef) //
					.build();
		}
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
		File file = this.getDbFile(channelAddress);
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
		File file = Paths.get(//
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
	 * @param channel the Channel
	 * @return the {@link DsDef}
	 */
	private ChannelDef getDsDefForChannel(Unit channelUnit) {
		switch (channelUnit) {
		case AMPERE:
		case AMPERE_HOURS:
		case DEGREE_CELSIUS:
		case DEZIDEGREE_CELSIUS:
		case HERTZ:
		case HOUR:
		case KILOAMPERE_HOURS:
		case KILOOHM:
		case KILOVOLT_AMPERE:
		case KILOVOLT_AMPERE_REACTIVE:
		case KILOWATT:
		case MICROOHM:
		case MILLIAMPERE_HOURS:
		case MILLIAMPERE:
		case MILLIHERTZ:
		case MILLIOHM:
		case MILLISECONDS:
		case MILLIVOLT:
		case MILLIWATT:
		case MINUTE:
		case NONE:
		case WATT:
		case VOLT:
		case VOLT_AMPERE:
		case VOLT_AMPERE_REACTIVE:
		case WATT_HOURS_BY_WATT_PEAK:
		case OHM:
		case SECONDS:
		case THOUSANDTH:
			return new ChannelDef(DsType.GAUGE, Double.NaN, Double.NaN, ConsolFun.AVERAGE);
		case PERCENT:
			return new ChannelDef(DsType.GAUGE, Double.NaN, 100, ConsolFun.AVERAGE);
		case ON_OFF:
			return new ChannelDef(DsType.GAUGE, Double.NaN, 1, ConsolFun.AVERAGE);
		case WATT_HOURS:
		case KILOWATT_HOURS:
		case VOLT_AMPERE_HOURS:
		case VOLT_AMPERE_REACTIVE_HOURS:
		case KILOVOLT_AMPERE_REACTIVE_HOURS:
			return new ChannelDef(DsType.GAUGE, Double.NaN, Double.NaN, ConsolFun.MAX);
		}
		throw new IllegalArgumentException("Unhandled Channel unit [" + channelUnit + "]");
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
