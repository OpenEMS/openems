package io.openems.edge.controller.ess.timeofusetariff;

import static com.google.common.math.Quantiles.percentiles;
import static io.openems.common.utils.DateUtils.durationUntilNextQuarter;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.IntUtils.fitWithin;
import static io.openems.common.utils.IntUtils.maxInt;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.AVOID_GRID_SELL_LIMIT;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.LIMIT_CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.PEAK_SHAVING;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.google.common.primitives.ImmutableIntArray;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.OptimizationContext;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.periods.PeriodData.Price;
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

	static ApplyMode calculateApplyMode(//
			Sum sum, ManagedSymmetricEss ess, Integer gridBuySoftLimit, int gridSellHardLimit,
			DifferentModes.Period<StateMachine, OptimizationContext> period, StateMachine forceMode, Clock clock) {
		final Integer gridActivePower = sum.getGridActivePower().get();
		final Integer essActivePower = ess.getActivePower().get();
		final Integer productionActivePower = sum.getProductionActivePower().get();

		if (gridActivePower == null || essActivePower == null || productionActivePower == null || period == null) {
			return new ApplyMode(BALANCING, null);
		}

		final var mode = forceMode != null ? forceMode : period.mode();
		final var optimizationContext = period.coc();

		final int balancingSetpoint = gridActivePower + essActivePower;

		final var applyMode = switch (mode) {
		case BALANCING -> new ApplyMode(BALANCING, balancingSetpoint);
		case DELAY_DISCHARGE -> calculateDelayDischarge(ess, essActivePower, balancingSetpoint);
		case CHARGE_GRID, PEAK_SHAVING -> calculateChargeGrid(ess, essActivePower, gridActivePower, balancingSetpoint,
				gridBuySoftLimit, optimizationContext);
		case DISCHARGE_GRID -> calculateDischargeGrid(essActivePower, gridActivePower);
		case DELAY_CHARGE -> calculateLimitCharge(ess, essActivePower, 0, balancingSetpoint);
		case LIMIT_CHARGE -> {
			final var limitChargePower = calculateLimitChargePower(ess, optimizationContext, clock);
			yield calculateLimitCharge(ess, essActivePower, limitChargePower, balancingSetpoint);
		}
		case DISCHARGE_CONSUMPTION, AVOID_GRID_SELL_LIMIT ->
			calculateDischargeConsumption(balancingSetpoint, productionActivePower);
		};

		return postProcessApplyMode(applyMode, balancingSetpoint, gridBuySoftLimit, gridSellHardLimit);
	}

	static ApplyMode calculateChargeGrid(ManagedSymmetricEss ess, int essActivePower, int gridActivePower,
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
			var power = maxInt(targetChargePower, peakShavingPower);
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

	static ApplyMode calculateDischargeGrid(int essActivePower, int gridActivePower) {
		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		var essDischargeInDischargeGrid = ESS_DISCHARGE_TO_GRID_POWER; // [W]
		var targetDischargePower = essDischargeInDischargeGrid + max(0, realGridPower);
		// TODO limit grid-sell power
		// var effectiveGridSellPower = min(0, realGridPower) + targetDischargePower;
		// var chargePower = max(0, targetChargePower - max(0, effectiveGridBuyPower -
		// maxChargePowerFromGrid));

		return new ApplyMode(DISCHARGE_GRID, targetDischargePower);
	}

	static ApplyMode calculateDelayDischarge(//
			ManagedSymmetricEss ess, int essActivePower, int balancingSetpoint) {
		final int delayDischargeSetpoint = switch (ess) {
		case HybridEss e -> max(0, essActivePower - e.getDcDischargePower().orElse(0));
		default -> 0;
		};

		if (delayDischargeSetpoint >= balancingSetpoint) {
			// But actually charging
			return new ApplyMode(BALANCING, balancingSetpoint);
		}

		return new ApplyMode(DELAY_DISCHARGE, delayDischargeSetpoint);
	}

	static ApplyMode calculateLimitCharge(//
			ManagedSymmetricEss ess, int essActivePower, Integer limitChargePower, int balancingSetpoint) {
		if (limitChargePower == null) {
			// No limit charge power is set
			return new ApplyMode(BALANCING, balancingSetpoint);
		}

		final int limitChargeSetpoint = switch (ess) {
		case HybridEss e -> max(0, essActivePower - e.getDcDischargePower().orElse(0)) - limitChargePower;
		default -> -limitChargePower;
		};

		if (limitChargeSetpoint <= balancingSetpoint) {
			// But actually charging less or discharging
			return new ApplyMode(BALANCING, balancingSetpoint);
		}

		if (limitChargePower == 0) {
			// Delay charging since the target limit charge power is zero
			return new ApplyMode(DELAY_CHARGE, limitChargeSetpoint);
		}

		return new ApplyMode(LIMIT_CHARGE, limitChargeSetpoint);
	}

	static ApplyMode calculateDischargeConsumption(//
			int balancingSetpoint, int productionActivePower) {
		final int dischargeConsumptionSetpoint = balancingSetpoint + productionActivePower;
		return new ApplyMode(DISCHARGE_CONSUMPTION, dischargeConsumptionSetpoint);
	}

	static ApplyMode postProcessApplyMode(//
			ApplyMode applyMode, int balancingSetpoint, Integer gridBuySoftLimit, int gridSellHardLimit) {
		final int setPoint = applyMode.setPoint();

		if (gridBuySoftLimit != null) {
			final int minSetpoint = balancingSetpoint - gridBuySoftLimit;
			if (setPoint < minSetpoint) {
				return new ApplyMode(PEAK_SHAVING, minSetpoint);
			}
		}

		final int maxSetpoint = balancingSetpoint + gridSellHardLimit;
		if (setPoint > maxSetpoint) {
			return new ApplyMode(AVOID_GRID_SELL_LIMIT, maxSetpoint);
		}

		return applyMode;
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
	static int calculateChargePowerInChargeGrid(GlobalOptimizationContext goc, int maxEnergyInChargeGrid) {
		var refs = ImmutableIntArray.builder();

		// Uses the total available energy as reference (= fallback)
		add(refs, maxEnergyInChargeGrid);

		// Uses the total excess consumption as reference
		add(refs, goc.periods().stream() //
				// calculates excess Consumption Power per Period
				.mapToInt(p -> p.data().consumption() //
						.map(c -> p.duration() //
								.convertEnergyToPower(c.actual() - p.data().production())) //
						.orElse(0)) //
				.sum());

		add(refs, goc.periods().stream() //
				.map(p -> p.data().consumption() //
						// Calculate excess consumption
						.map(c -> p.duration().convertEnergyToPower(c.actual() - p.data().production()))) //
				.takeWhile(opt -> opt.map(v -> v >= 0).orElse(false)) // Take only first periods
				.mapToInt(opt -> opt.orElse(0)) //
				.sum());

		// Uses the excess consumption during high grid-buy price periods as reference
		{
			var gridBuyPrices = goc.periods().stream() //
					.map(p -> p.data().gridBuyPrice()) //
					.flatMap(Optional::stream) //
					.mapToDouble(Price::actual) //
					.toArray();
			var peakIndex = findFirstPeakIndex(findFirstValleyIndex(0, gridBuyPrices), gridBuyPrices);
			var firstPrices = stream(gridBuyPrices) //
					.limit(peakIndex) //
					.toArray();
			if (firstPrices.length > 0) {
				var percentilePrice = percentiles().index(95).compute(firstPrices);
				add(refs, goc.periods().stream() //
						.filter(p -> p.data().gridBuyPrice().isPresent() && p.data().consumption().isPresent()) //
						.limit(peakIndex) //
						// Take only prices > percentile
						.filter(p -> p.data().gridBuyPrice().get().actual() >= percentilePrice)
						// Excess consumption power
						.mapToInt(p -> p.duration()
								.convertEnergyToPower(p.data().consumption().get().actual() - p.data().production())) //
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

	private static Integer calculateLimitChargePower(//
			ManagedSymmetricEss ess, OptimizationContext coc, Clock clock) {
		final var now = ZonedDateTime.now(clock);
		final int numFutureQuartersWithSurplus = Optional.ofNullable(//
				coc.futureQuarterlyPeriodsWithSurplusByQuarter()//
						.get(roundDownToQuarter(now).toLocalDateTime()))
				.orElse(0);
		if (numFutureQuartersWithSurplus <= 0) {
			return null;
		}

		final var soc = ess.getSoc().get();
		final var capacity = ess.getCapacity().get();
		if (soc == null || capacity == null) {
			return null;
		}

		final double socPerc = soc / 100.0;
		if (socPerc >= 1.0) {
			return null;
		}

		return GridOptimizedChargeUtils.calculateLimitChargePower(//
				numFutureQuartersWithSurplus, socPerc, capacity, durationUntilNextQuarter(clock));
	}

	static int calculateMaxSocForEnvironment(Environment environment) {
		return switch (environment) {
		case PRODUCTION, BETA, TEST -> 99;
		};
	}

	static int calculateMinSocForEnvironment(Environment environment) {
		return switch (environment) {
		case PRODUCTION, BETA, TEST -> 1;
		};
	}

	public record ApplyMode(StateMachine actualMode, Integer setPoint) {
	}
}
