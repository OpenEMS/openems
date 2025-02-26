package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.Genotype;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Hour;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Quarter;
import io.openems.edge.energy.optimizer.Simulator.EshToMode;

public record SimulationResult(//
		double cost, //
		ImmutableSortedMap<ZonedDateTime, Period> periods, //
		ImmutableMap<//
				? extends EnergyScheduleHandler.WithDifferentModes, //
				ImmutableSortedMap<ZonedDateTime, DifferentModes.Period.Transition>> schedules) {

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
	public static final SimulationResult EMPTY_SIMULATION_RESULT = new SimulationResult(0., //
			ImmutableSortedMap.of(), ImmutableMap.of());

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
		var cost = Simulator.simulate(goc, schedule, new Simulator.BestScheduleCollector(//
				p -> allPeriods.put(p.period().time(), p), //
				allEshToModes::add));

		var schedules = allEshToModes.stream() //
				.collect(toImmutableMap(EshToMode::esh, //
						eshToMode -> ImmutableSortedMap.of(eshToMode.period().period.time(),
								new DifferentModes.Period.Transition(eshToMode.postProcessedModeIndex(),
										eshToMode.period().period.price(), eshToMode.period().energyFlow,
										eshToMode.period().essInitialEnergy)),
						(a, b) -> ImmutableSortedMap.<ZonedDateTime, DifferentModes.Period.Transition>naturalOrder()
								.putAll(a).putAll(b).build()));

		return new SimulationResult(cost, allPeriods.build(), schedules);
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
				.append("Time   Price Production Consumption ManagedCons    Ess   Grid ProdToCons ProdToGrid ProdToEss GridToCons GridToEss EssToCons EssInitial\n");
		this.periods.entrySet().forEach(e -> {
			final var time = e.getKey();
			final var p = e.getValue();
			final var ef = p.energyFlow;
			log(b, "%s", prefix);
			log(b, "%s ", time.format(TIME_FORMATTER));
			log(b, "%6.2f ", p.period.price());
			log(b, "%10d ", ef.getProd());
			log(b, "%11d ", ef.getCons());
			log(b, "%11d ", ef.getManagedCons());
			log(b, "%6d ", ef.getEss());
			log(b, "%6d ", ef.getGrid());
			log(b, "%10d ", ef.getProdToCons());
			log(b, "%10d ", ef.getProdToGrid());
			log(b, "%9d ", ef.getProdToEss());
			log(b, "%10d ", ef.getGridToCons());
			log(b, "%9d ", ef.getGridToEss());
			log(b, "%9d ", ef.getEssToCons());
			log(b, "%10d ", p.essInitialEnergy);
			this.schedules.forEach((esh, schedule) -> {
				log(b, "%-15s ", esh.toModeString(schedule.get(time).modeIndex()));
			});
			b.append("\n");
		});
		b.append(prefix).append("cost=").append(this.cost);
		return b.toString();
	}
}
