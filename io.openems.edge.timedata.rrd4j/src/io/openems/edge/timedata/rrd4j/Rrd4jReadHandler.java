package io.openems.edge.timedata.rrd4j;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.rrd4j.core.Archive;
import org.rrd4j.core.RrdDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.timedata.api.Timeranges;

@Component(//
		scope = ServiceScope.SINGLETON, //
		service = { Rrd4jReadHandler.class } //
)
public class Rrd4jReadHandler {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Rrd4jSupplier rrd4jSupplier;

	@Activate
	public Rrd4jReadHandler() {
	}

	private static int calculateIndex(Archive archive, long timestamp) throws IOException {
		return (int) ((timestamp - archive.getStartTime()) / archive.getArcStep());
	}

	/**
	 * Gets a list of the archives from the given database, sorted by their arc step
	 * size ascending. The step size can be obtained with
	 * {@link Archive#getArcStep()}.
	 * 
	 * @param db the archives of which database
	 * @return a sorted list of the archives
	 * @throws IOException Thrown in case of I/O error.
	 */
	protected static List<Archive> getArchivesSortedByArcStep(RrdDb db) throws IOException {
		try {
			return IntStream.range(0, db.getArcCount()) //
					.mapToObj(i -> db.getArchive(i)) //
					.sorted((o1, o2) -> {
						try {
							return Long.compare(o1.getArcStep(), o2.getArcStep());
						} catch (IOException e) {
							throw new RuntimeIOException(e);
						}
					}) //
					.toList();
		} catch (RuntimeIOException e) {
			throw e.getIOException();
		}
	}

	// CHECKSTYLE:OFF
	private static class RuntimeIOException extends RuntimeException {
		// CHECKSTYLE:ON
		private static final long serialVersionUID = 266138749715908697L;

		private final IOException ioexception;

		public RuntimeIOException(IOException ioexception) {
			super(ioexception);
			this.ioexception = ioexception;
		}

		// CHECKSTYLE:OFF
		public IOException getIOException() {
			// CHECKSTYLE:ON
			return this.ioexception;
		}

	}

	private static Timeranges getTimerangesOfNotSendData(RrdDb db, long start) throws IOException {
		final var timeranges = new Timeranges();

		final var sortedArchives = getArchivesSortedByArcStep(db);
		var nextEnd = -1L;
		for (final var archive : sortedArchives) {
			final var startTime = archive.getStartTime();

			final var robin = archive.getRobin(0);
			final var startIndex = Math.max(calculateIndex(archive, start) + 1 /* first timestamp exclusive */, 0);
			final var endIndex = nextEnd == -1L ? robin.getSize() : calculateIndex(archive, nextEnd);

			if (nextEnd == -1L || nextEnd > startTime) {
				nextEnd = startTime;
			}

			final var values = robin.getValues(startIndex, endIndex - startIndex);
			for (var j = 0; j < values.length; j++) {
				final var value = values[j];
				if (Double.isNaN(value) //
						// 0.0 => data got send successfully
						|| value == 0.0) {
					continue;
				}

				final var timestamp = archive.getStartTime() + archive.getArcStep() * (startIndex + j);
				timeranges.insert(timestamp);
			}

			// timerange got fully fetched
			if (startIndex != 0) {
				break;
			}
		}
		return timeranges;
	}

