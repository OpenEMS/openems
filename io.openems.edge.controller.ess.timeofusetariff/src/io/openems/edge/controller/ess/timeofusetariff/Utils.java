package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.common.type.TypeUtils.max;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.common.type.TypeUtils.subtract;
import static io.openems.edge.common.type.TypeUtils.sum;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController.PERIODS_PER_HOUR;

import java.util.Arrays;
import java.util.Objects;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public final class Utils {

	private Utils() {
	}

	/**
	 * Calculates the ActivePower constraint for CHARGE_FROM_GRID state.
	 * 
	 * @param chargeDischargeEnergy  the scheduled charge/discharge energy of this
	 *                               period
	 * @param ess                    the {@link ManagedSymmetricEss}
	 * @param sum                    the {@link Sum}
	 * @param maxChargePowerFromGrid the configured max charge from grid power
	 * @return the set-point or null
	 */
	protected static Integer calculateChargeFromGridPower(int chargeDischargeEnergy, ManagedSymmetricEss ess, Sum sum,
			int maxChargePowerFromGrid) {
		// Calculate 'real' grid-power (without current ESS charge/discharge)
		var gridPower = sum(//
				sum.getGridActivePower().get(), /* current buy-from/sell-to grid */
				ess.getActivePower().get() /* current charge/discharge Ess */);

		return max(//
				chargeDischargeEnergy * PERIODS_PER_HOUR, // planned for this period
				subtract(gridPower, maxChargePowerFromGrid) // apply maxChargePowerFromGrid
		);
	}

	/**
	 * Calculates the ActivePower constraint for DELAY_DISCHARGE state.
	 * 
	 * @param chargeDischargeEnergy the scheduled charge/discharge energy of this
	 *                              period
	 * @param ess                   the {@link ManagedSymmetricEss}
	 * @return the set-point or null
	 */
	protected static Integer calculateDelayDischargePower(int chargeDischargeEnergy, ManagedSymmetricEss ess) {
		return chargeDischargeEnergy * PERIODS_PER_HOUR + calculateDcProduction(ess);
	}

	/**
	 * Gets the DC-PV production.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @return the actual DC production (never negative; zero if not available)
	 */
	// TODO consider moving this feature to ManagedSymmetricEss as it is used often
	// throughout OpenEMS
	private static int calculateDcProduction(ManagedSymmetricEss ess) {
		if (ess instanceof HybridEss e) {
			// DC or Hybrid system: limit AC export power to DC production power
			return max(orElse(//
					subtract(e.getActivePower().get(), e.getDcDischargePower().get()), //
					0), 0);
		} else {
			return 0;
		}
	}

	/**
	 * Interpolate an Array of {@link Float}s.
	 * 
	 * <p>
	 * Replaces nulls with previous value. If first entry is null, it is set to
	 * first available value. If all values are null, all are set to 0.
	 * 
	 * @param values the values
	 * @return values without nulls
	 */
	protected static float[] interpolateArray(Float[] values) {
		var firstNonNull = Arrays.stream(values) //
				.filter(Objects::nonNull) //
				.findFirst();
		var result = new float[values.length];
		if (firstNonNull.isEmpty()) {
			// all null
			return result;
		}
		float last = firstNonNull.get();
		for (var i = 0; i < values.length; i++) {
			float value = orElse(values[i], last);
			result[i] = last = value;
		}
		return result;
	}

	/**
	 * Interpolate an Array of {@link Integer}s.
	 * 
	 * <p>
	 * Replaces nulls with previous value. If first entry is null, it is set to
	 * first available value. If all values are null, all are set to 0.
	 * 
	 * @param values the values
	 * @return values without nulls
	 */
	protected static int[] interpolateArray(Integer[] values) {
		var firstNonNull = Arrays.stream(values) //
				.filter(Objects::nonNull) //
				.findFirst();
		var result = new int[values.length];
		if (firstNonNull.isEmpty()) {
			// all null
			return result;
		}
		int last = firstNonNull.get();
		for (var i = 0; i < values.length; i++) {
			int value = orElse(values[i], last);
			result[i] = last = value;
		}
		return result;
	}

}
