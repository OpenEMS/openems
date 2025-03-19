package io.openems.edge.ess.api;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public final class ActivePowerConstraintWithPid implements ThrowingBiConsumer<ManagedSymmetricEss, Integer, OpenemsNamedException> {

	@Override
	public void accept(ManagedSymmetricEss ess, Integer value) throws OpenemsNamedException {
		if (value != null) {
			var power = ess.getPower();
			var pidFilter = power.getPidFilter();

			// configure PID filter
			var minPower = power.getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
			var maxPower = power.getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
			if (maxPower < minPower) {
				maxPower = minPower; // avoid rounding error
			}

			int currentActivePower = ess.getActivePower().orElse(0);
			
			if (value <= 0 && currentActivePower <= 0 && minPower < 0 && maxPower > 0) {
				// Prevent PID filter to overshoot and flicker from charging to discharging
				pidFilter.setLimits(minPower, 0);
			} else if (value >= 0 && currentActivePower >= 0 && minPower < 0 && maxPower > 0) {
				pidFilter.setLimits(0, maxPower);
			} else {
				// changing between charging/discharging is intended behavior, so we allow it.
				pidFilter.setLimits(minPower, maxPower);
			}
			
			var pidOutput = pidFilter.applyPidFilter(currentActivePower, value);

			ess.setActivePowerEquals(pidOutput);
		}
	}
}