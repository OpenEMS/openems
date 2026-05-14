package io.openems.edge.energy.api.handler;

import java.util.Comparator;

public record Fitness(//
		int hardConstraintViolations, //
		double gridBuyCostScore, //
		double gridBuyEnergyWh, //
		double gridSellRevenueScore, //
		double gridSellEnergyWh, //
		double modePreferencePenalty, //
		int softConstraintViolations//
) implements Comparable<Fitness> {

	public static final Comparator<Fitness> DEFAULT_COMPARATOR = Comparator//
			.comparingInt(Fitness::hardConstraintViolations)//
			.thenComparingDouble(Fitness::gridBuyCostScore)//
			.thenComparingDouble(Fitness::gridBuyEnergyWh)//
			.thenComparing(Comparator.comparingDouble(Fitness::gridSellRevenueScore).reversed())// higher is better
			.thenComparing(Comparator.comparingDouble(Fitness::gridSellEnergyWh).reversed())// higher is better
			.thenComparingDouble(Fitness::modePreferencePenalty)//
			.thenComparingInt(Fitness::softConstraintViolations);

	/**
	 * {@inheritDoc}
	 *
	 * @implNote A {@link Fitness} instance is considered <i>better</i> than another
	 *           one if this method returns a value smaller than {@code 0}. In other
	 *           words: {@code a.compareTo(b) < 0} means that {@code a} is preferred
	 *           over {@code b} according to {@link #DEFAULT_COMPARATOR}.
	 */
	@Override
	public int compareTo(Fitness o) {
		return DEFAULT_COMPARATOR.compare(this, o);
	}

	/**
	 * Creates a new builder instance for incrementally constructing a
	 * {@link Fitness}.
	 *
	 * @return a new {@link Builder} used to accumulate fitness components
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private int hardConstraintViolations;
		private double gridBuyCostScore;
		private double gridBuyEnergyWh;
		private double gridSellRevenueScore;
		private double gridSellEnergyWh;
		private double modePreferencePenalty;
		private int softConstraintViolations;

		private Builder() {
		}

		/**
		 * Increments the number of hard constraint violations by 1.
		 *
		 * @return this
		 */
		public Builder addHardConstraintViolation() {
			this.hardConstraintViolations++;
			return this;
		}

		/**
		 * Adds a hard constraint violation.
		 *
		 * @param degree degree of violation
		 * @return this
		 */
		public Builder addHardConstraintViolation(int degree) {
			this.hardConstraintViolations += degree;
			return this;
		}

		/**
		 * Adds to the total grid-buy cost score.
		 *
		 * @param score cost score to add
		 * @return this
		 */
		public Builder addGridBuyCostScore(double score) {
			this.gridBuyCostScore += score;
			return this;
		}

		/**
		 * Adds to the total grid-buy energy in Wh.
		 *
		 * @param amount amount to add
		 * @return this
		 */
		public Builder addGridBuyEnergyWh(double amount) {
			this.gridBuyEnergyWh += amount;
			return this;
		}

		/**
		 * Adds to the total grid-sell revenue score.
		 *
		 * @param score revenue score to add
		 * @return this
		 */
		public Builder addGridSellRevenueScore(double score) {
			this.gridSellRevenueScore += score;
			return this;
		}

		/**
		 * Adds to the total grid-sell energy in Wh.
		 *
		 * @param amount amount to add
		 * @return this
		 */
		public Builder addGridSellEnergyWh(double amount) {
			this.gridSellEnergyWh += amount;
			return this;
		}

		/**
		 * Sets the mode preference penalty.
		 *
		 * @param penalty penalty value
		 * @return this
		 */
		public Builder withModePreferencePenalty(double penalty) {
			this.modePreferencePenalty = penalty;
			return this;
		}

		/**
		 * Increments the number of soft constraint violations by 1.
		 *
		 * @return this
		 */
		public Builder addSoftConstraintViolation() {
			this.softConstraintViolations++;
			return this;
		}

		/**
		 * Adds a soft constraint violation.
		 *
		 * @param degree degree of violation
		 * @return this
		 */
		public Builder addSoftConstraintViolation(int degree) {
			this.softConstraintViolations += degree;
			return this;
		}

		public Fitness build() {
			return new Fitness(//
					this.hardConstraintViolations, //
					this.gridBuyCostScore, //
					this.gridBuyEnergyWh, //
					this.gridSellRevenueScore, //
					this.gridSellEnergyWh, //
					this.modePreferencePenalty, //
					this.softConstraintViolations);
		}
	}
}