package io.openems.edge.sonnen.ess;

import io.openems.edge.sonnen.core.OperatingMode;

/**
 * Handles the Active-Power Set-Point for the sonnenBatterie.
 *
 * <p>
 * Depending on the {@link ControlMode}, the Set-Point is applied via the
 * 'Manual' operating mode with charge/discharge Set-Points (like the GoodWe
 * and SolarEdge implementations) or left to the internal energy management of
 * the sonnenBatterie.
 */
public class ApplyPowerHandler {

	/**
	 * The desired state of the sonnenBatterie: the {@link OperatingMode} and
	 * the charge/discharge Set-Points in [W] (zero or positive;
	 * {@code null} = not relevant in this mode).
	 *
	 * @param operatingMode    the desired {@link OperatingMode}
	 * @param chargeSetpointW   the charge Set-Point in [W]; null if not relevant
	 * @param dischargeSetpointW the discharge Set-Point in [W]; null if not
	 *                           relevant
	 */
	public record Result(OperatingMode operatingMode, Integer chargeSetpointW, Integer dischargeSetpointW) {
	}

	/**
	 * Calculates the desired state from the Active-Power Set-Point.
	 *
	 * @param setActivePower     the Active-Power Set-Point; negative for
	 *                           charging, positive for discharging
	 * @param controlMode        the {@link ControlMode}
	 * @param gridActivePower    the grid active power (positive = purchase
	 *                           from grid)
	 * @param essActivePower     the ESS active power (positive = discharging)
	 * @param defaultOperatingMode the {@link OperatingMode} to fall back to in
	 *                           {@link ControlMode#INTERNAL} mode
	 * @param maxChargePower    the maximum allowed charge power in [W]
	 * @param maxDischargePower the maximum allowed discharge power in [W]
	 * @return the {@link Result}
	 */
	protected static Result calculate(int setActivePower, ControlMode controlMode, Integer gridActivePower,
			Integer essActivePower, OperatingMode defaultOperatingMode, int maxChargePower, int maxDischargePower) {
		return switch (controlMode) {
		case INTERNAL -> handleInternalMode(defaultOperatingMode);

		case SMART -> {
			if (gridActivePower != null && essActivePower != null) {
				// Is the sonnenBatterie (in its internal operating mode) already able to
				// keep the grid exchange at the set-point?
				var diffBalancing = setActivePower - (gridActivePower + essActivePower);
				if (diffBalancing > -1 && diffBalancing < 1) {
					yield handleInternalMode(defaultOperatingMode);
				}
			}
			yield handleRemoteMode(setActivePower, maxChargePower, maxDischargePower);
		}

		case REMOTE -> handleRemoteMode(setActivePower, maxChargePower, maxDischargePower);
		};
	}

	private static Result handleInternalMode(OperatingMode defaultOperatingMode) {
		return new Result(defaultOperatingMode, null, null);
	}

	private static Result handleRemoteMode(int setActivePower, int maxChargePower, int maxDischargePower) {
		if (setActivePower < 0) {
			// charge
			var chargePower = Math.min(-setActivePower, maxChargePower);
			return new Result(OperatingMode.MANUAL, chargePower, 0);
		}
		// discharge or standby
		var dischargePower = Math.min(setActivePower, maxDischargePower);
		return new Result(OperatingMode.MANUAL, 0, dischargePower);
	}
}
