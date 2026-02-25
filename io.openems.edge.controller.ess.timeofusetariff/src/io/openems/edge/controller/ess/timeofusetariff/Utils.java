package io.openems.edge.controller.ess.timeofusetariff;

import static com.google.common.math.Quantiles.percentiles;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.PEAK_SHAVING;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;

import com.google.common.primitives.ImmutableIntArray;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.OptimizationContext;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Price;
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

	/**
	 * C-Rate (capacity divided by time) during {@link StateMachine#CHARGE_GRID}.
	 * With a C-Rate of 0.5 the battery gets fully charged within 2 hours.
	 */
	public static final float ESS_CHARGE_C_RATE = 0.5F;

	// TODO dynamic
	public static final int ESS_DISCHARGE_TO_GRID_POWER = 5000; // [W]

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static final ChannelAddress SUM_GRID = new ChannelAddress("_sum", "GridActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	public static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	public static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	public static record ApplyMode(StateMachine actualMode, Integer setPoint) {
	}

	/**
	 * Calculate Automatic Mode.
	 * 
	 * @param sum           the {@link Sum}
	 * @param ess           the {@link ManagedSymmetricEss}
	 * @param gridSoftLimit the configured Soft-Limit from Grid (or null)
	 * @param period        the scheduled {@link Period}
	 * @param forceMode     force a target {@link StateMachine}
	 * @return {@link ApplyMode}
	 */
	public static ApplyMode calculateAutomaticMode(Sum sum, ManagedSymmetricEss ess, Integer gridSoftLimit,
			DifferentModes.Period<StateMachine, OptimizationContext> period, StateMachine forceMode) {
		var gridActivePower = sum.getGridActivePower().get(); // current buy-from/sell-to grid
		var essActivePower = ess.getActivePower().get(); // current charge/discharge ESS
		if ((period == null && forceMode == null) || gridActivePower == null || essActivePower == null) {
			// undefined state
			return new ApplyMode(BALANCING, null);
		}

		final var mode = forceMode != null //
				? forceMode //
				: period.mode();
		final var pwrBalancing = gridActivePower + essActivePower;
		return switch (mode) {
		case BALANCING -> new ApplyMode(BALANCING, pwrBalancing);
		case DELAY_DISCHARGE ->
			calculateDelayDischarge(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit);
		case CHARGE_GRID, PEAK_SHAVING ->
			calculateChargeGrid(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit, period.coc());
		case DISCHARGE_GRID -> calculateDischargeGrid(ess, essActivePower, gridActivePower);
		};
	}

	/**
	 * Calculates the {@link ApplyMode} for {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param coc             the {@link OptimizationContext}
	 * @param ess             the {@link ManagedSymmetricEss}
	 * @param essActivePower  the ESS ActivePower
	 * @param gridActivePower the Grid ActivePower
	 * @param pwrBalancing    the set-point in {@link StateMachine#BALANCING}
	 * @param gridSoftLimit   the configured Soft-Limit from Grid (or null)
	 * @return the {@link ApplyMode}
	 */
	private static ApplyMode calculateChargeGrid(ManagedSymmetricEss ess, int essActivePower, int gridActivePower,
			int pwrBalancing, Integer gridSoftLimit, OptimizationContext coc) {
		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		var targetChargePower = -coc.essChargePowerInChargeGrid() //
				+ min(0, realGridPower); // add excess production
		var peakShavingPower = calculatePeakShavingPower(essActivePower, gridActivePower, gridSoftLimit);

		// CHARGE_GRID...
		if (peakShavingPower != null && peakShavingPower > 0) {
			// ...but discharging is required for peak-shaving to gridSoftLimit
			return new ApplyMode(PEAK_SHAVING, peakShavingPower);

		} else {
			// Charge from Grid; limited by gridSoftLimit
			var power = TypeUtils.max(targetChargePower, peakShavingPower);
			if (power == 0) {
				// ...but actually DELAY_DISCHARGE
				return new ApplyMode(DELAY_DISCHARGE, 0);
			}

			if (power == pwrBalancing) {
				// ...but actually same as BALANCING
				return new ApplyMode(BALANCING, power);
			}

			var soc = ess.getSoc();
			if (soc.isDefined() && soc.get() >= coc.maxSocInChargeGrid()) {
				// ...but charge-limit was reached
				return new ApplyMode(BALANCING, pwrBalancing);
			}
			return new ApplyMode(CHARGE_GRID, power);
		}
	}

	/**
	 * Calculates the {@link ApplyMode} for {@link StateMachine#DISCHARGE_GRID}.
	 * 
	 * @param ess             the {@link ManagedSymmetricEss}
	 * @param essActivePower  the ESS ActivePower
	 * @param gridActivePower the Grid ActivePower
	 * @return the {@link ApplyMode}
	 */
	private static ApplyMode calculateDischargeGrid(ManagedSymmetricEss ess, int essActivePower, int gridActivePower) {
		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		var essDischargeInDischargeGrid = ESS_DISCHARGE_TO_GRID_POWER; // [W]
		var targetDischargePower = essDischargeInDischargeGrid + max(0, realGridPower);
		// TODO limit grid-sell power
		// var effectiveGridSellPower = min(0, realGridPower) + targetDischargePower;
		// var chargePower = max(0, targetChargePower - max(0, effectiveGridBuyPower -
		// maxChargePowerFromGrid));

		return new ApplyMode(DISCHARGE_GRID, targetDischargePower);
	}

	/**
	 * Calculates the {@link ApplyMode} for {@link StateMachine#DELAY_DISCHARGE}.
	 * 
	 * <p>
	 * This mode stops discharging the battery, but allows charging. (i.e.
	 * ESS::DcDischargePower <= 0); unless peak-shaving to "gridSoftLimit" is
	 * required, then it also allows discharging.
	 * 
	 * @param ess             the {@link ManagedSymmetricEss}
	 * @param essActivePower  the ESS ActivePower
	 * @param gridActivePower the Grid ActivePower
	 * @param pwrBalancing    the set-point in {@link StateMachine#BALANCING}
	 * @param gridSoftLimit   the configured Soft-Limit from Grid (or null)
	 * @return the {@link ApplyMode}
	 */
	private static ApplyMode calculateDelayDischarge(ManagedSymmetricEss ess, int essActivePower, int gridActivePower,
			int pwrBalancing, Integer gridSoftLimit) {
		var pwrDelayDischarge = switch (ess) {
		case HybridEss e ->
			// Limit discharge to DC-PV power
			max(0, essActivePower - e.getDcDischargePower().orElse(0));
		default ->
			// Limit discharge to 0
			0;
		};

		// DELAY_DISCHARGE...
		var peakShavingPower = calculatePeakShavingPower(essActivePower, gridActivePower, gridSoftLimit);
		if (peakShavingPower != null && peakShavingPower > 0) {
			// ...but discharging is required for peak-shaving to gridSoftLimit
			return new ApplyMode(PEAK_SHAVING, peakShavingPower);

		} else if (pwrDelayDischarge >= pwrBalancing) {
			// ...but actually charging
			return new ApplyMode(BALANCING, pwrBalancing);

		} else {
			return new ApplyMode(DELAY_DISCHARGE, pwrDelayDischarge);
		}
	}

	/**
	 * Calculates the default ESS charge power per period in
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * <p>
	 * Applies {@link #ESS_CHARGE_C_RATE} with the minimum of usable ESS energy or
	 * predicted consumption energy that cannot be supplied from production.
	 * 
	 * @param goc                   the {@link GlobalOptimizationContext}
	 * @param maxEnergyInChargeGrid the Max-Energy in [Wh] in CHARGE_GRID
	 * @return the value in [W]
	 */
	public static int calculateChargePowerInChargeGrid(GlobalOptimizationContext goc, int maxEnergyInChargeGrid) {
		var refs = ImmutableIntArray.builder();

		// Uses the total available energy as reference (= fallback)
		add(refs, maxEnergyInChargeGrid);

		// Uses the total excess consumption as reference
		add(refs, goc.streamPeriodsWithPrediction() //
				// calculates excess Consumption Power per Period
				.mapToInt(p -> p.duration().convertEnergyToPower(p.prediction().excessConsumption())) //
				.sum());

		add(refs, goc.streamPeriodsWithPrediction() //
				.takeWhile(p -> p.prediction().excessConsumption() >= 0) // take only first Periods
				// calculates excess Consumption Power per Period
				.mapToInt(p -> p.duration().convertEnergyToPower(p.prediction().excessConsumption())) //
				.sum());

		// Uses the excess consumption during high price periods as reference
		{
			var ps = goc.streamCompletePeriods() //
					.toList();
			var prices = ps.stream() //
					.map(Period.Complete::price) //
					.mapToDouble(Price::actual) //
					.toArray();
			var peakIndex = findFirstPeakIndex(findFirstValleyIndex(0, prices), prices);
			var firstPrices = stream(prices) //
					.limit(peakIndex) //
					.toArray();
			if (firstPrices.length > 0) {
				var percentilePrice = percentiles().index(95).compute(firstPrices);
				add(refs, ps.stream() //
						.limit(peakIndex) //
						.filter(p -> p.price().actual() >= percentilePrice) // takes only prices > percentile
						// excess Consumption Power per Period
						.mapToInt(p -> p.duration().convertEnergyToPower(p.prediction().excessConsumption())) //
						.sum());
			}
		}

		return (int) round(//
				refs.build().stream() //
						// filter negative and very large values
						.map(v -> fitWithin(0, maxEnergyInChargeGrid, v)) //
						.average() //
						.orElse(maxEnergyInChargeGrid) //
						* ESS_CHARGE_C_RATE);
	}

	private static Integer calculatePeakShavingPower(int essActivePower, int gridActivePower, Integer gridSoftLimit) {
		if (gridSoftLimit == null) {
			return null;
		}
		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		return realGridPower - gridSoftLimit;
	}

	private static void add(ImmutableIntArray.Builder builder, int value) {
		if (value > 0) {
			builder.add(value);
		}
	}

	protected static int calculateMaxSocForEnvironment(Environment environment) {
		return switch (environment) {
			case PRODUCTION, BETA, TEST -> 99;
		};
	}

	protected static int calculateMinSocForEnvironment(Environment environment) {
		return switch (environment) {
			case PRODUCTION, BETA, TEST -> 1;
		};
	}
}
