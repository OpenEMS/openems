package io.openems.edge.energy.api.simulation;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.IntUtils.maxInt;
import static io.openems.common.utils.IntUtils.minInt;
import static io.openems.edge.energy.api.EnergyConstants.SCHEDULE_PERIODS_ON_EMPTY;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.api.simulation.periods.PeriodDuration.QUARTER;
import static java.lang.Math.abs;
import static java.util.stream.Collectors.joining;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.periods.Periods;
import io.openems.edge.energy.api.simulation.periods.RawPeriod;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.timeofusetariff.api.TariffManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class GocBuilder {
	private final Logger log = LoggerFactory.getLogger(GocBuilder.class);
	private final LogVerbosity logVerbosity;

	private ComponentManager componentManager;
	private Meta meta;
	private Environment environment;
	private ImmutableList<EnergyScheduleHandler> eshs;
	private Sum sum;
	private PredictorManager predictorManager;
	private TariffManager tariffManager;

	protected GocBuilder(LogVerbosity logVerbosity) {
		this.logVerbosity = logVerbosity;
	}

	/**
	 * The {@link ComponentManager}.
	 *
	 * @param componentManager the {@link ComponentManager}
	 * @return myself
	 */
	public GocBuilder setComponentManager(ComponentManager componentManager) {
		this.componentManager = componentManager;
		return this;
	}

	/**
	 * The {@link Meta}.
	 *
	 * @param meta the {@link Meta}
	 * @return myself
	 */
	public GocBuilder setMeta(Meta meta) {
		this.meta = meta;
		return this;
	}

	/**
	 * The {@link Environment}.
	 *
	 * @param environment the {@link Environment}
	 * @return myself
	 */
	public GocBuilder setEnvironment(Environment environment) {
		this.environment = environment;
		return this;
	}

	/**
	 * The {@link EnergyScheduleHandler}s of the {@link EnergySchedulable}s.
	 *
	 * <p>
	 * The list is sorted by Scheduler.
	 *
	 * @param eshs the list of {@link EnergyScheduleHandler}s
	 * @return myself
	 */
	public GocBuilder setEnergyScheduleHandlers(ImmutableList<EnergyScheduleHandler> eshs) {
		this.eshs = eshs;
		return this;
	}

	/**
	 * The {@link Sum}.
	 *
	 * @param sum the {@link Sum}
	 * @return myself
	 */
	public GocBuilder setSum(Sum sum) {
		this.sum = sum;
		return this;
	}

	/**
	 * The {@link PredictorManager}.
	 *
	 * @param predictorManager the {@link PredictorManager}
	 * @return myself
	 */
	public GocBuilder setPredictorManager(PredictorManager predictorManager) {
		this.predictorManager = predictorManager;
		return this;
	}

	/**
	 * The {@link TariffManager}.
	 *
	 * @param tariffManager the {@link TariffManager}
	 * @return myself
	 */
	public GocBuilder setTariffManager(TariffManager tariffManager) {
		this.tariffManager = tariffManager;
		return this;
	}

	/**
	 * Builds the {@link GlobalOptimizationContext}.
	 *
	 * @return the {@link GlobalOptimizationContext} record
	 */
	public GlobalOptimizationContext build() {
		this.validate();

		final var consumptionValues = this.predictorManager.getPrediction(SUM_UNMANAGED_CONSUMPTION);
		this.logInfo("GlobalOptimizationContext CONSUMPTIONS: " + consumptionValues.toString());

		final var productionValues = this.predictorManager.getPrediction(SUM_PRODUCTION);
		this.logInfo("GlobalOptimizationContext PRODUCTIONS: " + productionValues.toString());

		final var gridBuyPrices = this.tariffManager.getGridBuyDayAheadPrices();
		this.logInfo("GlobalOptimizationContext GRID-BUY PRICES: " + gridBuyPrices.toString());

		final var gridSellPrices = this.tariffManager.getGridSellDayAheadPrices();
		this.logInfo("GlobalOptimizationContext GRID-SELL PRICES: " + gridSellPrices.toString());

		final var clock = this.componentManager.getClock();
		final var startTime = roundDownToQuarter(ZonedDateTime.now(clock));
		final var endTime = this.computeEndTime(startTime, consumptionValues, productionValues, gridBuyPrices,
				gridSellPrices);

		final var grid = this.buildGrid();
		final var gridBuySoftLimits = grid.gridBuySoftLimit()//
				.getOneTasksBetween(startTime, endTime.plusMinutes(15));

		final var periods = this.buildPeriods(//
				startTime, gridBuySoftLimits, productionValues, consumptionValues, gridBuyPrices, gridSellPrices);

		if (periods.isEmpty()) {
			throw new IllegalStateException("No forecast periods available");
		}

		final Integer essCapacity = this.sum.getEssCapacity().get();
		Objects.requireNonNull(essCapacity, "ESS capacity is not available");
		final Integer essSoc = this.sum.getEssSoc().get();
		Objects.requireNonNull(essSoc, "ESS SoC is not available");
		final var ess = this.buildEss(essCapacity, essSoc);

		final var eshsWithDifferentModes = filterEshsWithDifferentModes(this.eshs) //
				.collect(toImmutableList());

		this.logInfo("OPTIMIZER GlobalOptimizationContext: " //
				+ "startTime=" + startTime + "; " //
				+ "consumptions=" + consumptionValues.asArray().length + "; " //
				+ "productions=" + productionValues.asArray().length + "; " //
				+ "gridBuyPrices=" + gridBuyPrices.asArray().length + "; " //
				+ "gridSellPrices=" + gridSellPrices.asArray().length + "; " //
				+ "periods=" + periods.size() + "; " //
				+ "eshs=" + this.eshs.stream() //
						.map(EnergyScheduleHandler::getParentId) //
						.collect(joining(","))
				+ "; " //
				+ "eshsWithDifferentModes=" + eshsWithDifferentModes.stream() //
						.map(EnergyScheduleHandler::getParentId) //
						.collect(joining(",")));

		return new GlobalOptimizationContext(//
				clock, this.environment, startTime, this.eshs, eshsWithDifferentModes, grid, ess, periods);
	}

	private void validate() {
		Objects.requireNonNull(this.componentManager, "ComponentManager is not available");
		Objects.requireNonNull(this.meta, "Meta is not available");
		Objects.requireNonNull(this.eshs, "EnergyScheduleHandlers is not available");
		Objects.requireNonNull(this.sum, "Sum is not available");
		Objects.requireNonNull(this.predictorManager, "PredictorManager is not available");
		Objects.requireNonNull(this.tariffManager, "TariffManager is not available");
	}

	private ZonedDateTime computeEndTime(//
			ZonedDateTime startTime, //
			Prediction consumptionValues, //
			Prediction productionValues, //
			TimeOfUsePrices gridBuyPrices, //
			TimeOfUsePrices gridSellPrices) {
		return Optional//
				.ofNullable(DateUtils.min(//
						consumptionValues.getLastTime(), //
						productionValues.getLastTime(), //
						gridBuyPrices.getLastTime(), //
						gridSellPrices.getLastTime()))//
				.map(i -> i.atZone(startTime.getZone()))//
				.orElse(startTime.plusMinutes(SCHEDULE_PERIODS_ON_EMPTY * 15));
	}

	private GlobalOptimizationContext.Grid buildGrid() {
		return new GlobalOptimizationContext.Grid(//
				/* maxBuyPower */ this.meta.getGridBuyHardLimit(), //
				/* maxSellPower */ this.meta.getGridSellHardLimit(), //
				/* gridBuySoftLimit */ this.meta.getGridBuySoftLimit());
	}

	private GlobalOptimizationContext.Ess buildEss(int essCapacity, int essSoc) {
		final int essInitialEnergy = socToEnergy(essCapacity, essSoc);

		// Power Values for scheduling battery for individual periods.
		final int maxDischargePower = maxInt(1000 /* at least 1000 W */, //
				this.sum.getEssMaxDischargePower().get());
		final int maxChargePower = minInt(-1000 /* at least 1000 W */, //
				this.sum.getEssMinDischargePower().get());

		return new GlobalOptimizationContext.Ess(//
				essInitialEnergy, //
				essCapacity, //
				abs(maxChargePower), //
				maxDischargePower);
	}

	private Periods buildPeriods(//
			ZonedDateTime startTime, //
			JSCalendar.OneTasks<GridBuySoftLimit> gridBuySoftLimits, //
			Prediction productionValues, //
			Prediction consumptionValues, //
			TimeOfUsePrices gridBuyPrices, //
			TimeOfUsePrices gridSellPrices) {
		final var periodsBuilder = Periods.builder(this.environment);
		for (var i = 0;; i++) {
			final var time = startTime.plusMinutes(i * 15L);
			final var gridBuySoftLimit = Optional.ofNullable(gridBuySoftLimits.getPayloadAt(time)) //
					.map(GridBuySoftLimit::power) //
					.map(QUARTER::convertPowerToEnergy) //
					.orElse(null);
			final int production = QUARTER.convertPowerToEnergy(//
					productionValues.getAtOrElse(time, 0 /* defaults to zero */));
			final var consumption = Optional.ofNullable(consumptionValues.getAt(time)) //
					.map(QUARTER::convertPowerToEnergy) //
					.orElse(null);
			final var gridBuyPrice = gridBuyPrices.getAt(time);
			final var gridSellPrice = gridSellPrices.getAt(time);

			final var rawPeriodData = new RawPeriod.RawPeriodData(production, consumption, gridBuyPrice, gridSellPrice);
			final var rawPeriod = new RawPeriod(time, gridBuySoftLimit, rawPeriodData);
			if (!periodsBuilder.tryAddPeriod(rawPeriod)) {
				break;
			}
		}
		return periodsBuilder.build();
	}

	private void logInfo(String message) {
		switch (this.logVerbosity) {
		case NONE, DEBUG_LOG -> doNothing();
		case TRACE -> this.log.info("OPTIMIZER {}", message);
		}
	}
}
