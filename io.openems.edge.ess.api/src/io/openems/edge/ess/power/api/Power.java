package io.openems.edge.ess.power.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface Power {

	public static final Logger log = LoggerFactory.getLogger(Power.class);

	public static Constraint[] NO_CONSTRAINTS = {};

	/**
	 * Adds a Constraint.
	 *
	 * @param constraint the {@link Constraint}
	 * @return the {@link Constraint}
	 */
	public Constraint addConstraint(Constraint constraint);

	/**
	 * Adds a Constraint if the problem is still solvable afterwards.
	 *
	 * @param constraint the {@link Constraint}
	 * @return the {@link Constraint}
	 * @throws PowerException   on error
	 * @throws OpenemsException on error
	 */
	public Constraint addConstraintAndValidate(Constraint constraint) throws OpenemsException;

	/**
	 * Creates a simple constraint.
	 *
	 * @param description  a description (for debug)
	 * @param ess          the {@link ManagedSymmetricEss}
	 * @param phase        the {@link Phase}
	 * @param pwr          the {@link Pwr}
	 * @param relationship the {@link Relationship}
	 * @param value        the value
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			Relationship relationship, double value) throws OpenemsException;

	/**
	 * Removes a Constraint.
	 *
	 * @param constraint the {@link Constraint}
	 */
	public void removeConstraint(Constraint constraint);

	/**
	 * Gets the maximum possible Power under the active Constraints.
	 * 
	 * @param ess   the {@link ManagedSymmetricEss}
	 * @param phase the {@link Phase}
	 * @param pwr   the {@link Pwr}
	 * @return the maximum possible power
	 */
	public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Gets the minimum possible possible Power under the active Constraints.
	 * 
	 * @param ess   the {@link ManagedSymmetricEss}
	 * @param phase the {@link Phase}
	 * @param pwr   the {@link Pwr}
	 * @return the minimum possible power
	 */
	public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Gets the Coefficient singleton for a specific combination of ess, phase and
	 * pwr.
	 *
	 * @param ess   the {@link ManagedSymmetricEss}
	 * @param phase the {@link Phase}
	 * @param pwr   the {@link Pwr}
	 * @return the {@link Coefficient}
	 * @throws OpenemsException on error
	 */
	public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) throws OpenemsException;

	/**
	 * Adjusts the given value so that it fits into Min/MaxPower.
	 *
	 * @param componentId Component-ID of the calling component for the log message
	 * @param ess         the {@link ManagedSymmetricEss}
	 * @param phase       the {@link Phase}
	 * @param pwr         the {@link Pwr}
	 * @param value       the target value
	 * @return a value that fits into Min/MaxPower
	 */
	public default int fitValueIntoMinMaxPower(String componentId, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			int value) {
		// fit into max possible discharge power
		value = this.fitValueToMaxPower(componentId, ess, phase, pwr, value);

		return this.fitValueToMinPower(componentId, ess, phase, pwr, value);
	}

	/**
	 * Adjusts the given value so that it does not break an existing
	 * Max-Power-Constraint.
	 *
	 * @param componentId Component-ID of the calling component for the log message
	 * @param ess         the {@link ManagedSymmetricEss}
	 * @param phase       the {@link Phase}
	 * @param pwr         the {@link Pwr}
	 * @param value       the target value
	 * @return a value that is compared to existing Max-Power
	 */
	public default int fitValueToMaxPower(String componentId, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			int value) {
		var maxPower = this.getMaxPower(ess, phase, pwr);
		if (value > maxPower) {
			Power.log.info("[" + componentId + "] reducing requested [" + value + " W] to maximum power [" + maxPower
					+ " W] for [" + ess.id() + pwr.getSymbol() + phase.getSymbol() + "]");
			return maxPower;
		}
		return value;
	}

	/**
	 * Adjusts the given value so that it does not break an existing
	 * Min-Power-Constraint.
	 *
	 * @param componentId Component-ID of the calling component for the log message
	 * @param ess         the {@link ManagedSymmetricEss}
	 * @param phase       the {@link Phase}
	 * @param pwr         the {@link Pwr}
	 * @param value       the target value
	 * @return a value that is compared to existing Min-Power
	 */
	public default int fitValueToMinPower(String componentId, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			int value) {
		var minPower = this.getMinPower(ess, phase, pwr);
		if (value < minPower) {
			Power.log.info("[" + componentId + "] increasing requested [" + value + " W] to minimum power [" + minPower
					+ " W] for [" + ess.id() + pwr.getSymbol() + phase.getSymbol() + "]");
			return minPower;
		}
		return value;
	}

	/**
	 * Gets the PidFilter instance with the configured P, I and D variables.
	 *
	 * @return an instance of {@link PidFilter}
	 */
	public PidFilter getPidFilter();

	/**
	 * Check if PidFilter is enabled.
	 *
	 * @return true if PidFilter is enable, otherwise false
	 */
	public boolean isPidEnabled();
}
