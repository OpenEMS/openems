package io.openems.edge.controller.ess.stepsoc.enums;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public enum Direction {
	CHARGE, //
	DISCHARGE, //
	;

	public int getFactor() {
		return this == CHARGE ? -1 : 1;
	}

	/**
	 * Checks whether the ESS has reached its SoC limit for the current
	 * {@link Direction}, i.e. it can no longer be charged (CHARGE) or discharged
	 * (DISCHARGE) any further.
	 *
	 * @param ess the {@link ManagedSymmetricEss} to check
	 * @return true if the allowed power for this direction is defined and equal to
	 *         zero, i.e. the ESS is at its limit; false if the limit is not yet
	 *         reached or the value is undefined
	 */
	public boolean isFinished(ManagedSymmetricEss ess) {
		final var allowedPower = switch (this) {
		case CHARGE -> ess.getAllowedChargePower();
		case DISCHARGE -> ess.getAllowedDischargePower();
		};
		return allowedPower.isDefined() && allowedPower.get() == 0;
	}
}