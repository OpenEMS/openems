package io.openems.edge.energy;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.CollectorUtils.toSortedMap;
import static io.openems.common.utils.JsonUtils.getAsOptionalDouble;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.toSet;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.sum.Sum;
import io.openems.common.jsonrpc.type.ScheduleUpdate.EshData;
import io.openems.common.jsonrpc.type.ScheduleUpdate.Period;
import io.openems.common.jsonrpc.type.ScheduleUpdate.PeriodType;
import io.openems.common.jsonrpc.type.ScheduleUpdate.SumData;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.periods.PeriodData;
import io.openems.edge.energy.optimizer.SimulationResult;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timeofusetariff.api.TariffManager;

final class SchedulePeriodsFactory {

	private static final int HISTORY_RESOLUTION_MINUTES = 5;
	private static final int PREDICTION_RESOLUTION_MINUTES = 15;

	private SchedulePeriodsFactory() {
	}

	static ImmutableList<Period> create(//
			ZonedDateTime fromDate, //
			Clock clock, //
			Timedata timedata, //
			PredictorManager predictorManager, //
			TariffManager tariffManager, //
			SimulationResult simulationResult) {

		final var fromRaw = DateUtils.roundDownToQuarter(fromDate);

		// One or two full days
		final var from = fromRaw.toLocalTime() == LocalTime.MIDNIGHT //
				? fromRaw //
				: fromRaw.truncatedTo(ChronoUnit.DAYS);
		final var toRaw = fromRaw.plusHours(24);
		final var to = toRaw.toLocalTime() == LocalTime.MIDNIGHT //
				? toRaw //
				: toRaw.plusDays(1).truncatedTo(ChronoUnit.DAYS);

		final var nowRaw = ZonedDateTime.now(clock);
		final var now = to.isBefore(nowRaw) //
				? DateUtils.roundDownToMinutes(nowRaw, HISTORY_RESOLUTION_MINUTES) //
				: DateUtils.roundDownToMinutes(nowRaw, PREDICTION_RESOLUTION_MINUTES);

		final Stream<Period> historyPeriods = now.isBefore(from) //
				? Stream.empty() //
				: Periods.fromHistory(//
						from, //
						now.isAfter(to) ? to : now, //
						timedata, //
						simulationResult.schedules().keySet());

		final Stream<Period> predictionPeriods = now.isAfter(to) //
				? Stream.empty() //
				: Periods.fromPrediction(//
						now.isAfter(from) ? now : from, //
						to, //
						tariffManager, //
						predictorManager, //
						simulationResult);

		return Streams.concat(historyPeriods, predictionPeriods) //
				.collect(toImmutableList());
	}

