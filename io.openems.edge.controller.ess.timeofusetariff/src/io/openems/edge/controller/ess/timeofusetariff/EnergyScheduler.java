package io.openems.edge.controller.ess.timeofusetariff;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.precomputeLimitChargeWithFixedTargetTime;
import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.precomputeLimitChargeWithSocBuffer;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.AVOID_GRID_SELL_LIMIT;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.LIMIT_CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.PEAK_SHAVING;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_DISCHARGE_TO_GRID_POWER;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargePowerInChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateMaxSocForEnvironment;
import static io.openems.edge.energy.api.EnergyUtils.energyToSoc;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static io.openems.edge.energy.api.EnergyUtils.findValleyIndexes;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.SingleModes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.SingleModes.SingleMode;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.energy.api.simulation.periods.PeriodData.Price;

public class EnergyScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(EnergyScheduler.class);

	/**
	 * Minimum power factor is applied to the maximum allowed charge power of the
	 * ess, to avoid very low charge power (Default 8.10% equals minimum 4A on every
	 * System).
	 */
	public static final float MINIMUM_POWER_FACTOR = 0.081F;

	private static final Fitness.CustomConstraint CONSTRAINT_TARGET_TIME_ENERGY_MISSING = new Fitness.CustomConstraint(//
			"targetTimeEnergyMissing", //
			Fitness.CustomConstraint.Position.AFTER_GRID_BUY_COST_SCORE, //
			Fitness.CustomConstraint.Direction.LOWER_IS_BETTER);

	public record OptimizationContext(//
			int maxSocInChargeGrid, int maxEnergyInChargeGrid, int essChargePowerInChargeGrid,
			Map<LocalDateTime, Integer> futureQuarterlyPeriodsWithSurplusByQuarter,
			Map<LocalDate, LocalTime> targetTimeByDay) {

		protected static OptimizationContext from(//
				GlobalOptimizationContext goc, Double targetSocBuffer, LocalTime manualTargetTime) {
			// TODO: calculateMaxSocForEnvironment() is prepared for DISCHARGE_GRID
			final var maxSocInChargeGrid = calculateMaxSocForEnvironment(goc.environment());
			final var maxEnergyInChargeGrid = round(goc.ess().totalEnergy() * (maxSocInChargeGrid / 100F));
			final var essChargePowerInChargeGrid = calculateChargePowerInChargeGrid(goc, maxEnergyInChargeGrid);

			// Precompute grid optimized charge
			final Map<LocalDateTime, Integer> futureQuarterlyPeriodsWithSurplusByQuarter;
			final Map<LocalDate, LocalTime> targetTimeByDay;
			if (targetSocBuffer == null && manualTargetTime == null) {
				futureQuarterlyPeriodsWithSurplusByQuarter = Collections.emptyMap();
				targetTimeByDay = Collections.emptyMap();
			} else {
				final var precomputeLimitChargeResult = manualTargetTime != null
						? precomputeLimitChargeWithFixedTargetTime(goc, manualTargetTime)
						: precomputeLimitChargeWithSocBuffer(goc, targetSocBuffer);
				futureQuarterlyPeriodsWithSurplusByQuarter = precomputeLimitChargeResult.futureSurplusCounts();
				targetTimeByDay = precomputeLimitChargeResult.targetTimeByDay();
			}

			// TODO calculateDoNotDischargeToGridAfterPeriod(goc, prices) is prepared for
			// DISCHARGE_GRID
			return new OptimizationContext(maxSocInChargeGrid, maxEnergyInChargeGrid, essChargePowerInChargeGrid,
					futureQuarterlyPeriodsWithSurplusByQuarter, targetTimeByDay);
		}
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 *
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 *
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<StateMachine, OptimizationContext, Void> buildEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<StateMachine, OptimizationContext, Void>create(parent) //
				.setSerializer(Config.serializer(), configSupplier) //

				.setModes(() -> {
					var config = configSupplier.get();
					return new SingleModes<>(//
							new Modes.Channels(//
									new ChannelAddress(
											parent.id(), TimeOfUseTariffController.ChannelId.STATE_MACHINE.id()),
									null /* no ManagedConsumptionPower channel */), //
							Arrays.stream(StateMachine.values()) //
									.map(m -> new SingleMode<>(//
											m, //
											config != null && config.activeModes().contains(m), //
											preferenceRankFor(m)))//
									.collect(toImmutableList()));
				})

				.setInitialPopulationsProvider((goc, coc, modes) -> {
					// Prepare Initial Population with cheapest grid-buy price per valley set to
					// DELAY_DISCHARGE or CHARGE_GRID
					final var result = ImmutableSortedSet.<InitialPopulation<StateMachine>>naturalOrder();
					final var gridBuyPrices = goc.periods().stream()//
							.map(p -> p.data().gridBuyPrice())//
							.flatMap(Optional::stream)//
							.mapToDouble(Price::actual)//
							.toArray();

					generateInitialPopulation(result, goc, gridBuyPrices, DELAY_DISCHARGE);
					var hasChargeGrid = modes.streamForOptimizer().anyMatch(m -> m == CHARGE_GRID);
					if (hasChargeGrid) {
						generateInitialPopulation(result, goc, gridBuyPrices, CHARGE_GRID);
					}
					var hasDischargeGrid = modes.streamForOptimizer().anyMatch(m -> m == DISCHARGE_GRID);
					if (hasDischargeGrid) {
						generateInitialPopulationForDischargeToGrid(result, goc, gridBuyPrices);
					}
					return result.build();
				})

				.setOptimizationContext(goc -> {
					final var config = configSupplier.get();
					final var targetSocBuffer = config != null ? config.targetSocBuffer() : null;
					final var manualTargetTime = config != null ? config.manualTargetTime() : null;
					return OptimizationContext.from(goc, targetSocBuffer, manualTargetTime);
				})

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					if (mode == null) {
						mode = BALANCING;
					}

					if (period.gridBuySoftLimit() != null) {
						// Try setting GridMaxBuy to GridBuySoftLimit
						ef.setGridMaxBuy(period.gridBuySoftLimit());
					}

					if (isFinalRun) {
						// This is the final run -> post-process mode
						mode = postProcessMode(period, gsc, coc, ef, mode);
					}

					// Disallow mode CHARGE_GRID when ess is full
					final int soc = energyToSoc(gsc.ess.getInitialEnergy(), gsc.goc.ess().totalEnergy());
					if (mode == CHARGE_GRID && soc >= coc.maxSocInChargeGrid()) {
						fitness.addHardConstraintViolation();
					}

					simulateMode(period, gsc, coc, ef, mode);
					return mode;
				})

				.setEvaluator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					var targetTime = coc.targetTimeByDay().get(period.time().toLocalDate());
					if (targetTime == null) {
						return;
					}

					targetTime = switch (period.duration()) {
					case QUARTER -> targetTime;
					case HOUR -> {
						final boolean isFullHour = targetTime.getMinute() == 0 && targetTime.getSecond() == 0
								&& targetTime.getNano() == 0;
						yield isFullHour //
								? targetTime //
								: targetTime.truncatedTo(ChronoUnit.HOURS).plusHours(1);
					}
					};

					if (!period.time().toLocalTime().equals(targetTime)) {
						return;
					}

					final var today = period.time().toLocalDate();
					final boolean areTherePeriodsForTomorrow = gsc.goc.periods().stream()
							.anyMatch(p -> p.time().toLocalDate().isAfter(today));
					if (areTherePeriodsForTomorrow) {
						return;
					}

					final int capacity = gsc.goc.ess().totalEnergy();
					final int currEnergy = gsc.ess.getInitialEnergy() - ef.getEss();

					final int targetTimeEnergyMissing = capacity - currEnergy;
					fitness.addCustomConstraint(CONSTRAINT_TARGET_TIME_ENERGY_MISSING, targetTimeEnergyMissing);
				})

				.build();
	}

	private static Integer preferenceRankFor(StateMachine m) {
		return switch (m) {
		case LIMIT_CHARGE -> 1;
		case BALANCING -> 2;
		case DELAY_DISCHARGE, DELAY_CHARGE -> 3;
		case DISCHARGE_CONSUMPTION -> 4;
		case CHARGE_GRID, DISCHARGE_GRID -> 5;
		default -> 6;
		};
	}

	private static void simulateMode(Period period, GlobalScheduleContext gsc, OptimizationContext coc,
			EnergyFlow.Model ef, StateMachine mode) {
		switch (mode) {
		case BALANCING -> applyBalancing(ef);
		case DELAY_DISCHARGE -> applyDelayDischarge(ef);
		case CHARGE_GRID, PEAK_SHAVING -> {
			var chargeEnergy = min(//
					period.duration().convertPowerToEnergy(coc.essChargePowerInChargeGrid),
					coc.maxEnergyInChargeGrid - gsc.ess.getInitialEnergy());
			applyChargeGrid(ef, chargeEnergy);
		}
		case DISCHARGE_GRID -> {
			var dischargeEnergy = period.duration().convertPowerToEnergy(ESS_DISCHARGE_TO_GRID_POWER);
			applyDischargeGrid(ef, dischargeEnergy);
		}
		case DELAY_CHARGE -> applyDelayCharge(ef);
		case LIMIT_CHARGE -> applyLimitCharge(ef, calculateLimitChargeEnergy(period, gsc, coc));
		case DISCHARGE_CONSUMPTION, AVOID_GRID_SELL_LIMIT -> applyDischargeConsumption(ef);
		}
	}

	/**
	 * Simulate {@link EnergyFlow} in a future {@link StateMachine#BALANCING} state.
	 *
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyBalancing(EnergyFlow.Model model) {
		final int target = -model.getSurplus();
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future {@link StateMachine#DELAY_DISCHARGE}
	 * state.
	 *
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyDelayDischarge(EnergyFlow.Model model) {
		final int target = min(0, -model.getSurplus());
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future {@link StateMachine#CHARGE_GRID}
	 * state.
	 *
	 * @param model        the {@link EnergyFlow.Model}
	 * @param chargeEnergy the target charge-from-grid energy
	 */
	public static void applyChargeGrid(EnergyFlow.Model model, int chargeEnergy) {
		final int target = min(0, -model.getSurplus()) - chargeEnergy;
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future {@link StateMachine#DISCHARGE_GRID}
	 * state.
	 *
	 * @param model           the {@link EnergyFlow.Model}
	 * @param dischargeEnergy the target discharge-to-grid energy
	 */
	public static void applyDischargeGrid(EnergyFlow.Model model, int dischargeEnergy) {
		final int target = max(0, -model.getSurplus()) + dischargeEnergy;
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future {@link StateMachine#DELAY_CHARGE} or
	 * {@link StateMachine#AVOID_GRID_SELL_LIMIT} state.
	 *
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyDelayCharge(EnergyFlow.Model model) {
		final int target = -min(0, model.getSurplus());
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future {@link StateMachine#LIMIT_CHARGE}
	 * state.
	 *
	 * @param model             the {@link EnergyFlow.Model}
	 * @param limitChargeEnergy the target limit charge energy, or null if no limit
	 */
	public static void applyLimitCharge(EnergyFlow.Model model, Integer limitChargeEnergy) {
		final int target = (limitChargeEnergy != null) //
				? -min(limitChargeEnergy, model.getSurplus()) //
				: -model.getSurplus();
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future
	 * {@link StateMachine#DISCHARGE_CONSUMPTION} state.
	 *
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyDischargeConsumption(EnergyFlow.Model model) {
		final int target = model.getConsumption();
		model.setEss(target);
	}

	private static Integer calculateLimitChargeEnergy(//
			Period period, GlobalScheduleContext gsc, OptimizationContext coc) {
		final int numFutureQuartersWithSurplus = Optional.ofNullable(//
				coc.futureQuarterlyPeriodsWithSurplusByQuarter()//
						.get(period.time().toLocalDateTime()))
				.orElse(0);
		if (numFutureQuartersWithSurplus <= 0) {
			return null;
		}

		final int capacity = gsc.goc.ess().totalEnergy();
		final int initialEnergy = gsc.ess.getInitialEnergy();

		return GridOptimizedChargeUtils.calculateLimitChargeEnergy(//
				numFutureQuartersWithSurplus, capacity, initialEnergy, period.duration());
	}

	private static void generateInitialPopulation(ImmutableSortedSet.Builder<InitialPopulation<StateMachine>> result,
			GlobalOptimizationContext goc, double[] prices, StateMachine mode) {
		Arrays.stream(findValleyIndexes(prices)) //
				.mapToObj(i -> goc.periods().stream() //
						.map(p -> p.index() == i //
								? mode // set mode
								: BALANCING) // default
						.toArray(StateMachine[]::new)) //
				.map(InitialPopulation::new) //
				.forEach(result::add);
	}

	private static void generateInitialPopulationForDischargeToGrid(
			ImmutableSortedSet.Builder<InitialPopulation<StateMachine>> result, GlobalOptimizationContext goc,
			double[] prices) {
		IntStream.of(findFirstPeakIndex(1, prices)) //
				.mapToObj(i -> goc.periods().stream() //
						.map(p -> p.index() == i //
								? DISCHARGE_GRID
								: BALANCING) // default
						.toArray(StateMachine[]::new)) //
				.map(InitialPopulation::new) //
				.forEach(result::add);
	}

	protected static int calculateDoNotDischargeToGridAfterPeriod(GlobalOptimizationContext goc, double[] prices) {
		return findFirstPeakIndex(findFirstValleyIndex(1, prices), prices);
	}

	private static StateMachine postProcessMode(Period period, GlobalScheduleContext gsc, OptimizationContext coc,
			EnergyFlow.Model ef, StateMachine mode) {
		final var balancing = simulateModeWithCopy(period, gsc, coc, ef, BALANCING);
		final var delayDischarge = simulateModeWithCopy(period, gsc, coc, ef, DELAY_DISCHARGE);

		var postProcessedMode = mode;

		if (mode == DISCHARGE_CONSUMPTION) {
			final var dischargeConsumption = simulateModeWithCopy(period, gsc, coc, ef, DISCHARGE_CONSUMPTION);
			final var delayCharge = simulateModeWithCopy(period, gsc, coc, ef, DELAY_CHARGE);
			if (dischargeConsumption.getEss() == balancing.getEss()) {
				postProcessedMode = BALANCING;
			} else if (dischargeConsumption.getEss() == delayCharge.getEss()) {
				postProcessedMode = DELAY_CHARGE;
			}
		}

		if (mode == LIMIT_CHARGE) {
			final var limitCharge = simulateModeWithCopy(period, gsc, coc, ef, LIMIT_CHARGE);
			final var delayCharge = simulateModeWithCopy(period, gsc, coc, ef, DELAY_CHARGE);
			final int maxSellEnergy = period.duration().convertPowerToEnergy(gsc.goc.grid().maxSellPowerWithBuffer());
			if (limitCharge.getEss() == balancing.getEss()) {
				postProcessedMode = BALANCING;
			} else if (-limitCharge.getGrid() >= maxSellEnergy) {
				postProcessedMode = AVOID_GRID_SELL_LIMIT;
			} else if (limitCharge.getEss() > 0) {
				postProcessedMode = PEAK_SHAVING;
			} else if (limitCharge.getEss() == delayCharge.getEss()) {
				postProcessedMode = DELAY_CHARGE;
			}
		}

		if (mode == DELAY_DISCHARGE) {
			if (delayDischarge.getEss() == balancing.getEss()) {
				postProcessedMode = BALANCING;
			} else if (delayDischarge.getEss() > 0) {
				postProcessedMode = PEAK_SHAVING;
			} else if (delayDischarge.getEss() < 0) {
				postProcessedMode = AVOID_GRID_SELL_LIMIT;
			}
		}

		if (mode == DELAY_CHARGE) {
			final var delayCharge = simulateModeWithCopy(period, gsc, coc, ef, DELAY_CHARGE);
			if (delayCharge.getEss() == balancing.getEss()) {
				postProcessedMode = BALANCING;
			} else if (delayCharge.getEss() > 0) {
				postProcessedMode = PEAK_SHAVING;
			} else if (delayCharge.getEss() < 0) {
				postProcessedMode = AVOID_GRID_SELL_LIMIT;
			}
		}

		if (mode == CHARGE_GRID) {
			final var chargeGrid = simulateModeWithCopy(period, gsc, coc, ef, CHARGE_GRID);
			if (chargeGrid.getEss() == balancing.getEss()) {
				postProcessedMode = BALANCING;
			} else if (chargeGrid.getEss() > balancing.getEss()) {
				postProcessedMode = PEAK_SHAVING;
			} else if (chargeGrid.getEss() == delayDischarge.getEss()) {
				postProcessedMode = DELAY_DISCHARGE;
			}
		}

		if (postProcessedMode != mode) {
			LOG.info("OPTIMIZER PostProcess {} from {} to {}", period.time(), mode, postProcessedMode);
		}

		return postProcessedMode;
	}

	private static EnergyFlow simulateModeWithCopy(Period period, GlobalScheduleContext gsc, OptimizationContext coc,
			EnergyFlow.Model ef, StateMachine mode) {
		var efCopy = EnergyFlow.Model.copyOf(ef);
		simulateMode(period, gsc, coc, efCopy, mode);
		return efCopy.solve();
	}

	// TODO maxChargePowerFromGrid is not used!
	public record Config(List<StateMachine> activeModes, Double targetSocBuffer, LocalTime manualTargetTime) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			return jsonObjectSerializer(Config.class, json -> {
				return new Config(//
						json.getList("activeModes", e -> e.getAsEnum(StateMachine.class)), //
						json.getOptionalDouble("targetSocBuffer").orElse(null), //
						json.getOptionalLocalTime("manualTargetTime").orElse(null));
			}, obj -> {
				return buildJsonObject() //
						.add("activeModes", obj.activeModes.stream()//
								.map(StateMachine::name)//
								.map(JsonPrimitive::new)//
								.collect(toJsonArray()))//
						.addPropertyIfNotNull("targetSocBuffer", obj.targetSocBuffer)//
						.addPropertyIfNotNull("manualTargetTime", obj.manualTargetTime)//
						.build();
			});
		}
	}
}