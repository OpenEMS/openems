package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class SellToGridLimit {

	/**
	 * Last sellToGridLimit used in the power ramp.
	 */
	private int lastSellToGridLimit = 0;

	/**
	 * Reference to parent controller.
	 */
	private final ControllerEssGridOptimizedChargeImpl parent;

	public SellToGridLimit(ControllerEssGridOptimizedChargeImpl parent) {
		this.parent = parent;
	}

	/**
	 * Set active power limits depending on the maximum sell to grid power.
	 *
	 * @return result
	 * @throws OpenemsNamedException on error
	 */
	protected Integer getSellToGridLimit() throws OpenemsNamedException {

		if (!this.parent.config.sellToGridLimitEnabled()) {
			this.setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.DISABLED, null);
			return null;
		}

		// Current buy-from/sell-to grid
		int gridPower = this.parent.meter.getActivePower().getOrError();

		// Current ess charge/discharge power
		int essActivePower = this.parent.ess.getActivePower().getOrError();

		// State of charge
		int soc = this.parent.ess.getSoc().getOrError();

		var maximumSellToGridPower = this.parent.config.maximumSellToGridPower();

		// Reduce the maximum sell to grid power with a buffer, to avoid PV curtail
		// TODO improve logic to make sure we always meet the maximum sell to grid
		// power. Right now we simply try to target a value 5 % below the configured
		// maximum sell to grid power.
		if (soc < 100) {
			maximumSellToGridPower = Math.round(maximumSellToGridPower * 0.95F);
			this.parent.logDebug("Reduced SellToGridPowerLimit: " + maximumSellToGridPower);
		}

		// Calculate actual limit for Ess
		var essMinChargePower = gridPower + essActivePower + maximumSellToGridPower;

		// Log debug
		this.parent.logDebug("Maximum Discharge/Minimum Charge Power: " + essMinChargePower + "(Grid:" + gridPower
				+ " + Ess:" + essActivePower + " + MaximumGrid:" + maximumSellToGridPower + ")| Last limit: "
				+ this.lastSellToGridLimit);

		return this.applyPowerRamp(essMinChargePower);
	}

	protected void applyCalculatedMinimumChargePower(int sellToGridLimit) {

		// Current DelayCharge state
		var state = SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT;

		try {
			// Set the power limitation constraint
			this.parent.ess.setActivePowerLessOrEquals(sellToGridLimit);
		} catch (OpenemsNamedException e) {
			state = SellToGridLimitState.NO_FEASABLE_SOLUTION;
		}

		// Set channels
		this.setSellToGridLimitChannelsAndLastLimit(state, sellToGridLimit);
	}

	/**
	 * Apply power ramp, to react in a smooth way.
	 *
	 * <p>
	 * Calculates a limit depending on the given power limit and the last power
	 * limit. Stronger limits are taken directly, while the last limit will only be
	 * reduced if the new limit is lower.
	 *
	 * @param essPowerLimit essPowerLimit
	 * @return adjusted ess power limit
	 * @throws OpenemsException on error
	 */
	private int applyPowerRamp(int essPowerLimit) throws OpenemsException {

		// Stronger Limit will be taken
		if (this.lastSellToGridLimit == 0 || essPowerLimit <= this.lastSellToGridLimit) {
			return essPowerLimit;
		}

		// Maximum power
		int maxEssPower = this.parent.ess.getMaxApparentPower().getOrError();

		// Reduce last SellToGridLimit by configured percentage
		var percentage = this.parent.config.sellToGridLimitRampPercentage() / 100.0;
		var rampValue = (int) (maxEssPower * percentage);

		// Use ramp only when the difference would be higher than the applied ramp
		if (Math.abs(this.lastSellToGridLimit - essPowerLimit) > rampValue) {
			essPowerLimit = this.lastSellToGridLimit + rampValue;
			// REMOVE
			this.parent.logDebug("Ramp added: " + rampValue);
		}
		return essPowerLimit;
	}

	/**
	 * Set Channels and lastLimit for SellToGridLimit part.
	 *
	 * @param state             SellToGridLimit state
	 * @param essMinChargePower SellToGridLimit absolute charge limit
	 */
	protected void setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState state, Integer essMinChargePower) {
		this.parent._setSellToGridLimitState(state);
		if (essMinChargePower == null) {
			this.parent._setSellToGridLimitMinimumChargeLimit(null);
			this.parent._setRawSellToGridLimitChargeLimit(null);
			this.lastSellToGridLimit = 0;
			return;
		}

		this.parent._setRawSellToGridLimitChargeLimit(essMinChargePower);
		this.lastSellToGridLimit = essMinChargePower;

		// Calculate & set readable AC format
		int dcProduction = this.parent.sum.getProductionDcActualPower().orElse(0);
		var essMinChargePowerAc = essMinChargePower - dcProduction;
		this.parent._setSellToGridLimitMinimumChargeLimit(essMinChargePowerAc * -1);
	}
}
