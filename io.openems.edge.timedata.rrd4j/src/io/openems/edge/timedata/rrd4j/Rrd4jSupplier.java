package io.openems.edge.timedata.rrd4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.Unit;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.rrd4j.version.Version.CreateDatabaseConfig;
import io.openems.edge.timedata.rrd4j.version.VersionHandler;

@Component(//
		scope = ServiceScope.SINGLETON, //
		service = Rrd4jSupplier.class //
)
public class Rrd4jSupplier {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private VersionHandler versionHandler;

	private final KeyLock keyLock = new KeyLock();
	private final RrdBackendFactory factory;

	protected Rrd4jSupplier(//
			final RrdBackendFactory factory //
	) {
		this.factory = factory;
	}

	@Activate
	public Rrd4jSupplier() {
		this(new RrdRandomAccessFileBackendFactory());
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
	 * @param rrdDbId        the id of the rrd4j database
	 * @return the RrdDb
	 * @throws IOException on error
	 */
	public RrdDb getRrdDb(//
			final String rrdDbId, //
			final ChannelAddress channelAddress, //
			final Unit channelUnit, //
			final long startTime //
	) throws IOException {
		return this.keyLock.lock(rrdDbId + "/" + channelAddress.toString(), () -> {
			var rrdDb = this.getExistingRrdDb(channelAddress, rrdDbId);
			if (rrdDb != null) {
				// Database exists
				return this.updateRrdDbToLatestDefinition(rrdDb, rrdDbId, channelAddress, channelUnit);
			}
			// Create new database
			return this.createNewDb(rrdDbId, channelAddress, channelUnit, startTime);
		});
	}

	/**
	 * Gets an existing and updated {@link RrdDb}. If the found {@link RrdDb} is not
	 * on the current version it gets updated.
	 * 
	 * @param rrdDbId        the id of the RrdDb
	 * @param channelAddress the address of the {@link RrdDb}
	 * @param channelUnit    the unit of the channel
	 * @return the {@link RrdDb} or null if not existing
	 * @throws IOException on IO-Error
	 */
	public RrdDb getExistingUpdatedRrdDb(//
			final String rrdDbId, //
			final ChannelAddress channelAddress, //
			final Unit channelUnit //
	) throws IOException {
		return this.keyLock.lock(rrdDbId + "/" + channelAddress.toString(), () -> {
			var rrdDb = this.getExistingRrdDb(channelAddress, rrdDbId);
			if (rrdDb == null) {
				return null;
			}
			return this.updateRrdDbToLatestDefinition(rrdDb, rrdDbId, channelAddress, channelUnit);
		});
	}

	/**
	 * Defines the datasource properties for a given Channel, i.e. min/max allowed
	 * value and GAUGE vs. COUNTER type.
	 * 
	 * @param channelUnit the {@link Unit}
	 * @return the {@link DsDef}
	 */
	public static ChannelDef getDsDefForChannel(final Unit channelUnit) {
		return switch (channelUnit) {
		case AMPERE, AMPERE_HOURS, DEGREE_CELSIUS, DEZIDEGREE_CELSIUS, EUROS_PER_MEGAWATT_HOUR, HERTZ, HOUR,
				KILOAMPERE_HOURS, KILOOHM, KILOVOLT_AMPERE, KILOVOLT_AMPERE_REACTIVE, KILOWATT, MICROOHM, MICROAMPERE,
				MICROVOLT, MILLIAMPERE_HOURS, MILLIAMPERE, MILLIHERTZ, MILLIOHM, MILLISECONDS, MILLIVOLT, MILLIWATT,
				MINUTE, NONE, WATT, VOLT, VOLT_AMPERE, VOLT_AMPERE_REACTIVE, WATT_HOURS_BY_WATT_PEAK, OHM, SECONDS,
				THOUSANDTH, WATT_HOURS, KILOWATT_HOURS, VOLT_AMPERE_HOURS, VOLT_AMPERE_REACTIVE_HOURS,
				KILOVOLT_AMPERE_REACTIVE_HOURS ->
			new ChannelDef(DsType.GAUGE, Double.NaN, Double.NaN, ConsolFun.AVERAGE);
		case PERCENT -> new ChannelDef(DsType.GAUGE, 0, 100, ConsolFun.AVERAGE);
		case ON_OFF -> new ChannelDef(DsType.GAUGE, 0, 1, ConsolFun.AVERAGE);
		case CUMULATED_SECONDS, CUMULATED_WATT_HOURS ->
			new ChannelDef(DsType.GAUGE, Double.NaN, Double.NaN, ConsolFun.MAX);
		};
	}

	/**
	 * Gets an existing RrdDb.
	 * 
	 * @param channelAddress the ChannelAddress
	 * @param rrdDbId        the id of the rrdDb
	 * @return the RrdDb or null
	 */
	private RrdDb getExistingRrdDb(//
			final ChannelAddress channelAddress, //
			final String rrdDbId //
	) {
		var file = getDbFile(channelAddress, rrdDbId);
		if (!file.exists()) {
			return null;
		}
		try {
			return RrdDb.getBuilder() //
					.setBackendFactory(this.factory) //
					// .setPool(RrdDbPool.getInstance()) //
					// ^^ is not used anymore because of caching
					// problems when overwriting the old database file
					.setPath(file.toURI()) //
					.build();
		} catch (IOException e) {
			this.log.error("Unable to open existing RrdDb", e);
			return null;
		}
	}

	private static File getDbFile(//
			final ChannelAddress channelAddress, //
			final String rrdDbId //
	) {
		return getDbFile(channelAddress, rrdDbId, false);
	}

	private static File getDbFile(//
			final ChannelAddress channelAddress, //
			final String rrdDbId, //
			final boolean isTemp //
	) {
		final var file = Paths.get(//
				OpenemsConstants.getOpenemsDataDir(), //
				Rrd4jConstants.RRD4J_PATH, //
				rrdDbId, //
				channelAddress.getComponentId(), //
				channelAddress.getChannelId() + (isTemp ? ".tmp" : "")) //
				.toFile();
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

	/**
	 * Creates new DB.
	 * 
	 * @param rrdDbId        the id of the RrdDb
	 * @param channelAddress the {@link ChannelAddress}
	 * @param channelUnit    the {@link Unit} of the Channel
	 * @param startTime      the timestamp of the newly added data
	 * @return the {@link RrdDb}
	 * @throws IOException on error
	 */
	private RrdDb createNewDb(//
			final String rrdDbId, //
			final ChannelAddress channelAddress, //
			final Unit channelUnit, //
			final long startTime //
	) throws IOException {
		return this.versionHandler.getLatestVersion() //
				.createNewDb(new CreateDatabaseConfig(//
						rrdDbId, //
						channelUnit, //
						getDbFile(channelAddress, rrdDbId).getCanonicalPath(), //
						startTime, //
						this.factory, //
						null //
				// ^^ was "RrdDbPool.getInstance()" but is not used anymore because of caching
				// problems when overwriting the old database file
				));
	}

	/**
	 * Migrates between different versions of the OpenEMS-RRD4j Definition.
	 * 
	 * @param rrdDbId        the id of the RrdDb
	 * @param oldDb          the old {@link RrdDb} database
	 * @param channelAddress the {@link ChannelAddress}
	 * @param channelUnit    the {@link Unit} of the Channel
	 * @return new {@link RrdDb}
	 * @throws IOException on error
	 */
	private RrdDb updateRrdDbToLatestDefinition(//
			final RrdDb oldDb, //
			final String rrdDbId, //
			final ChannelAddress channelAddress, //
			final Unit channelUnit //
	) throws IOException {
		if (this.versionHandler.isUpToDate(oldDb)) {
			// No Update required
			return oldDb;
		}
		var currentVersion = VersionHandler.getVersion(oldDb);

		this.log.info("Begin migrating channel '" + channelAddress + "' from version " + currentVersion + " to "
				+ this.versionHandler.getLatestVersionNumber());
		var lastCreatedDb = oldDb;
		try {

			for (var version : this.versionHandler.getVersions()) {
				if (currentVersion >= version.getVersion()) {
					continue;
				}
				this.log.info("Start migration for " + channelAddress + " from version " + currentVersion + " to "
						+ version.getVersion());
				currentVersion = version.getVersion();

				// delete unfinished migration file if existing
				final var tmpFile = getDbFile(channelAddress, rrdDbId, true);
				if (tmpFile.exists() && tmpFile.delete()) {
					this.log.warn("Deleted unfinished migration file for channel " + channelAddress + "!");
				}

				RrdDb newDb = null;
				try {
					newDb = version.migrate(lastCreatedDb, new CreateDatabaseConfig(rrdDbId, //
							channelUnit, //
							tmpFile.getAbsolutePath(), //
							oldDb.getLastUpdateTime(), //
							this.factory, //
							null //
					// ^^ was "RrdDbPool.getInstance()" but is not used anymore because of caching
					// problems when overwriting the old database file
					));

					if (newDb == lastCreatedDb) {
						continue;
					}

					lastCreatedDb.close();
					newDb.close();

					var oldFile = getDbFile(channelAddress, rrdDbId);
					Files.move(//
							Path.of(tmpFile.toURI()), //
							Path.of(oldFile.toURI()), //
							StandardCopyOption.REPLACE_EXISTING, //
							StandardCopyOption.ATOMIC_MOVE //
					);

					lastCreatedDb = this.getExistingRrdDb(channelAddress, rrdDbId);
				} catch (RuntimeException | IOException e) {
					if (newDb != null && !newDb.isClosed()) {
						newDb.close();
					}
					throw e;
				}
			}
		} catch (RuntimeException | IOException e) {
			if (lastCreatedDb != null && !lastCreatedDb.isClosed()) {
				lastCreatedDb.close();
			}
			throw e;
		}

		return lastCreatedDb;
	}

	/**
	 * Post-Process the received data.
	 * 
	 * <p>
	 * This mainly makes sure the data has the correct resolution.
	 * 
	 * @param data       the RRD4j {@link FetchData}
	 * @param resolution the resolution in seconds
	 * @return the result array
	 * @throws IOException              on error
	 * @throws IllegalArgumentException on error
	 */
	public static double[] postProcessData(FetchData data, long resolution)
			throws IOException, IllegalArgumentException {
		var step = data.getStep();
		var input = data.getValues()[0];

		// Initialize result array
		final var result = new double[(int) ((data.getLastTimestamp() - data.getFirstTimestamp()) / resolution)];
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
			System.arraycopy(input, 0, result, 0, Math.min(result.length, input.length));
		}
		return result;
	}

	private class KeyLock {

		private final Map<String, Object> locks = new ConcurrentHashMap<>();

		public <T, E extends Exception> T lock(String key, ThrowingSupplier<T, E> supplier) throws E {
			synchronized (this.locks.computeIfAbsent(key, t -> new Object())) {
				return supplier.get();
			}
		}

	}

}
