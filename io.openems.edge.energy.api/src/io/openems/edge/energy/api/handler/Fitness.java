package io.openems.edge.energy.api.handler;

import static com.google.common.base.MoreObjects.toStringHelper;

public class Fitness implements Comparable<Fitness> {

	private int hardConstraintViolations = 0;
	private double gridBuyCost = 0.;
	private double gridSellRevenue = 0.;
	private double modePreferencePenalty = 0.;
	private int softConstraintViolations = 0;

	/**
	 * Gets the number of Hard-Constraint-Violations.
	 * 
	 * @return Hard-Constraint-Violations
	 */
	public int getHardConstraintViolations() {
		return this.hardConstraintViolations;
	}

	/**
	 * Add a Hard-Constraint-Violation with degree=1.
	 */
	public void addHardConstraintViolation() {
		this.hardConstraintViolations++;
	}

	/**
	 * Add a Hard-Constraint-Violation.
	 * 
	 * @param degree degree of violation
	 */
	public void addHardConstraintViolation(int degree) {
		this.hardConstraintViolations += degree;
	}

	/**
	 * Gets the Grid-Buy cost.
	 * 
	 * @return Grid-Buy cost
	 */
	public double getGridBuyCost() {
		return this.gridBuyCost;
	}

	/**
	 * Add Grid-Buy cost.
	 * 
	 * @param cost the cost
	 */
	public void addGridBuyCost(double cost) {
		this.gridBuyCost += cost;
	}

	/**
	 * Add Grid-Sell revenue.
	 * 
	 * @param revenue the revenue
	 */
	public void addGridSellRevenue(double revenue) {
		this.gridSellRevenue += revenue;
	}

	/**
	 * Gets the Mode-Preference penalty.
	 *
	 * @return Mode-Preference penalty
	 */
	public double getModePreferencePenalty() {
		return this.modePreferencePenalty;
	}

	/**
	 * Sets the Mode-Preference penalty.
	 *
	 * @param penalty the penalty
	 */
	public void setModePreferencePenalty(double penalty) {
		this.modePreferencePenalty = penalty;
	}

	/**
	 * Gets the number of Soft-Constraint-Violations.
	 * 
	 * @return Soft-Constraint-Violations
	 */
	public int getSoftConstraintViolations() {
		return this.softConstraintViolations;
	}

	/**
	 * Add a Soft-Constraint-Violation with degree=1.
	 */
	public void addSoftConstraintViolation() {
		this.softConstraintViolations++;
	}

	/**
	 * Add a Soft-Constraint-Violation.
	 * 
	 * @param degree degree of violation
	 */
	public void addSoftConstraintViolation(int degree) {
		this.softConstraintViolations += degree;
	}

	@Override
	public int compareTo(Fitness o) {
		// 1st priority: hard constraints (lower is better)
		if (this.hardConstraintViolations != o.hardConstraintViolations) {
			return Integer.compare(this.hardConstraintViolations, o.hardConstraintViolations);
		}

		// 2nd priority: grid buy cost (lower is better); ignore negative costs
		final var thisGridBuyCost = Math.max(0, this.gridBuyCost);
		final var otherGridBuyCost = Math.max(0, o.gridBuyCost);
		if (thisGridBuyCost != otherGridBuyCost) {
			return Double.compare(thisGridBuyCost, otherGridBuyCost);
		}

		// 3nd priority: grid sell revenue (higher is better); ignore negative revenue
		final var thisGridSellRevenue = Math.max(0, this.gridSellRevenue);
		final var otherGridSellRevenue = Math.max(0, o.gridSellRevenue);
		if (thisGridSellRevenue != otherGridSellRevenue) {
			return Double.compare(otherGridSellRevenue, thisGridSellRevenue);
		}

		// 4th priority: mode preference penalty (lower is better)
		if (this.modePreferencePenalty != o.modePreferencePenalty) {
			return Double.compare(this.modePreferencePenalty, o.modePreferencePenalty);
		}

		// 5th priority: soft constraints (lower is better)
		return Integer.compare(this.softConstraintViolations, o.softConstraintViolations);
	}

	@Override
	public String toString() {
		return toStringHelper(Fitness.class) //
				.add("hardConstraintViolations", this.hardConstraintViolations) //
				.add("gridBuyCost", this.gridBuyCost) //
				.add("gridSellRevenue", this.gridSellRevenue) //
				.add("modePreferencePenalty", this.modePreferencePenalty) //
				.add("softConstraintViolations", this.softConstraintViolations) //
				.toString();
	}
}