package io.openems.edge.controller.ess.timeofusetariff;

import static com.google.common.math.Quantiles.percentiles;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.energy.api.EnergyConstants.PERIODS_PER_HOUR;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static io.openems.edge.energy.api.EnergyUtils.toPower;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;

import com.google.common.primitives.ImmutableIntArray;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.EshContext;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Utils for {@link TimeOfUseTariffController}.
 * 
 * <p>
 * All energy values are in [Wh] and positive, unless stated differently.
 */
public final class Utils {

	private Utils() {
	}

	/** Keep some buffer to avoid scheduling errors because of bad predictions. */
	public static final float ESS_MAX_SOC = 90F;

	/** Limit Charge Power for §14a EnWG. */
	public static final int ESS_LIMIT_14A_ENWG = -4200;

	/**
	 * C-Rate (capacity divided by time) during {@link StateMachine#CHARGE_GRID}.
	 * With a C-Rate of 0.5 the battery gets fully charged within 2 hours.
	 */
	public static final float ESS_CHARGE_C_RATE = 0.5F;

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static final ChannelAddress SUM_GRID = new ChannelAddress("_sum", "GridActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	public static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	public static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	public static record ApplyState(StateMachine actualState, Integer setPoint) {
	}

	/**
	 * Calculate Automatic Mode.
	 * 
	 * @param sum                        the {@link Sum}
	 * @param ess                        the {@link ManagedSymmetricEss}
	 * @param maxChargePowerFromGrid     the configured max charge from grid power
	 * @param limitChargePowerFor14aEnWG Limit Charge Power for §14a EnWG
	 * @param period                     the scheduled {@link Period}
	 * @return {@link ApplyState}
	 */
	public static ApplyState calculateAutomaticMode(Sum sum, ManagedSymmetricEss ess, int maxChargePowerFromGrid,
			boolean limitChargePowerFor14aEnWG, Period<StateMachine, EshContext> period) {
		final StateMachine actualState;
		final Integer setPoint;

		var gridActivePower = sum.getGridActivePower().get(); // current buy-from/sell-to grid
		var essActivePower = ess.getActivePower().get(); // current charge/discharge ESS
		if (period == null || gridActivePower == null || essActivePower == null) {
			// undefined state
			return new ApplyState(BALANCING, null);
		}

		// Post-process and get actual state
		final var pwrBalancing = gridActivePower + essActivePower;
		final var pwrDelayDischarge = calculateDelayDischargePower(ess);
		final var pwrChargeGrid = calculateChargeGridPower(period.context().essChargeInChargeGrid(), ess,
				essActivePower, gridActivePower, maxChargePowerFromGrid, limitChargePowerFor14aEnWG);
		actualState = postprocessRunState(period.state(), pwrBalancing, pwrDelayDischarge, pwrChargeGrid);

		// Get and apply ActivePower Less-or-Equals Set-Point
		setPoint = switch (actualState) {
		case BALANCING -> null; // delegate to next priority Controller
		case DELAY_DISCHARGE -> pwrDelayDischarge;
		case CHARGE_GRID -> pwrChargeGrid;
		};

		return new ApplyState(actualState, setPoint);
	}

	/**
	 * Post-Process a state during {@link Controller#run()}, i.e. replace with
	 * 'better' state if appropriate.
	 * 
	 * <p>
	 * NOTE: this can be useful, if live operation deviates from predicted
	 * operation, e.g. because predictions were wrong.
	 * 
	 * @param state             the initial state
	 * @param pwrBalancing      the power set-point as it would be in
	 *                          {@link StateMachine#BALANCING}
	 * @param pwrDelayDischarge the power set-point as it would be in
	 *                          {@link StateMachine#DELAY_DISCHARGE}
	 * @param pwrChargeGrid     the power set-point as it would be in
	 *                          {@link StateMachine#CHARGE_GRID}
	 * @return the new state
	 */
	public static StateMachine postprocessRunState(StateMachine state, int pwrBalancing, int pwrDelayDischarge,
			int pwrChargeGrid) {
		if (state == CHARGE_GRID) {
			// CHARGE_GRID,...
			if (pwrChargeGrid >= pwrDelayDischarge) {
				// but battery charge/discharge is the same as DELAY_DISCHARGE
				state = DELAY_DISCHARGE;
			}
		}

		if (state == DELAY_DISCHARGE) {
			// CHARGE_GRID,...
			if (pwrDelayDischarge >= pwrBalancing) {
				// but battery charge/discharge is the same as DELAY_DISCHARGE
				state = BALANCING;
			}
		}

		return state;
	}

	/**
	 * Post-Process a state of a Period during Simulation, i.e. replace with
	 * 'better' state with the same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param state the initial state
	 * @param ef    the {@link EnergyFlow} for the state
	 * @return the new state
	 */
	public static StateMachine postprocessSimulatorState(StateMachine state, EnergyFlow ef) {
		if (state == CHARGE_GRID) {
			// CHARGE_GRID,...
			if (ef.getGridToEss() == 0) {
				// but battery is not charged from grid
				state = DELAY_DISCHARGE;
			}
		}

		if (state == DELAY_DISCHARGE) {
			// DELAY_DISCHARGE,...
			if (ef.getEss() < 0) {
				// but battery gets charged
				state = BALANCING;
			}
		}

		return state;
	}

