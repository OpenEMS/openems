package io.openems.edge.controller.evse.single;

import static java.util.Collections.unmodifiableNavigableMap;

import java.time.Instant;
import java.util.NavigableMap;
import java.util.TreeMap;

import io.openems.edge.evse.api.chargepoint.EvseChargePoint;

public class Types {

	private Types() {
	}

	public static class History {
		protected static final int MAX_AGE = 300; // [s]

		private final TreeMap<Instant, Entry> entries = new TreeMap<>();

		/** True once outdated entries have been cleared. */
		private boolean entriesFullyInitialized = false;

		/** True if Current has been set, but no ActivePower was measured. */
		private boolean appearsToBeFullyCharged = false;

		private record Entry(Integer activePower, int current) {
		}

		/**
		 * Adds a {@link Entry} to {@link History} and clears outdated entries.
		 * 
		 * @param now         the timestamp
		 * @param activePower the measured {@link EvseChargePoint} ActivePower
		 * @param current     the set-point Current
		 */
		public synchronized void addEntry(Instant now, Integer activePower, int current) {
			this.entries.put(now, new Entry(activePower, current));

			// Clear outdated entries; update entriesFullyInitialized
			var outdatedEntries = this.entries.headMap(now.minusSeconds(MAX_AGE));
			if (!outdatedEntries.isEmpty()) {
				this.entriesFullyInitialized = true;
			}
			outdatedEntries.clear();

			// Update AppearsToBeFullyCharged
			if (activePower != null && activePower > 500 /* [W] threshold */) {
				this.appearsToBeFullyCharged = false;
			} else if (this.entriesFullyInitialized && this.noCurrentsAreZero()) {
				this.appearsToBeFullyCharged = true;
			}
		}

		/**
		 * Gets the {@link Entry Entries} as a UnmodifiableNavigableMap.
		 * 
		 * @return NavigableMap
		 */
		public NavigableMap<Instant, Entry> getEntries() {
			return unmodifiableNavigableMap(this.entries);
		}

		/**
		 * Are all Currents Zero?.
		 * 
		 * @return boolean
		 */
		public boolean allCurrentsAreZero() {
			return this.entries.values().stream() //
					.map(Entry::current) //
					.allMatch(v -> v == 0);
		}

		/**
		 * Is no Current Zero?.
		 * 
		 * @return boolean
		 */
		public boolean noCurrentsAreZero() {
			return this.entries.values().stream() //
					.map(Entry::current) //
					.allMatch(v -> v != 0);
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
			final var entries = history.getEntries();
			if (entries.isEmpty()) {
				return Hysteresis.INACTIVE;
			}
			var lastValue = entries.lastEntry().getValue();
			if (lastValue.current == 0) {
				if (history.allCurrentsAreZero()) {
					return Hysteresis.INACTIVE; // Hysteresis finished
				} else {
					return Hysteresis.KEEP_ZERO;
				}

			} else {
				if (history.noCurrentsAreZero()) {
					return Hysteresis.INACTIVE; // Hysteresis finished
				} else {
					return Hysteresis.KEEP_CHARGING;
				}
			}
		}
	}
}
