package io.openems.edge.energy.api.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record Fitness(//
		int hardConstraintViolations, //
		List<AppliedCustomConstraint> constraintsAfterHardConstraintViolations, //
		double gridBuyCostScore, //
		double gridBuyEnergyWh, //
		List<AppliedCustomConstraint> constraintsAfterGridBuyConstraints, //
		double gridSellRevenueScore, //
		double gridSellEnergyWh, //
		double modePreferencePenalty, //
		int softConstraintViolations//
) implements Comparable<Fitness> {

	public static final Comparator<List<AppliedCustomConstraint>> CUSTOM_CONSTRAINT_LIST_COMPARATOR = //
			(left, right) -> {
				if (left.size() != right.size()) {
					throw new IllegalArgumentException("Cannot compare Fitness with different custom constraints");
				}

				for (int i = 0; i < left.size(); i++) {
					final var leftConstraint = left.get(i);
					final var rightConstraint = right.get(i);

					if (!leftConstraint.constraint().equals(rightConstraint.constraint())) {
						throw new IllegalArgumentException(
								"Cannot compare Fitness with different custom constraint definitions or order");
					}

					final int result = leftConstraint.compareTo(rightConstraint);
					if (result != 0) {
						return result;
					}
				}

				return 0;
			};

	public static final Comparator<Fitness> DEFAULT_COMPARATOR = Comparator//
			.comparingInt(Fitness::hardConstraintViolations)//
			.thenComparing(Fitness::constraintsAfterHardConstraintViolations, CUSTOM_CONSTRAINT_LIST_COMPARATOR)//
			.thenComparingDouble(Fitness::gridBuyCostScore)//
			.thenComparingDouble(Fitness::gridBuyEnergyWh)//
			.thenComparing(Fitness::constraintsAfterGridBuyConstraints, CUSTOM_CONSTRAINT_LIST_COMPARATOR)//
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

	public record AppliedCustomConstraint(CustomConstraint constraint, double value)
			implements Comparable<AppliedCustomConstraint> {

		public AppliedCustomConstraint {
			Objects.requireNonNull(constraint);
		}

		/**
		 * Compares this constraint value to another, respecting the
		 * {@link CustomConstraint.Direction} defined.
		 *
		 * @param o the other {@link AppliedCustomConstraint} to compare to; must have
		 *          the same {@link CustomConstraint} definition
		 * @return a negative integer, zero, or a positive integer if this value is
		 *         better than, equal to, or worse than the other value
		 */
		@Override
		public int compareTo(AppliedCustomConstraint o) {
			if (!this.constraint.equals(o.constraint)) {
				throw new IllegalArgumentException("Cannot compare different custom constraints");
			}

			return switch (this.constraint.direction()) {
			case LOWER_IS_BETTER -> Comparator.comparingDouble(AppliedCustomConstraint::value).compare(this, o);
			case HIGHER_IS_BETTER ->
				Comparator.comparingDouble(AppliedCustomConstraint::value).reversed().compare(this, o);
			};
		}

		/**
		 * Returns a new {@link AppliedCustomConstraint} with the given value added to
		 * the current value.
		 *
		 * @param additionalValue the value to add
		 * @return a new {@link AppliedCustomConstraint} with the accumulated value
		 */
		public AppliedCustomConstraint add(double additionalValue) {
			return new AppliedCustomConstraint(this.constraint, this.value + additionalValue);
		}
	}

	public record CustomConstraint(//
			String name, //
			Position position, //
			Direction direction) {

		public CustomConstraint {
			Objects.requireNonNull(name);
			Objects.requireNonNull(position);
			Objects.requireNonNull(direction);
		}

		public enum Position {
			AFTER_HARD_CONSTRAINT_VIOLATIONS, //
			AFTER_GRID_BUY_COST_SCORE,
		}

		public enum Direction {
			LOWER_IS_BETTER, //
			HIGHER_IS_BETTER,
		}
	}

	public static final class Builder {

		private final List<AppliedCustomConstraint> constraintsAfterHardConstraintViolations = new ArrayList<>();
		private final List<AppliedCustomConstraint> constraintsAfterGridBuyConstraints = new ArrayList<>();

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

		/**
		 * Adds a value to the given custom constraint, accumulating if called multiple
		 * times for the same constraint.
		 *
		 * @param constraint the constraint definition
		 * @param value      the value to add
		 * @return this
		 */
		public Builder addCustomConstraint(CustomConstraint constraint, double value) {
			Objects.requireNonNull(constraint);

			final var targetList = switch (constraint.position()) {
			case AFTER_HARD_CONSTRAINT_VIOLATIONS -> this.constraintsAfterHardConstraintViolations;
			case AFTER_GRID_BUY_COST_SCORE -> this.constraintsAfterGridBuyConstraints;
			};

			for (int i = 0; i < targetList.size(); i++) {
				if (targetList.get(i).constraint().equals(constraint)) {
					targetList.set(i, targetList.get(i).add(value));
					return this;
				}
			}

			targetList.add(new AppliedCustomConstraint(constraint, value));
			return this;
		}

		public Fitness build() {
			return new Fitness(//
					this.hardConstraintViolations, //
					List.copyOf(this.constraintsAfterHardConstraintViolations), //
					this.gridBuyCostScore, //
					this.gridBuyEnergyWh, //
					List.copyOf(this.constraintsAfterGridBuyConstraints), //
					this.gridSellRevenueScore, //
					this.gridSellEnergyWh, //
					this.modePreferencePenalty, //
					this.softConstraintViolations);
		}
	}
}