	/**
	 * Gets the {@link Timeranges} to data which got not send. The not send data
	 * gets determined with the notSendChannel and the lastResendTimestamp.
	 * 
	 * @param rrdDbId             the id of the rrdb
	 * @param notSendChannel      the channel with the timestamps where the data got
	 *                            not send
	 * @param lastResendTimestamp the timstamp of the last resend
	 * @param debugMode           if debugMode is active
	 * @return the {@link Timeranges}
	 * @throws OpenemsNamedException on error
	 */
	public Timeranges getResendTimeranges(//
			final String rrdDbId, //
			final ChannelAddress notSendChannel, //
			final long lastResendTimestamp, //
			final boolean debugMode //
	) throws OpenemsNamedException {
		final Channel<?> channel;
		try {
			channel = this.componentManager.getChannel(notSendChannel);
		} catch (Exception e) {
			// unable to get channel
			throw new OpenemsException("RRD4j Database for " + notSendChannel + " is missing");
		}
		try (final var database = this.rrd4jSupplier.getExistingUpdatedRrdDb(//
				rrdDbId, channel.address(), channel.channelDoc().getUnit())) {
			if (database == null) {
				throw new OpenemsException("RRD4j Database for " + notSendChannel + " is missing");
			}
			return getTimerangesOfNotSendData(database, lastResendTimestamp);
		} catch (IOException e) {
			throw new OpenemsException("Unable to query database.", e);
		}
	}

	/**
	 * Queries data to resend.
	 * 
	 * @param rrdDbId   the id of the rrdb
	 * @param fromDate  the start date
	 * @param toDate    the end date
	 * @param channels  the channels to resend
	 * @param debugMode if debugMode is active
	 * @return the query result; possibly null
	 * @throws OpenemsNamedException on error
	 */
	public SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> queryResendData(//
			final String rrdDbId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels, //
			final boolean debugMode //
	) throws OpenemsNamedException {
		final var fromTime = fromDate.toEpochSecond();
		final var toTime = toDate.toEpochSecond();

		final var resultMap = new TreeMap<Long, SortedMap<ChannelAddress, JsonElement>>();

		for (var channelAddress : channels) {
			final Channel<?> channel;
			try {
				channel = this.componentManager.getChannel(channelAddress);
			} catch (Exception e) {
				// unable to get channel
				if (debugMode) {
					this.log.warn("Unable to query RRD4j", e);
				}
				continue;
			}
			try (final var database = this.rrd4jSupplier.getExistingUpdatedRrdDb(//
					rrdDbId, channel.address(), channel.channelDoc().getUnit())) {
				if (database == null) {
					if (debugMode) {
						this.log.warn("Unable to query RRD4j: " //
								+ "RRD4j Database for " + channelAddress + " is missing");
					}
					continue;
				}

				for (int i = 0; i < database.getArcCount(); i++) {
					final var archive = database.getArchive(i);
					final var arcStep = archive.getArcStep();

					final var adjustSeconds = arcStep - Rrd4jConstants.DEFAULT_STEP_SECONDS;

					final var start = Math.max(fromTime - adjustSeconds, archive.getStartTime());
					final var stop = Math.min(toTime - adjustSeconds, archive.getEndTime());
					if (start > archive.getEndTime()) {
						continue;
					}
					if (stop < archive.getStartTime()) {
						continue;
					}

					final var fetchData = database.createFetchRequest(archive.getConsolFun(), start, stop, arcStep) //
							.fetchData();

					final var timestamps = fetchData.getTimestamps();
					final var values = fetchData.getValues()[0];
					for (int j = 0; j < values.length; j++) {
						final var value = values[j];
						if (Double.isNaN(value)) {
							continue;
						}
						final var timestamp = timestamps[j] + adjustSeconds;

						if (timestamp < fromTime //
								|| timestamp > toTime) {
							continue;
						}

						// return timestamps in milliseconds
						resultMap.computeIfAbsent(timestamp * 1000, t -> new TreeMap<>()) //
								.put(channelAddress, new JsonPrimitive(value));
					}
				}

			} catch (Exception e) {
				if (debugMode) {
					this.log.warn("Unable to query RRD4j", e);
				}
			}
		}

		return resultMap;
	}

