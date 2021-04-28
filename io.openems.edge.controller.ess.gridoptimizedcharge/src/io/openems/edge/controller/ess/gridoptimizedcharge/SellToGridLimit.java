package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class SellToGridLimit {

	// Last sellToGridLimit used in the power ramp
	private int lastSellToGridLimit = 0;

	// Reference to parent controller
	private GridOptimizedChargeImpl parent;

	public SellToGridLimit(GridOptimizedChargeImpl parent) {
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
			this.setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.UNDEFINED, null);
			return null;
		}

		// Current buy-from/sell-to grid
		int gridPower = this.parent.meter.getActivePower().getOrError();

		// Current ess charge/discharge power
		int essActivePower = this.parent.getIntValueOrSetStateAndException(this.parent.ess.getActivePower(),
				GridOptimizedCharge.ChannelId.ESS_HAS_NO_ACTIVE_POWER);

		// Calculate actual limit for Ess
		int essMinChargePower = gridPower + essActivePower + this.parent.config.maximumSellToGridPower();

		// Adjust value so that it fits into Min/MaxActivePower
		essMinChargePower = this.parent.ess.getPower().fitValueIntoMinMaxPower(this.parent.id(), this.parent.ess,
				Phase.ALL, Pwr.ACTIVE, essMinChargePower);

		// Adjust ramp
		essMinChargePower = applyPowerRamp(essMinChargePower);

		// Avoid max discharge constraint
		if (essMinChargePower > 0) {
			setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.NO_LIMIT, 0);
			return null;
		}

		return essMinChargePower;
	}

	protected void applyCalculatedLimit(int sellToGridLimit) {
		// Set the power limitation constraint
		boolean constraintSet = this.parent.setActivePowerConstraint(sellToGridLimit, Relationship.LESS_OR_EQUALS);

		// Current DelayCharge state
		SellToGridLimitState state = constraintSet ? SellToGridLimitState.ACTIVE_LIMIT
				: SellToGridLimitState.NO_FEASABLE_SOLUTION;

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
	 * @throws InvalidValueException on error
	 */
	private int applyPowerRamp(int essPowerLimit) throws InvalidValueException {

		// Stronger Limit will be taken
		if (this.lastSellToGridLimit == 0 || essPowerLimit <= this.lastSellToGridLimit) {
			return essPowerLimit;
		}

		// Maximum power
		int maxEssPower = this.parent.ess.getMaxApparentPower().orElse(this.parent.config.maximumSellToGridPower());

		// Reduce last SellToGridLimit by configured percentage
		double percentage = (this.parent.config.sellToGridLimitRampPercentage() / 100.0);
		int rampValue = (int) (maxEssPower * percentage);
		essPowerLimit = lastSellToGridLimit + rampValue;
		return essPowerLimit;
	}

	/**
	 * Set Channels and lastLimit for SellToGridLimit part.
	 * 
	 * @param state                 SellToGridLimitState
	 * @param sellToGridChargeLimit sellToGridLimit absolute charge limit
	 * @param essPowerLimit         ess power limit
	 */
	protected void setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState state, Integer essMinChargePower) {
		this.parent._setSellToGridLimitState(state);
		if (essMinChargePower == null) {
			this.parent._setSellToGridLimitMinimumChargeLimit(null);
			this.lastSellToGridLimit = 0;
			return;
		}
		this.parent._setSellToGridLimitMinimumChargeLimit(essMinChargePower * -1);
		this.lastSellToGridLimit = essMinChargePower;
	}
}
