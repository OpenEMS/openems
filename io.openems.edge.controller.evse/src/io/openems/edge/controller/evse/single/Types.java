package io.openems.edge.controller.evse.single;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Types.History.allReadyForCharging;
import static io.openems.edge.controller.evse.single.Types.History.allSetPointsAreZero;
import static io.openems.edge.controller.evse.single.Types.History.noSetPointsAreZero;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;

public class Types {

	private Types() {
	}

	public static class History {
		protected static final int MAX_AGE = 300; // [s]

		private final TreeMap<Instant, Entry> entries = new TreeMap<>();

		/** True once outdated entries have been cleared. */
		private boolean entriesAreFullyInitialized = false;

		/** True if Current has been set, but no ActivePower was measured. */
		private boolean appearsToBeFullyCharged = false;

		public record Entry(Integer activePower, int setPoint, boolean isReadyForCharging) {
		}

		/**
		 * Adds a {@link Entry} to {@link History} and clears outdated entries.
		 * 
		 * @param now                the timestamp
		 * @param activePower        the measured {@link EvseChargePoint} ActivePower
		 * @param setPointInWatt     the {@link SetPoint} value in [W]
		 * @param isReadyForCharging {@link EvseChargePoint.ChannelId#IS_READY_FOR_CHARGING}
		 */
		public synchronized void addEntry(Instant now, Integer activePower, int setPointInWatt,
				boolean isReadyForCharging) {
			this.entries.put(now, new Entry(activePower, setPointInWatt, isReadyForCharging));

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

	public static enum Hysteresis {
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
			 * Returns a {@link JsonSerializer} for {@link Payload.Manual}.
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
			 * Returns a {@link JsonSerializer} for a {@link Payload.Smart}.
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
