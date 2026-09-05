package io.openems.edge.controller.evse.single;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Types.History.allReadyForCharging;
import static io.openems.edge.controller.evse.single.Types.History.allSetPointsAreZero;
import static io.openems.edge.controller.evse.single.Types.History.noSetPointsAreZero;
import static java.util.stream.IntStream.rangeClosed;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.common.math.Quantiles;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;

public class Types {

	private Types() {
	}

	public static class History {
		protected static final int MAX_AGE = 300; // [s]

		private static final Duration AUTOMATIC_PHASE_SWITCH_COOLDOWN = Duration.ofSeconds(MAX_AGE);
		private static final Duration AUTOMATIC_PHASE_SWITCH_PV_LIMIT_WINDOW = Duration.ofMinutes(2);
		private static final int AUTOMATIC_PHASE_SWITCH_MIN_SAMPLE_COUNT = 60;

		private final TreeMap<Instant, Entry> entries = new TreeMap<>();

		/** True once outdated entries have been cleared. */
		private boolean entriesAreFullyInitialized = false;

		/** True if Current has been set, but no ActivePower was measured. */
		private boolean appearsToBeFullyCharged = false;

		/** Next allowed timestamp for automatic phase switching. */
		private Instant automaticPhaseSwitchCooldownUntil;

		public enum AutomaticPhaseSwitchThresholdDirection {
			ABOVE, BELOW
		}

		public static record AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation(//
				boolean windowActive, //
				boolean shouldSwitch, //
				double currentPercentage, //
				double directionalNinetyPercentAverage, //
				int thresholdInWatt, //
				AutomaticPhaseSwitchThresholdDirection direction, //
				int sampleCount) {
		}

		public record Entry(Integer activePower, int setPoint, Integer setPointWithoutPhaseLimitation,
				boolean isReadyForCharging) {
		}

		/**
		 * Adds a {@link Entry} to {@link History} and clears outdated entries.
		 * 
		 * @param now                            the timestamp
		 * @param setPointWithoutPhaseLimitation the automatic phase-switch set-point
		 *                                       without phase limitation sample in [W]
		 * @param activePower                    the measured {@link EvseChargePoint}
		 *                                       ActivePower
		 * @param setPointInWatt                 the Set-Point value in [W]
		 * @param isReadyForCharging             {@link EvseChargePoint.ChannelId#IS_READY_FOR_CHARGING}
		 */
		public synchronized void addEntry(Instant now, Integer activePower, int setPointInWatt,
				Integer setPointWithoutPhaseLimitation, boolean isReadyForCharging) {
			this.entries.put(now,
					new Entry(activePower, setPointInWatt, setPointWithoutPhaseLimitation, isReadyForCharging));
			this.cleanupAutomaticPhaseSwitchCooldown(now);

			// Clear outdated entries; update entriesFullyInitialized
			var outdatedEntries = this.entries.headMap(now.minusSeconds(MAX_AGE));
			if (!outdatedEntries.isEmpty()) {
				this.entriesAreFullyInitialized = true;
			}
			outdatedEntries.clear();

			// Update AppearsToBeFullyCharged
			if (activePower != null && activePower > 500 /* [W] threshold */) {
				this.appearsToBeFullyCharged = false;

			} else if (this.entriesAreFullyInitialized //
					&& this.entries.values().stream() //
							.map(Entry::setPoint) //
							.allMatch(sp -> sp != 0)) {
				// Fully initialized, no set-points are zero but activePower is null/little
				this.appearsToBeFullyCharged = true;
			}
		}

		/**
		 * Gets the timestamp until automatic phase switching is blocked.
		 *
		 * @return the cooldown-until timestamp; null if no cooldown is active
		 */
		public synchronized Instant getAutomaticPhaseSwitchCooldownUntil() {
			return this.automaticPhaseSwitchCooldownUntil;
		}

		/**
		 * Checks whether automatic phase switching is currently in cooldown.
		 *
		 * <p>
		 * Outdated cooldown values are cleared automatically.
		 *
		 * @param now current timestamp
		 * @return true if cooldown is active; else false
		 */
		public synchronized boolean isAutomaticPhaseSwitchInCooldown(Instant now) {
			this.cleanupAutomaticPhaseSwitchCooldown(now);
			return this.automaticPhaseSwitchCooldownUntil != null;
		}