	/**
	 * Queries historic data.
	 *
	 * @param rrdDbId    the id of the rrdb
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @param debugMode  if debugMode is active
	 * @return the query result; possibly null
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			final String rrdDbId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels, //
			final Resolution resolution, //
			final boolean debugMode //
	) throws OpenemsNamedException {
		final var timezone = fromDate.getZone();
		final var table = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();

		final var fromTimestamp = fromDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
		final var toTimeStamp = toDate.withZoneSameInstant(ZoneOffset.UTC).toEpochSecond();
		var errorCounter = 0;

		try {
			for (var channelAddress : channels) {
				final Channel<?> channel;
				try {
					channel = this.componentManager.getChannel(channelAddress);
				} catch (Exception e) {
					if (debugMode) {
						this.log.warn("Unable to get channel " + channelAddress, e);
					}
					errorCounter++;
					continue;
				}
				final var chDef = Rrd4jSupplier.getDsDefForChannel(channel.channelDoc().getUnit());
				final double[] result;
				try (final var database = this.rrd4jSupplier.getExistingUpdatedRrdDb(rrdDbId, channel.address(),
						channel.channelDoc().getUnit())) {
					if (database == null) {
						if (debugMode) {
							this.log.warn(
									"Unable to query RRD4j" + "RRD4j Database for " + channelAddress + " is missing");
						}
						errorCounter++;
						continue;
					}

					final var fetchedData = database
							.createFetchRequest(chDef.consolFun(), fromTimestamp, toTimeStamp, resolution.toSeconds())
							.fetchData();
					// Post-Process data
					result = Rrd4jSupplier.postProcessData(fetchedData, resolution.toSeconds());
				} catch (Exception e) {
					if (debugMode) {
						this.log.warn("Unable to query RRD4j " + channelAddress, e);
					}
					errorCounter++;
					continue;
				}
				try {
					for (var i = 0; i < result.length; i++) {
						final var timestamp = fromTimestamp + (i * resolution.toSeconds());

						// Prepare result table row
						var timestampInstant = Instant.ofEpochSecond(timestamp);
						var dateTime = ZonedDateTime.ofInstant(timestampInstant, ZoneOffset.UTC) //
								.withZoneSameInstant(timezone);

						final var tableRow = table.computeIfAbsent(dateTime, t -> new TreeMap<>());

						final var value = result[i];
						tableRow.put(channelAddress, Double.isNaN(value) //
								? JsonNull.INSTANCE
								: new JsonPrimitive(value));
					}
				} catch (Exception e) {
					if (debugMode) {
						this.log.warn("Unable to query RRD4j " + channelAddress, e);
					}
					errorCounter++;
				}
			}

			// If no Channel can be read successfully: throw exception; otherwise return the
			// available data
			if (errorCounter == channels.size()) {
				throw new OpenemsException("None of the requested Channels is available: "
						+ channels.stream().map(c -> c.toString()).collect(Collectors.joining(", ")));
			}

		} catch (Exception e) {
			throw new OpenemsException("Unable to read historic data: " + e.getMessage());
		}
		return table;
	}

	/**
	 * Queries historic energy.
	 *
	 * @param rrdDbId   the id of the rrdb
	 * @param fromDate  the From-Date
	 * @param toDate    the To-Date
	 * @param channels  the Channels
	 * @param debugMode if debugMode is active
	 * @return the query result; possibly null
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			final String rrdDbId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels, //
			final boolean debugMode //
	) throws OpenemsNamedException {
		final var fromTimestamp = fromDate.toEpochSecond();
		final var toTimestamp = toDate.toEpochSecond();

		final var result = channels.stream() //
				.collect(Collectors.toMap(Function.identity(), channelAddress -> {
					final Channel<?> channel;
					try {
						channel = this.componentManager.getChannel(channelAddress);
					} catch (Exception e) {
						// unable to get channel
						if (debugMode) {
							this.log.warn("Unable to query RRD4j", e);
						}
						return JsonNull.INSTANCE;
					}
					try (final var database = this.rrd4jSupplier.getExistingUpdatedRrdDb(//
							rrdDbId, channel.address(), channel.channelDoc().getUnit())) {
						if (database == null) {
							if (debugMode) {
								this.log.warn("Unable to query RRD4j: " //
										+ "RRD4j Database for " + channelAddress + " is missing");
							}
							return JsonNull.INSTANCE;
						}

						var first = getFirstValueBefore(database, fromTimestamp);

						// minus 1h to include the last timestamp of the requested day
						final var last = getLastValue(database, fromTimestamp, toTimestamp);

						// get first value in range
						if (Double.isNaN(first) && !Double.isNaN(last)) {
							return new JsonPrimitive(last);
						}

						if (Double.isNaN(first) || Double.isNaN(last)) {
							return JsonNull.INSTANCE;
						}
						return new JsonPrimitive(last - first);
					} catch (Exception e) {
						if (debugMode) {
							this.log.warn("Unable to query RRD4j", e);
						}
						return JsonNull.INSTANCE;
					}
				}, (t, u) -> t, TreeMap::new));

		// If no Channel can be read successfully: throw exception
		if (result.values().stream().allMatch(JsonElement::isJsonNull)) {
			throw new OpenemsException("Unable to read historic data: None of the requested Channels is available: "
					+ channels.stream().map(c -> c.toString()).collect(Collectors.joining(", ")));
		}

		return result;
	}

	/**
	 * Queries historic energy per period.
	 *
	 * <p>
	 * This is for use-cases where you want to get the energy for each period (with
	 * {@link Resolution}) per Channel, e.g. to visualize energy in a histogram
	 * chart. For each period the energy is calculated by subtracting first value of
	 * the period from the last value of the period.
	 *
	 * @param rrdDbId    the id of the rrdb
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @param debugMode  if debugMode is active
	 * @return the query result; possibly null
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			final String rrdDbId, //
			final ZonedDateTime fromDate, //
			final ZonedDateTime toDate, //
			final Set<ChannelAddress> channels, //
			final Resolution resolution, //
			final boolean debugMode //
	) throws OpenemsNamedException {
		return streamRanges(fromDate, toDate, resolution) //
				.collect(Collectors.toMap(Range::from, range -> {
					try {
						return this.queryHistoricEnergy(rrdDbId, range.from(), range.to(), channels, debugMode);
					} catch (OpenemsNamedException e) {
						return channels.stream().collect(Collectors.toMap(Function.identity(),
								channel -> JsonNull.INSTANCE, (t, u) -> t, TreeMap::new));
					}
				}, (t, u) -> t, TreeMap::new));
	}

	private static record Range(ZonedDateTime from, ZonedDateTime to) {
	}

	private static Stream<Range> streamRanges(//
			final ZonedDateTime from, //
			final ZonedDateTime to, //
			final Resolution resolution //
	) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("'from' needs to be lower than 'to'!");
		}
		final var builder = Stream.<Range>builder();

		var fromRange = from;
		var toRange = increase(from, resolution);
		if (toRange.isAfter(to)) {
			toRange = to;
		}

		while (!fromRange.equals(toRange)) {
			builder.accept(new Range(fromRange, toRange));
			fromRange = toRange;
			toRange = increase(toRange, resolution);
			if (toRange.isAfter(to)) {
				toRange = to;
			}
		}

		return builder.build();
	}

	private static ZonedDateTime increase(ZonedDateTime date, Resolution resolution) {
		return switch (resolution.getUnit()) {
		case DAYS, HALF_DAYS, HOURS, SECONDS, MINUTES, MILLIS, NANOS, MICROS -> {
			yield date.plus(resolution.getValue(), resolution.getUnit());
		}
		case CENTURIES, DECADES, ERAS, FOREVER, MILLENNIA, YEARS, WEEKS -> {
			throw new UnsupportedOperationException();
		}
		case MONTHS -> date.plusMonths(resolution.getValue());
		};
	}

	/**
	 * Gets the latest known value for the given {@link ChannelAddress}.
	 *
	 * @param rrdDbId        the id of the rrdb
	 * @param channelAddress the ChannelAddress to be queried
	 * @return the latest known value or Empty
	 */
	public CompletableFuture<Optional<Object>> getLatestValue(//
			final String rrdDbId, //
			final ChannelAddress channelAddress //
	) {
		return CompletableFuture.supplyAsync(() -> {
			final Channel<?> channel;
			try {
				channel = this.componentManager.getChannel(channelAddress);
			} catch (Exception e) {
				// unable to get channel
				this.log.warn("Unable to query RRD4j", e);
				return Optional.empty();
			}

			try (var database = this.rrd4jSupplier.getExistingUpdatedRrdDb(rrdDbId, channelAddress,
					channel.channelDoc().getUnit())) {
				if (database == null) {
					return Optional.empty();
				}

				// search for last value in robin
				final var robin = database.getArchive(0).getRobin(0);
				for (int i = robin.getSize() - 1; i >= 0; i--) {
					final var value = robin.getValue(i);
					if (Double.isNaN(value)) {
						continue;
					}
					return Optional.of(value);
				}

				return Optional.empty();
			} catch (Exception e) {
				return Optional.empty();
			}
		});
	}