	static final ChannelAddress SUM_PRODUCTION = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id());

	static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.CONSUMPTION_ACTIVE_POWER.id());

	static final ChannelAddress SUM_GRID = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.GRID_ACTIVE_POWER.id());

	static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER.id());

	static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.ESS_DISCHARGE_POWER.id());

	static final ChannelAddress SUM_ESS_SOC = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.ESS_SOC.id());

	static final ChannelAddress SUM_GRID_BUY_PRICE = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.GRID_BUY_PRICE.id());

	static final ChannelAddress SUM_GRID_SELL_PRICE = new ChannelAddress(//
			Sum.SINGLETON_COMPONENT_ID, //
			Sum.ChannelId.GRID_SELL_PRICE.id());

	private static final class Periods {

		private static final Logger LOG = LoggerFactory.getLogger(Periods.class);

		private static final Comparator<EshData> ESH_DATA_COMPARATOR = Comparator //
				.comparing(EshData::id) //
				.thenComparing(EshData::mode, nullsFirst(naturalOrder()));

		private Periods() {
		}

		private static Stream<Period> fromHistory(//
				ZonedDateTime from, //
				ZonedDateTime to, //
				Timedata timedata, //
				ImmutableSet<? extends EnergyScheduleHandler.WithDifferentModes> eshwdms) {

			final var channels = Streams //
					.concat(//
							Stream.of(//
									SUM_GRID_BUY_PRICE, //
									SUM_GRID_SELL_PRICE, //
									SUM_GRID, //
									SUM_PRODUCTION, //
									SUM_CONSUMPTION, //
									SUM_UNMANAGED_CONSUMPTION, //
									SUM_ESS_DISCHARGE_POWER, //
									SUM_ESS_SOC), //
							eshwdms.stream() //
									.flatMap(esh -> switch (esh.modes()) {
									case DifferentModes.Modes.JointModes<?> jointModes ->
										jointModes.channels.values().stream();

									case DifferentModes.Modes.SingleModes<?> singleModes ->
										Stream.of(singleModes.channels);
									}) //
									.filter(Objects::nonNull) //
									.flatMap(channelsForMode -> Stream.of(//
											channelsForMode.mode(), //
											channelsForMode.managedConsumptionPower()))) //
					.filter(Objects::nonNull) //
					.collect(toSet());

			final var historicData = queryHistoricData(//
					timedata, //
					from, //
					to, //
					channels, //
					new Resolution(HISTORY_RESOLUTION_MINUTES, MINUTES));

			final var emptyData = new TreeMap<ChannelAddress, JsonElement>();

			return Stream.iterate(//
					from, //
					timestamp -> timestamp.plus(HISTORY_RESOLUTION_MINUTES, MINUTES)) //
					.takeWhile(timestamp -> timestamp.isBefore(to)) //
					.map(timestamp -> {

						final var periodData = historicData.getOrDefault(//
								timestamp.toInstant(), //
								emptyData);

						final Function<ChannelAddress, JsonElement> getValue = channel -> //
						Optional.ofNullable(periodData.get(channel)) //
								.orElse(JsonNull.INSTANCE);

						final Function<ChannelAddress, Double> getDouble = channel -> //
						getAsOptionalDouble(getValue.apply(channel)) //
								.orElse(null);

						final Function<ChannelAddress, Integer> getInteger = channel -> //
						getAsOptionalInt(getValue.apply(channel)) //
								.orElse(null);

						final var sumData = new SumData(//
								getDouble.apply(SUM_GRID_BUY_PRICE), //
								getDouble.apply(SUM_GRID_SELL_PRICE), //
								getInteger.apply(SUM_PRODUCTION), //
								getInteger.apply(SUM_CONSUMPTION), //
								getInteger.apply(SUM_UNMANAGED_CONSUMPTION), //
								getInteger.apply(SUM_ESS_DISCHARGE_POWER), //
								getInteger.apply(SUM_ESS_SOC), //
								getInteger.apply(SUM_GRID));

						final var eshsData = eshwdms.stream() //
								.flatMap(esh -> switch (esh.modes()) {

								case DifferentModes.Modes.SingleModes<?> singleModes -> //
									Stream.ofNullable(singleModes.channels) //
											.map(channelsForMode -> new EshData(//
													esh.getParentId(), //
													channelsForMode.mode() == null //
															? null //
															: getInteger.apply(channelsForMode.mode()), //
													channelsForMode.managedConsumptionPower() == null //
															? null //
															: getInteger
																	.apply(channelsForMode.managedConsumptionPower())));

								case DifferentModes.Modes.JointModes<?> jointModes -> //
									jointModes.channels.entrySet().stream() //
											.map(entry -> Optional.ofNullable(//
													getInteger.apply(entry.getValue().mode())) //
													.map(mode -> new EshData(//
															entry.getKey(), //
															mode, //
															getInteger
																	.apply(entry.getValue().managedConsumptionPower()))) //
													.orElse(null)) //
											.filter(Objects::nonNull);
								}) //
								.sorted(ESH_DATA_COMPARATOR) //
								.collect(toImmutableList());

						return new Period(//
								timestamp.toInstant(), //
								PeriodType.HISTORY, //
								sumData, //
								eshsData);
					});
		}

		private static Stream<Period> fromPrediction(//
				ZonedDateTime from, //
				ZonedDateTime to, //
				TariffManager tariffManager, //
				PredictorManager predictorManager, //
				SimulationResult simulationResult) {

			final var gridBuyPrices = tariffManager.getGridBuyDayAheadPrices();
			final var gridSellPrices = tariffManager.getGridSellDayAheadPrices();

			final var unmanagedConsumptionPrediction = predictorManager //
					.getPrediction(SUM_UNMANAGED_CONSUMPTION);

			final var productionPrediction = predictorManager //
					.getPrediction(SUM_PRODUCTION);

			final var simulationPeriods = simulationResult.periods().entrySet().stream() //
					.collect(toSortedMap(//
							entry -> entry.getKey().toInstant(), //
							entry -> entry.getValue()));

			return Stream.iterate(//
					from, //
					timestamp -> timestamp.plus(PREDICTION_RESOLUTION_MINUTES, MINUTES)) //
					.takeWhile(timestamp -> timestamp.isBefore(to)) //
					.map(timestamp -> {

						final var instant = timestamp.toInstant();

						final var simulationPeriod = Optional.ofNullable(//
								simulationPeriods.get(instant));

						final var period = simulationPeriod.map(p -> p.period());
						final var periodData = period.map(p -> p.data());
						final var energyFlow = simulationPeriod.map(p -> p.energyFlow());
						final var duration = period.map(p -> p.duration());

						final IntUnaryOperator convertEnergyToPower = energy -> //
						duration.get().convertEnergyToPower(energy);

						final var gridBuyPrice = periodData //
								.flatMap(PeriodData::gridBuyPrice) //
								.map(PeriodData.Price::actual) //
								.orElse(gridBuyPrices.getAt(instant));

						final var gridSellPrice = periodData //
								.flatMap(PeriodData::gridSellPrice) //
								.map(PeriodData.Price::actual) //
								.orElse(gridSellPrices.getAt(instant));

						final var productionActivePower = energyFlow //
								.map(EnergyFlow::getProduction) //
								.map(convertEnergyToPower::applyAsInt) //
								.orElse(productionPrediction.getAt(instant));

						final var unmanagedConsumptionActivePower = energyFlow //
								.map(EnergyFlow::getUnmanagedConsumption) //
								.map(convertEnergyToPower::applyAsInt) //
								.orElse(unmanagedConsumptionPrediction.getAt(instant));

						final var consumptionActivePower = energyFlow //
								.map(EnergyFlow::getConsumption) //
								.map(convertEnergyToPower::applyAsInt) //
								.orElse(unmanagedConsumptionActivePower);

						final var essDischargePower = energyFlow //
								.map(EnergyFlow::getEss) //
								.map(convertEnergyToPower::applyAsInt) //
								.orElse(null);

						final var essSoc = simulationPeriod //
								.map(p -> Math.round(//
										p.essInitialEnergy() * 100f //
												/ simulationResult.ess().totalEnergy())) //
								.orElse(null);

						final var gridActivePower = energyFlow //
								.map(EnergyFlow::getGrid) //
								.map(convertEnergyToPower::applyAsInt) //
								.orElse(null);

						final var sumData = new SumData(//
								gridBuyPrice, //
								gridSellPrice, //
								productionActivePower, //
								consumptionActivePower, //
								unmanagedConsumptionActivePower, //
								essDischargePower, //
								essSoc, //
								gridActivePower);

						final var managedConsumptions = energyFlow //
								.map(EnergyFlow::getManagedConsumptions) //
								.orElse(ImmutableSortedMap.of());

						final var eshsData = simulationResult.schedules().entrySet().stream() //
								.flatMap(scheduleEntry -> {

									final var energyScheduleHandler = scheduleEntry.getKey();
									final var schedule = scheduleEntry.getValue();

									return Stream.ofNullable(schedule.get(timestamp)) //
											.flatMap(schedulePeriod -> switch (energyScheduleHandler.modes()) {

											case DifferentModes.Modes.SingleModes<?> singleModes -> //
												Stream.of(new EshData(//
														energyScheduleHandler.getParentId(), //
														singleModes.get(schedulePeriod.modeIndex()).getValue(), //
														Optional.ofNullable(managedConsumptions.get(//
																energyScheduleHandler.getParentId())) //
																.map(convertEnergyToPower::applyAsInt) //
																.orElse(null)));

											case DifferentModes.Modes.JointModes<?> jointModes -> //
												Stream.ofNullable(jointModes.get(schedulePeriod.modeIndex())) //
														.flatMap(jointMode -> jointMode.submodes().entrySet().stream()) //
														.map(entry -> new EshData(//
																entry.getKey(), //
																entry.getValue().getValue(), //
																Optional.ofNullable(
																		managedConsumptions.get(entry.getKey())) //
																		.map(convertEnergyToPower::applyAsInt) //
																		.orElse(null)));
											});
								}) //
								.sorted(ESH_DATA_COMPARATOR) //
								.collect(toImmutableList());

						return new Period(//
								instant, //
								PeriodType.PREDICTION, //
								sumData, //
								eshsData);
					});
		}

		private static SortedMap<Instant, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
				Timedata timedata, //
				ZonedDateTime from, //
				ZonedDateTime to, //
				Set<ChannelAddress> channels, //
				Resolution resolution) {

			if (timedata == null) {
				LOG.warn("Unable to read historic data: Timedata service is unavailable");
				return new TreeMap<>();
			}

			try {
				final var data = timedata.queryHistoricData(//
						null, //
						from, //
						to, //
						channels, //
						resolution);

				return data.entrySet().stream() //
						.collect(toSortedMap(//
								entry -> entry.getKey().toInstant(), //
								entry -> entry.getValue()));

			} catch (OpenemsError.OpenemsNamedException e) {
				LOG.warn("Unable to read historic data: {}", e.getMessage(), e);
				return new TreeMap<>();
			}
		}
	}
}