	protected static int calculateEssChargeInChargeGridPower(Integer essChargeInChargeGrid, ManagedSymmetricEss ess) {
		if (essChargeInChargeGrid != null) {
			return toPower(essChargeInChargeGrid);
		}
		var capacity = ess.getCapacity();
		if (capacity.isDefined()) {
			return round(capacity.get() * ESS_CHARGE_C_RATE);
		}
		var maxApparentPower = ess.getMaxApparentPower();
		if (maxApparentPower.isDefined()) {
			return maxApparentPower.get() / 4;
		}
		return 0;
	}

	/**
	 * Calculates the Max-ActivePower constraint for
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param essChargeInChargeGrid      ESS Charge Energy in CHARGE_GRID State [Wh]
	 * @param ess                        the {@link ManagedSymmetricEss}
	 * @param essActivePower             the ESS ActivePower
	 * @param gridActivePower            the Grid ActivePower
	 * @param maxChargePowerFromGrid     the configured max charge from grid power
	 * @param limitChargePowerFor14aEnWG Limit Charge Power for §14a EnWG
	 * @return the set-point or null
	 */
	public static int calculateChargeGridPower(Integer essChargeInChargeGrid, ManagedSymmetricEss ess,
			int essActivePower, int gridActivePower, int maxChargePowerFromGrid, boolean limitChargePowerFor14aEnWG) {
		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		var targetChargePower = calculateEssChargeInChargeGridPower(essChargeInChargeGrid, ess) //
				+ min(0, realGridPower) * -1; // add excess production
		var effectiveGridBuyPower = max(0, realGridPower) + targetChargePower;
		var chargePower = max(0, targetChargePower - max(0, effectiveGridBuyPower - maxChargePowerFromGrid));

		// Invert to negative for CHARGE
		chargePower *= -1;

		// Apply §14a EnWG limit
		if (limitChargePowerFor14aEnWG) {
			chargePower = max(ESS_LIMIT_14A_ENWG, chargePower);
		}

		return chargePower;
	}

	/**
	 * Calculates the Max-ActivePower constraint for
	 * {@link StateMachine#CHARGE_PRODUCTION}.
	 * 
	 * @param sum the {@link Sum}
	 * @return the set-point
	 */
	public static Integer calculateMaxChargeProductionPower(Sum sum) {
		var productionAcActivePower = sum.getProductionAcActivePower().get();
		if (productionAcActivePower == null || productionAcActivePower < 0) {
			return 0; // unknown AC production -> do not charge
		}
		return -productionAcActivePower;
	}

	/**
	 * Calculates the ActivePower constraint for
	 * {@link StateMachine#DELAY_DISCHARGE}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @return the set-point
	 */
	public static int calculateDelayDischargePower(ManagedSymmetricEss ess) {
		if (ess instanceof HybridEss e) {
			// Limit discharge to DC-PV power
			return max(0, ess.getActivePower().orElse(0) - e.getDcDischargePower().orElse(0));
		} else {
			// Limit discharge to 0
			return 0;
		}
	}

	/**
	 * Calculates the default ESS charge energy per period in
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * <p>
	 * Applies {@link #ESS_CHARGE_C_RATE} with the minimum of usable ESS energy or
	 * predicted consumption energy that cannot be supplied from production.
	 * 
	 * @param gsc the {@link GlobalSimulationsContext}
	 * @return the value in [Wh]
	 */
	public static int calculateChargeEnergyInChargeGrid(GlobalSimulationsContext gsc) {
		var refs = ImmutableIntArray.builder();

		// Uses the total available energy as reference (= fallback)
		var fallback = max(0, round(ESS_MAX_SOC / 100F * gsc.ess().totalEnergy()));
		add(refs, fallback);

		// Uses the total excess consumption as reference
		add(refs, gsc.periods().stream() //
				.mapToInt(p -> p.consumption() - p.production()) // calculates excess Consumption Energy per Period
				.sum());

		add(refs, gsc.periods().stream() //
				.takeWhile(p -> p.consumption() >= p.production()) // take only first Periods
				.mapToInt(p -> p.consumption() - p.production()) // calculates excess Consumption Energy per Period
				.sum());

		// Uses the excess consumption during high price periods as reference
		{
			var prices = gsc.periods().stream() //
					.mapToDouble(GlobalSimulationsContext.Period::price) //
					.toArray();
			var peakIndex = findFirstPeakIndex(findFirstValleyIndex(0, prices), prices);
			var firstPrices = stream(prices) //
					.limit(peakIndex) //
					.toArray();
			if (firstPrices.length > 0) {
				var percentilePrice = percentiles().index(95).compute(firstPrices);
				add(refs, gsc.periods().stream() //
						.limit(peakIndex) //
						.filter(p -> p.price() >= percentilePrice) // takes only prices > percentile
						.mapToInt(p -> p.consumption() - p.production()) // excess Consumption Energy per Period
						.sum());
			}
		}

		return (int) round(//
				refs.build().stream() //
						.average() //
						.orElse(fallback) //
						* ESS_CHARGE_C_RATE / PERIODS_PER_HOUR);
	}

	private static void add(ImmutableIntArray.Builder builder, int value) {
		if (value > 0) {
			builder.add(value);
		}
	}

}
