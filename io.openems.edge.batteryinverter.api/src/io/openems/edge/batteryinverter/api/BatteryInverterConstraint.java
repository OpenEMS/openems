package io.openems.edge.batteryinverter.api;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * Wraps a Constraint for a {@link ManagedSymmetricBatteryInverter}.
 */
public record BatteryInverterConstraint(String description, SingleOrAllPhase phase, Pwr pwr, Relationship relationship,
		int value) {

	public static final BatteryInverterConstraint[] NO_CONSTRAINTS = {};
}