	/**
	 * Gets the latest known value for the given {@link ChannelAddress} of a not
	 * existing channel.
	 * 
	 * <p>
	 * Gets the latest known value even if the ChannelAddress is not longer
	 * existing.
	 *
	 * @param rrdDbId        the id of the rrdb
	 * @param channelAddress the ChannelAddress to be queried
	 * @param unit           unit
	 * @return the latest known value or Empty
	 */
	public CompletableFuture<Optional<Object>> getLatestValueOfNotExistingChannel(//
			final String rrdDbId, //
			final ChannelAddress channelAddress, //
			final Unit unit //
	) {
		return CompletableFuture.supplyAsync(() -> {

			try (var database = this.rrd4jSupplier.getExistingUpdatedRrdDb(rrdDbId, channelAddress, unit)) {
				if (database == null) {
					return Optional.empty();
				}

				// search for last value in robin
				final var robin = database.getArchive(0).getRobin(0);
				for (int i = robin.getSize() - 1; i >= 0; i--) {
					final var value = robin.getValue(i);
					if (Double.isNaN(value)) {
						continue;
					}
					return Optional.of(value);
				}

				return Optional.empty();
			} catch (Exception e) {
				return Optional.empty();
			}
		});
	}

	private static double getFirstValueBefore(RrdDb database, long endTimestamp) throws IOException {
		final var archive = database.getArchive(0);
		if (archive.getStartTime() > endTimestamp) {
			// value out of range
			return Double.NaN;
		}

		final var robin = archive.getRobin(0);

		final var endIndex = calculateIndex(archive, endTimestamp) - 1 /* exclusive */;

		for (int i = Math.min(endIndex, robin.getSize() - 1); i > 0; i--) {
			final var value = robin.getValue(i);
			if (!Double.isNaN(value)) {
				return value;
			}
		}
		return Double.NaN;
	}

	private static double getLastValue(RrdDb database, long startTimestamp, long endTimestamp) throws IOException {
		if (startTimestamp > endTimestamp) {
			throw new IllegalArgumentException("'startTimestamp' needs to be smaller than 'endTimestamp'");
		}

		final var archive = database.getArchive(0);
		final var start = archive.getStartTime();

		if (archive.getEndTime() < startTimestamp) {
			// value out of range
			return Double.NaN;
		}
		if (start > endTimestamp) {
			// value out of range
			return Double.NaN;
		}

		final var step = archive.getArcStep();
		final var robin = archive.getRobin(0);

		var startIndex = (int) ((startTimestamp - start) / step);
		var endIndex = (int) ((endTimestamp - start) / step) - 1 /* exclusive */;

		if (startIndex < 0) {
			startIndex = 0;
		}

		for (int i = Math.min(endIndex, robin.getSize() - 1); i > startIndex; i--) {
			final var value = robin.getValue(i);
			if (!Double.isNaN(value)) {
				return value;
			}
		}
		return Double.NaN;
	}

}