		/**
		 * Starts the cooldown for automatic phase switching.
		 *
		 * @param now current timestamp
		 */
		public synchronized void setAutomaticPhaseSwitchCooldown(Instant now) {
			this.automaticPhaseSwitchCooldownUntil = now.plus(AUTOMATIC_PHASE_SWITCH_COOLDOWN);
		}

		private void cleanupAutomaticPhaseSwitchCooldown(Instant now) {
			if (this.automaticPhaseSwitchCooldownUntil != null
					&& !now.isBefore(this.automaticPhaseSwitchCooldownUntil)) {
				this.automaticPhaseSwitchCooldownUntil = null;
			}
		}

		/**
		 * Registers one set-point-without-phase-limitation sample for automatic phase
		 * switching and returns rolling-window status details for logging and decision
		 * making.
		 *
		 * <p>
		 * Samples are kept in a rolling 2-minute window.
		 *
		 * @param now                            current timestamp
		 * @param setPointWithoutPhaseLimitation the set-point value without phase
		 *                                       limitation in [W]
		 * @param thresholdInWatt                threshold in [W]
		 * @param direction                      threshold direction (ABOVE/BELOW)
		 * @return detailed evaluation including rolling-window average and switch
		 *         decision
		 */
		public synchronized AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(
				Instant now, int setPointWithoutPhaseLimitation, int thresholdInWatt,
				AutomaticPhaseSwitchThresholdDirection direction) {
			return this.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(now, thresholdInWatt,
					direction, AUTOMATIC_PHASE_SWITCH_PV_LIMIT_WINDOW, setPointWithoutPhaseLimitation);
		}

		/**
		 * Evaluates the directional 90%-average for a custom recent window.
		 *
		 * <p>
		 * Assumes
		 * {@link #evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(Instant, int, int, AutomaticPhaseSwitchThresholdDirection)}
		 * was called for this cycle so the current sample is already registered.
		 *
		 * @param now             current timestamp
		 * @param thresholdInWatt threshold in [W]
		 * @param direction       threshold direction (ABOVE/BELOW)
		 * @param window          window duration
		 * @return detailed evaluation for the requested window
		 */
		public synchronized AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(
				Instant now, int thresholdInWatt, AutomaticPhaseSwitchThresholdDirection direction, Duration window) {
			return this.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(now, thresholdInWatt,
					direction, window, null);
		}

		/**
		 * Evaluates automatic phase-switching for a custom window and optionally adds
		 * the current sample inline.
		 *
		 * @param now                                   current timestamp
		 * @param thresholdInWatt                       threshold in [W]
		 * @param direction                             threshold direction
		 * @param window                                evaluation window
		 * @param currentSetPointWithoutPhaseLimitation optional current sample in [W]
		 * @return evaluation details for this window
		 */
		public synchronized AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(
				Instant now, int thresholdInWatt, AutomaticPhaseSwitchThresholdDirection direction, Duration window,
				Integer currentSetPointWithoutPhaseLimitation) {
			final var samples = new ArrayList<>(this.entries //
					.tailMap(now.minus(window), true) //
					.values().stream() //
					.map(Entry::setPointWithoutPhaseLimitation) //
					.filter(Objects::nonNull) //
					.toList());
			if (currentSetPointWithoutPhaseLimitation != null) {
				samples.add(currentSetPointWithoutPhaseLimitation);
			}
			return this.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationSamples(samples, thresholdInWatt,
					direction);
		}

		private AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationSamples(
				List<Integer> samples, int thresholdInWatt, AutomaticPhaseSwitchThresholdDirection direction) {
			if (samples.isEmpty()) {
				return new AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation(false, false, 0, 0,
						thresholdInWatt, direction, 0);
			}

			final var directionalNinetyPercentAverage = calculateDirectionalNinetyPercentAverage(samples, direction);
			final var thresholdReached = switch (direction) {
			case ABOVE -> directionalNinetyPercentAverage >= thresholdInWatt;
			case BELOW -> directionalNinetyPercentAverage <= thresholdInWatt;
			};
			final var sampleCountReached = samples.size() >= AUTOMATIC_PHASE_SWITCH_MIN_SAMPLE_COUNT;
			final var currentPercentage = calculateDirectionalPercentage(directionalNinetyPercentAverage,
					thresholdInWatt, direction);

			return new AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation(true,
					sampleCountReached && thresholdReached, currentPercentage, directionalNinetyPercentAverage,
					thresholdInWatt, direction, samples.size());
		}

