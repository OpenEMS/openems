package io.openems.edge.energy.api.handler;

import static com.google.common.base.MoreObjects.toStringHelper;

public class Fitness implements Comparable<Fitness> {

	private int hardConstraintViolations = 0;
	private double gridBuyCostScore = 0.;
	private double gridBuyEnergyWh = 0.;
	private double gridSellRevenueScore = 0.;
	private double gridSellEnergyWh = 0.;
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
	 * Gets the Grid-Buy cost score.
	 * 
	 * @return the Grid-Buy cost score
	 */
	public double getGridBuyCostScore() {
		return this.gridBuyCostScore;
	}

	/**
	 * Adds Grid-Buy cost score.
	 * 
	 * @param score the cost score to add
	 */
	public void addGridBuyCostScore(double score) {
		this.gridBuyCostScore += score;
	}

	/**
	 * Adds Grid-Buy energy in Wh.
	 *
	 * @param amount the energy amount to add
	 */
	public void addGridBuyEnergyWh(double amount) {
		this.gridBuyEnergyWh += amount;
	}

	/**
	 * Adds Grid-Sell revenue score.
	 * 
	 * @param score the revenue score to add
	 */
	public void addGridSellRevenueScore(double score) {
		this.gridSellRevenueScore += score;
	}

	/**
	 * Adds Grid-Sell energy in Wh.
	 *
	 * @param amount the energy amount to add
	 */
	public void addGridSellEnergyWh(double amount) {
		this.gridSellEnergyWh += amount;
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

		// 2nd priority: Grid-Buy metrics (lower is better)
		// 2.1 cost score
		if (this.gridBuyCostScore != o.gridBuyCostScore) {
			return Double.compare(this.gridBuyCostScore, o.gridBuyCostScore);
		}

		// 2.2 energy
		if (this.gridBuyEnergyWh != o.gridBuyEnergyWh) {
			return Double.compare(this.gridBuyEnergyWh, o.gridBuyEnergyWh);
		}

		// 3nd priority: Grid-Sell metrics (higher is better)
		// 3.1 revenue score
		if (this.gridSellRevenueScore != o.gridSellRevenueScore) {
			return Double.compare(o.gridSellRevenueScore, this.gridSellRevenueScore);
		}

		// 3.2 energy
		if (this.gridSellEnergyWh != o.gridSellEnergyWh) {
			return Double.compare(o.gridSellEnergyWh, this.gridSellEnergyWh);
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
				.add("gridBuyCostScore", this.gridBuyCostScore) //
				.add("gridBuyEnergyWh", this.gridBuyEnergyWh) //
				.add("gridSellRevenueScore", this.gridSellRevenueScore) //
				.add("gridSellEnergyWh", this.gridSellEnergyWh) //
				.add("modePreferencePenalty", this.modePreferencePenalty) //
				.add("softConstraintViolations", this.softConstraintViolations) //
				.toString();
	}
}