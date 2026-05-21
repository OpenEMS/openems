package io.openems.edge.energy.api.simulation.periods;

import java.util.Optional;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

public class PeriodData {
	private final int production;
	private final Prediction consumption;
	private final Price gridBuyPrice;
	private final Price gridSellPrice;

	private PeriodData(Builder builder) {
		this.production = builder.production;
		this.consumption = builder.consumption;
		this.gridBuyPrice = builder.gridBuyPrice;
		this.gridSellPrice = builder.gridSellPrice;
	}

	/**
	 * Returns the production prediction for this period.
	 *
	 * @return the production prediction
	 */
	public int production() {
		return this.production;
	}

	/**
	 * Returns the consumption prediction for this period.
	 *
	 * @return the optional consumption {@link Prediction}
	 */
	public Optional<Prediction> consumption() {
		return Optional.ofNullable(this.consumption);
	}

	/**
	 * Returns the grid buy price for this period.
	 *
	 * @return the optional {@link Price}
	 */
	public Optional<Price> gridBuyPrice() {
		return Optional.ofNullable(this.gridBuyPrice);
	}

	/**
	 * Returns the grid sell price for this period.
	 *
	 * @return the optional {@link Price}
	 */
	public Optional<Price> gridSellPrice() {
		return Optional.ofNullable(this.gridSellPrice);
	}

	/**
	 * Checks whether both {@link PeriodData} instances have consistent value
	 * presence for all optional fields (either both values are present or both are
	 * absent).
	 *
	 * @param other the other {@link PeriodData} instance to compare with
	 * @return true if both instances have consistent value presence for all fields,
	 *         false otherwise
	 */
	public boolean hasConsistentValuePresence(PeriodData other) {
		return samePresence(this.consumption, other.consumption) //
				&& samePresence(this.gridBuyPrice, other.gridBuyPrice) //
				&& samePresence(this.gridSellPrice, other.gridSellPrice);
	}

	/**
	 * Creates a new {@link Builder} instance for constructing a {@link PeriodData}.
	 *
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		return "PeriodData[" //
				+ "production=" + this.production //
				+ ", consumption=" + this.consumption //
				+ ", gridBuyPrice=" + this.gridBuyPrice //
				+ ", gridSellPrice=" + this.gridSellPrice //
				+ "]";
	}

	private static <T> boolean samePresence(T a, T b) {
		return (a != null) == (b != null);
	}

	public static class Builder {
		private int production = 0;
		private Prediction consumption;
		private Price gridBuyPrice;
		private Price gridSellPrice;

		/**
		 * Sets the production prediction value.
		 *
		 * @param production the production prediction to set
		 * @return this builder instance
		 */
		public Builder withProduction(int production) {
			this.production = production;
			return this;
		}

		/**
		 * Sets the consumption prediction value.
		 *
		 * @param consumption the consumption {@link Prediction} to set
		 * @return this builder instance
		 */
		public Builder withConsumption(Prediction consumption) {
			this.consumption = consumption;
			return this;
		}

		/**
		 * Sets the grid buy price.
		 *
		 * @param price the {@link Price} to set
		 * @return this builder instance
		 */
		public Builder withGridBuyPrice(Price price) {
			this.gridBuyPrice = price;
			return this;
		}

		/**
		 * Sets the grid sell price.
		 *
		 * @param price the {@link Price} to set
		 * @return this builder instance
		 */
		public Builder withGridSellPrice(Price price) {
			this.gridSellPrice = price;
			return this;
		}

		/**
		 * Builds a new {@link PeriodData} instance.
		 *
		 * @return a constructed {@link PeriodData}
		 */
		public PeriodData build() {
			return new PeriodData(this);
		}
	}

	/**
	 * Prediction for a {@link GlobalOptimizationContext.Period}.
	 *
	 * @param actual       the actual prediction for the period in [Wh].
	 * @param riskAdjusted the risk-adjusted prediction for the period in [Wh].
	 */
	public record Prediction(//
			int actual, //
			int riskAdjusted) {
	}

	/**
	 * Price information for a {@link GlobalOptimizationContext.Period}.
	 * 
	 * @param actual          the actual (Average) Price for the Period in [1/MWh].
	 * @param normalized      the normalized Price for the Period in range [0,1]
	 *                        (inclusive).
	 * @param positiveShifted the price after shifting all prices by a constant
	 *                        offset so that the lowest value is slightly above
	 *                        zero.
	 */
	public record Price(//
			double actual, //
			double normalized, //
			double positiveShifted) {
	}
}
