package io.openems.edge.ess.api;

import java.util.List;

import io.openems.edge.ess.power.api.Constraint;

public class ApplyPowerContext {

	private final List<Constraint> constraints;

	/**
	 * Creates an {@link ApplyPowerContext}.
	 * 
	 * @param constraints the {@link Constraint}s for this specific ESS.
	 */
	public ApplyPowerContext(List<Constraint> constraints) {
		this.constraints = constraints;
	}

	/**
	 * Gets the {@link Constraint}s for this specific ESS.
	 * 
	 * @return a list of Constraints
	 */
	public List<Constraint> getConstraints() {
		return constraints;
	}
}