		/**
		 * Registers one set-point-without-phase-limitation sample for automatic phase
		 * switching and evaluates if the directional 90%-average over the last 2
		 * minutes crosses the threshold.
		 *
		 * @param now                            current timestamp
		 * @param setPointWithoutPhaseLimitation the set-point value without phase
		 *                                       limitation in [W]
		 * @param thresholdInWatt                threshold in [W]
		 * @param direction                      threshold direction (ABOVE/BELOW)
		 * @return true if a phase switch should be initiated
		 */
		public synchronized boolean addAutomaticPhaseSwitchSetPointWithoutPhaseLimitationAndEvaluate(Instant now,
				int setPointWithoutPhaseLimitation, int thresholdInWatt,
				AutomaticPhaseSwitchThresholdDirection direction) {
			return this //
					.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(now, setPointWithoutPhaseLimitation,
							thresholdInWatt, direction)
					.shouldSwitch();
		}

		private static double calculateDirectionalNinetyPercentAverage(List<Integer> samples,
				AutomaticPhaseSwitchThresholdDirection direction) {
			if (samples.isEmpty()) {
				return 0;
			}

			final var percentileValues = Quantiles.percentiles() //
					.indexes(rangeClosed(1, 100).boxed().toList()) //
					.compute(samples);

			final var selectedPercentiles = switch (direction) {
			case ABOVE -> rangeClosed(1, 90);
			case BELOW -> rangeClosed(11, 100);
			};

			return selectedPercentiles //
					.mapToDouble(percentileValues::get) //
					.average() //
					.orElse(0);
		}

		private static double calculateDirectionalPercentage(double directionalAverage, int thresholdInWatt,
				AutomaticPhaseSwitchThresholdDirection direction) {
			if (thresholdInWatt <= 0) {
				return 0;
			}
			return switch (direction) {
			case ABOVE -> directionalAverage * 100.0 / thresholdInWatt;
			case BELOW -> directionalAverage <= 0 ? 100.0 : thresholdInWatt * 100.0 / directionalAverage;
			};
		}

		/**
		 * Stream all {@link Entry}s.
		 * 
		 * @return {@link Stream} of {@link Entry}s
		 */
		public synchronized Stream<Entry> streamAll() {
			if (this.entries.isEmpty()) {
				return Stream.empty();
			}

			return this.entries //
					.values().stream();
		}

		/**
		 * Stream all but the last Entry value.
		 * 
		 * @return {@link Stream} of {@link Entry}s
		 */
		public synchronized Stream<Entry> streamAllButLast() {
			if (this.entries.isEmpty()) {
				return Stream.empty();
			}
			return this.entries.headMap(this.entries.lastKey(), false) //
					.values().stream();
		}

		/**
		 * Gets the entry with the highest key.
		 * 
		 * @return Instant and Entry or null
		 */
		public Map.Entry<Instant, Entry> getLastEntry() {
			return this.entries.lastEntry();
		}

		/**
		 * Stream all entries as {@link Map.Entry} with active power greater than 0 and
		 * isReadyForCharging = true.
		 *
		 * @return {@link Stream} of {@link Map.Entry}s with Instant keys and Entry
		 *         values
		 */
		public synchronized Stream<Map.Entry<Instant, Entry>> streamAllWithActivePowerAndReadyForCharging() {
			if (this.entries.isEmpty()) {
				return Stream.empty();
			}
			return this.entries.entrySet().stream()
					.filter(e -> e.getValue().activePower != null && e.getValue().activePower > 0)
					.filter(e -> e.getValue().isReadyForCharging);
		}

		/**
		 * True if all Entries are populated.
		 * 
		 * @return true or false
		 */
		public boolean isEntriesAreFullyInitialized() {
			return this.entriesAreFullyInitialized;
		}

		/**
		 * All Set-Point are Zero?.
		 * 
		 * @param entries {@link Stream} of {@link Entry}s
		 * @return boolean
		 */
		public static boolean allSetPointsAreZero(Stream<Entry> entries) {
			return entries //
					.map(Entry::setPoint) //
					.allMatch(sp -> sp == 0);
		}

