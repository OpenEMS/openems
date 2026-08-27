package io.openems.edge.energy.api.simulation.periods;

import java.time.ZonedDateTime;

public record RawPeriod(//
		ZonedDateTime time, //
		Integer gridBuySoftLimit, //
		RawPeriodData data) {

	public record RawPeriodData(//
			int production, //
			Integer consumption, //
			Double gridBuyPrice, //
			Double gridSellPrice) {

		/**
		 * Checks whether this {@link RawPeriodData} is compatible with another
		 * instance. Compatibility is violated if any previously present optional value
		 * becomes missing.
		 *
		 * @param other the other {@link RawPeriodData} to compare with
		 * @return {@code true} if both instances are compatible, otherwise
		 *         {@code false}
		 */
		public boolean isCompatibleWith(RawPeriodData other) {
			return !isValueLost(other.consumption(), this.consumption()) //
					&& !isValueLost(other.gridBuyPrice(), this.gridBuyPrice()) //
					&& !isValueLost(other.gridSellPrice(), this.gridSellPrice());
		}

		/**
		 * Checks whether this {@link RawPeriodData} contains no optional values.
		 *
		 * @return {@code true} if all optional fields are {@code null}, otherwise
		 *         {@code false}
		 */
		public boolean hasNoOptionalValues() {
			return this.consumption() == null //
					&& this.gridBuyPrice() == null //
					&& this.gridSellPrice() == null;
		}

		private static <T> boolean isValueLost(T previous, T current) {
			return previous != null && current == null;
		}
	}
}
