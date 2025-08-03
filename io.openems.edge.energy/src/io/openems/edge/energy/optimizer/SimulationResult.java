package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.Genotype;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Hour;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Quarter;
import io.openems.edge.energy.optimizer.Simulator.EshToMode;

public record SimulationResult(//
		Fitness fitness, //
		ImmutableSortedMap<ZonedDateTime, Period> periods, //
		ImmutableMap<//
				? extends EnergyScheduleHandler.WithDifferentModes, //
				ImmutableSortedMap<ZonedDateTime, DifferentModes.Period.Transition>> schedules, //
		ImmutableSet<? extends EnergyScheduleHandler.WithOnlyOneMode> eshsWithOnlyOneMode) {

	/**
	 * A Period in a {@link SimulationResult}. Duration of one period is always one
	 * quarter.
	 */
	public record Period(//
			GlobalOptimizationContext.Period period, //
			EnergyFlow energyFlow, //
			int essInitialEnergy //
	) {

		/**
		 * Constructor for {@link Period}.
		 * 
		 * @param gocPeriod        the {@link GlobalOptimizationContext.Period}
		 * @param energyFlow       the {@link EnergyFlow}
		 * @param essInitialEnergy the initial ESS energy in the beginning of the period
		 *                         in [Wh]
		 * @return a {@link Period}
		 */
		public static Period from(GlobalOptimizationContext.Period gocPeriod, EnergyFlow energyFlow,
				int essInitialEnergy) {
			return new Period(gocPeriod, energyFlow, essInitialEnergy);
		}
	}

	/**
	 * An empty {@link SimulationResult}.
	 */
	public static final SimulationResult EMPTY_SIMULATION_RESULT = new SimulationResult(new Fitness(), //
			ImmutableSortedMap.of(), ImmutableMap.of(), ImmutableSet.of());

	/**
	 * Re-Simulate a {@link Genotype} to create a {@link SimulationResult}.
	 * 
	 * @param cache    the {@link GenotypeCache}
	 * @param goc      the {@link GlobalOptimizationContext}
	 * @param schedule the schedule as defined by {@link EshCodec}
	 * @return the {@link SimulationResult}
	 */
	private static SimulationResult from(GlobalOptimizationContext goc, int[][] schedule) {
		var allPeriods = ImmutableSortedMap.<ZonedDateTime, Period>naturalOrder();
		var allEshToModes = new ArrayList<EshToMode>();
		var fitness = Simulator.simulate(goc, schedule, new Simulator.BestScheduleCollector(//
				p -> allPeriods.put(p.period().time(), p), //
				allEshToModes::add));

		var schedules = allEshToModes.stream() //
				.collect(toImmutableMap(EshToMode::esh, //
						eshToMode -> {
							var p = eshToMode.period();
							return ImmutableSortedMap.of(p.period.time(),
									new DifferentModes.Period.Transition(p.period.duration(),
											eshToMode.postProcessedModeIndex(), p.period.price(), p.energyFlow(),
											p.essInitialEnergy()));
						}, //
						(a, b) -> ImmutableSortedMap.<ZonedDateTime, DifferentModes.Period.Transition>naturalOrder()
								.putAll(a).putAll(b).build()));

		var eshsWithOnlyOneMode = goc.eshs().stream() //
				.filter(EnergyScheduleHandler.WithOnlyOneMode.class::isInstance)
				.map(EnergyScheduleHandler.WithOnlyOneMode.class::cast) //
				.collect(toImmutableSet());

		return new SimulationResult(fitness, allPeriods.build(), schedules, eshsWithOnlyOneMode);
	}

	/**
	 * Re-Simulate a {@link Genotype} to create a {@link SimulationResult}.
	 * 
	 * <p>
	 * This method re-simulates using the {@link Quarter} periods and not (only) the
	 * {@link Hour} periods.
	 * 
	 * @param goc      the {@link GlobalOptimizationContext}
	 * @param schedule the schedule as defined by {@link EshCodec}
	 * @return the {@link SimulationResult}
	 */
	public static SimulationResult fromQuarters(GlobalOptimizationContext goc, int[][] schedule) {
		if (goc == null || schedule.length == 0) {
			return EMPTY_SIMULATION_RESULT;
		}

		// Convert to Quarters
		final var quarterPeriods = goc.periods().stream() //
				.flatMap(period -> switch (period) {
				case GlobalOptimizationContext.Period.Hour ph //
					-> ph.quarterPeriods().stream();
				case GlobalOptimizationContext.Period.Quarter pq //
					-> Stream.of(period);
				}) //
				.collect(ImmutableList.<GlobalOptimizationContext.Period>toImmutableList());
		final var quarterGoc = new GlobalOptimizationContext(goc.clock(), goc.riskLevel(), goc.startTime(), goc.eshs(),
				goc.eshsWithDifferentModes(), goc.grid(), goc.ess(), quarterPeriods);
		final var quarterSchedule = IntStream.range(0, goc.periods().size()) //
				.flatMap(periodIndex -> switch (goc.periods().get(periodIndex)) {
				case GlobalOptimizationContext.Period.Hour ph //
					-> ph.quarterPeriods().stream().mapToInt(ignore -> periodIndex); // repeat
				case GlobalOptimizationContext.Period.Quarter pq //
					-> IntStream.of(periodIndex);
				}) //
				.mapToObj(periodIndex //
				-> IntStream.range(0, goc.eshsWithDifferentModes().size()) //
						.map(eshIndex -> {
							if (periodIndex < schedule.length && eshIndex < schedule[periodIndex].length) {
								return schedule[periodIndex][eshIndex];
							}
							if (periodIndex < goc.eshsWithDifferentModes().size()) {
								return goc.eshsWithDifferentModes().get(periodIndex).getDefaultModeIndex();
							}
							return 0;
						}) //
						.toArray()) //
				.toArray(int[][]::new);

		return from(quarterGoc, quarterSchedule);
	}

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private static void log(StringBuilder b, String format, Object... args) {
		b.append(String.format(Locale.ENGLISH, format, args).toString());
	}

	/**
	 * Builds a log string of this {@link SimulationResult}.
	 * 
	 * @return log string
	 */
	public String toLogString(String prefix) {
		var b = new StringBuilder(prefix) //
				.append("Time  Price  Prod  Cons   Ess  Grid ProdToCons ProdToGrid ProdToEss GridToCons GridToEss EssToCons EssInitial");
		var firstEntry = this.periods.firstEntry();
		if (firstEntry != null) {
			firstEntry.getValue().energyFlow.getManagedCons().keySet() //
					.forEach(v -> log(b, " %-10s", v.substring(Math.max(0, v.length() - 10))));
		}
		b.append("\n");
		this.periods.entrySet().forEach(e -> {
			final var time = e.getKey();
			final var p = e.getValue();
			final var ef = p.energyFlow;
			log(b, "%s", prefix);
			log(b, "%s ", time.format(TIME_FORMATTER));
			log(b, "%5.0f ", p.period.price());
			log(b, "%5d ", ef.getProd());
			log(b, "%5d ", ef.getCons());
			log(b, "%5d ", ef.getEss());
			log(b, "%5d ", ef.getGrid());
			log(b, "%10d ", ef.getProdToCons());
			log(b, "%10d ", ef.getProdToGrid());
			log(b, "%9d ", ef.getProdToEss());
			log(b, "%10d ", ef.getGridToCons());
			log(b, "%9d ", ef.getGridToEss());
			log(b, "%9d ", ef.getEssToCons());
			log(b, "%10d ", p.essInitialEnergy);
			ef.getManagedCons().values().stream() //
					.forEach(v -> log(b, "%10d", v));
			this.schedules.forEach((esh, schedule) -> {
				log(b, " %-10s ", esh.toModeString(schedule.get(time).modeIndex()));
			});
			b.append("\n");
		});
		b.append(prefix).append("fitness=").append(this.fitness);
		return b.toString();
	}
}
