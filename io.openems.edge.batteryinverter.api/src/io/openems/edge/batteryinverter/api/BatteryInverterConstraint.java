package io.openems.edge.batteryinverter.api;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * Wraps a Constraint for a {@link ManagedSymmetricBatteryInverter}.
 */
public class BatteryInverterConstraint {

	public static final BatteryInverterConstraint[] NO_CONSTRAINTS = {};

	public final String description;
	public final SingleOrAllPhase phase;
	public final Pwr pwr;
	public final Relationship relationship;
	public final double value;

	public BatteryInverterConstraint(String description, SingleOrAllPhase phase, Pwr pwr, Relationship relationship,
			double value) {
		this.description = description;
		this.phase = phase;
		this.pwr = pwr;
		this.relationship = relationship;
		this.value = value;
	}
}