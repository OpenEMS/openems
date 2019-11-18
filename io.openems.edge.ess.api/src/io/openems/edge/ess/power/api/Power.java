package io.openems.edge.ess.power.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface Power {

	public final static Logger log = LoggerFactory.getLogger(Power.class);

	public static Constraint[] NO_CONSTRAINTS = new Constraint[] {};

	/**
	 * Adds a Constraint.
	 * 
	 * @param constraint
	 */
	public Constraint addConstraint(Constraint constraint);

	/**
	 * Adds a Constraint if the problem is still solvable afterwards.
	 * 
	 * @param constraint
	 * @throws PowerException
	 * @throws OpenemsException
	 */
	public Constraint addConstraintAndValidate(Constraint constraint) throws OpenemsException;

	/**
	 * Creates a simple constraint
	 * 
	 * @param description
	 * @param ess
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 * @throws OpenemsException
	 */
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			Relationship relationship, double value) throws OpenemsException;

	/**
	 * Removes a Constraint.
	 * 
	 * @param constraint
	 */
	public void removeConstraint(Constraint constraint);

	/**
	 * Gets the maximum possible Power under the active Constraints.
	 */
	public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Gets the minimum possible possible Power under the active Constraints.
	 */
	public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Gets the Coefficient singleton for a specific combination of ess, phase and
	 * pwr
	 * 
	 * @param ess
	 * @param phase
	 * @param pwr
	 * @return
	 * @throws OpenemsException
	 */
	Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) throws OpenemsException;

	/**
	 * Adjusts the given value so that it fits into Min/MaxPower.
	 * 
	 * @param componentId Component-ID of the calling component for the log message
	 * @param value       the target value
	 * @return a value that fits into Min/MaxPower
	 */
	public default int fitValueIntoMinMaxPower(String componentId, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			int value) {
		/*
		 * Discharge
		 */
		// fit into max possible discharge power
		int maxPower = this.getMaxPower(ess, phase, pwr);
		if (value > maxPower) {
			Power.log.info("[" + componentId + "] reducing requested [" + value + " W] to maximum power [" + maxPower
					+ " W] for [" + ess.id() + pwr.getSymbol() + phase.getSymbol() + "]");
			value = maxPower;
		}

		/*
		 * Charge
		 */
		// fit into max possible charge power
		int minPower = this.getMinPower(ess, phase, pwr);
		if (value < minPower) {
			Power.log.info("[" + componentId + "] increasing requested [" + value + " W] to minimum power [" + minPower
					+ " W] for [" + ess.id() + pwr.getSymbol() + phase.getSymbol() + "]");
			value = minPower;
		}
		return value;
	}

	/**
	 * Builds a PidFilter instance with the configured P, I and D variables. If no
	 * configuration is found, it falls back to default PidFilter values.
	 * 
	 * @return an instance of {@link PidFilter}
	 */
	public PidFilter buildPidFilter();
}