		/**
		 * Is no Set-Point Zero?.
		 * 
		 * @param entries {@link Stream} of {@link Entry}s
		 * @return boolean
		 */
		public static boolean noSetPointsAreZero(Stream<Entry> entries) {
			return entries //
					.map(Entry::setPoint) //
					.allMatch(sp -> sp != 0);
		}

		/**
		 * Are all Active-Power values available and zero?.
		 * 
		 * @param entries {@link Stream} of {@link Entry}s
		 * @return boolean
		 */
		public static boolean allActivePowersAreZero(Stream<Entry> entries) {
			return entries //
					.map(Entry::activePower) //
					.allMatch(ap -> ap != null && ap == 0);
		}

		/**
		 * Are all {@link EvseChargePoint.ChannelId#IS_READY_FOR_CHARGING}?.
		 * 
		 * @param entries {@link Stream} of {@link Entry}s
		 * @return boolean
		 */
		public static boolean allReadyForCharging(Stream<Entry> entries) {
			return entries //
					.allMatch(e -> e.isReadyForCharging);
		}

		public synchronized boolean getAppearsToBeFullyCharged() {
			return this.appearsToBeFullyCharged;
		}

		/**
		 * Set AppearsToBeFullyCharged to false.
		 */
		public synchronized void unsetAppearsToBeFullyCharged() {
			this.appearsToBeFullyCharged = false;
		}

		@Override
		public final String toString() {
			return toStringHelper(History.class) //
					.add("entries", this.entries.size()) //
					.toString();
		}
	}

	public enum Hysteresis {
		INACTIVE, KEEP_CHARGING, KEEP_ZERO;

		/**
		 * Calculates {@link Hysteresis} from {@link History}.
		 * 
		 * @param history the {@link History}
		 * @return the {@link Hysteresis}
		 */
		public static Hysteresis from(History history) {
			final var lastEntry = history.getLastEntry();
			if (lastEntry == null) {
				return Hysteresis.INACTIVE;
			}
			if (!allReadyForCharging(history.streamAll())) {
				// Allow charging if EV just became ready
				return Hysteresis.KEEP_CHARGING;
			}

			if (lastEntry.getValue().setPoint == 0) {
				if (allSetPointsAreZero(history.streamAllButLast())) {
					// All set-points are zero -> Hysteresis finished
					return Hysteresis.INACTIVE;
				} else {
					// Latest set-point is zero; others are not -> KEEP_ZERO
					return Hysteresis.KEEP_ZERO;
				}

			} else {
				if (noSetPointsAreZero(history.streamAllButLast())) {
					// All set-points are non-zero -> Hysteresis finished
					return Hysteresis.INACTIVE;
				} else {
					// Latest set-point is non-zero; others are not -> KEEP_CHARGING
					return Hysteresis.KEEP_CHARGING;
				}
			}
		}
	}

	public sealed interface Payload {

		public static record Manual(Mode mode) implements Payload {
			/**
			 * Returns a {@link JsonSerializer} for {@link Manual}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<Manual> serializer() {
				return jsonObjectSerializer(json -> {
					return new Manual(//
							json.getEnum("mode", Mode.class));
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("mode", obj.mode) //
							.build();
				});
			}
		}

		public static record Smart(int sessionEnergyMinimum) implements Payload {
			/**
			 * Returns a {@link JsonSerializer} for a {@link Smart}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<Smart> serializer() {
				return jsonObjectSerializer(Smart.class, json -> {
					return new Smart(//
							json.getInt("sessionEnergyMinimum") //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("sessionEnergyMinimum", obj.sessionEnergyMinimum) //
							.build();
				});
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link Payload}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Payload> serializer() {
			final var polymorphicSerializer = PolymorphicSerializer.<Payload>create() //
					.add(Manual.class, Manual.serializer(), Manual.class.getSimpleName()) //
					.add(Smart.class, Smart.serializer(), Smart.class.getSimpleName()) //
					.build();

			return jsonSerializer(Payload.class, json -> {
				return json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("class"));
			}, obj -> {
				return polymorphicSerializer.serialize(obj);
			});
		}
	}
}
