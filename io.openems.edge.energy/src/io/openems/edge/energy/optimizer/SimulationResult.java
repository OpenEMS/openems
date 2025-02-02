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
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period.Hour;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period.Quarter;
import io.openems.edge.energy.optimizer.Simulator.EshToState;

public record SimulationResult(//
		double cost, //
		ImmutableSortedMap<ZonedDateTime, Period> periods, //
		ImmutableMap<//
				? extends EnergyScheduleHandler.WithDifferentStates<?, ?>, //
				ImmutableSortedMap<ZonedDateTime, EnergyScheduleHandler.WithDifferentStates.Period.Transition>> schedules) {

	/**
	 * A Period in a {@link SimulationResult}. Duration of one period is always one
	 * quarter.
	 */
	public record Period(//
			GlobalSimulationsContext.Period context, //
			EnergyFlow energyFlow, //
			int essInitialEnergy //
	) {

		/**
		 * Constructor for {@link Period}.
		 * 
		 * @param context          the {@link GlobalSimulationsContext}
		 * @param energyFlow       the {@link EnergyFlow}
		 * @param essInitialEnergy the initial ESS energy in the beginning of the period
		 *                         in [Wh]
		 * @return a {@link Period}
		 */
		public static Period from(GlobalSimulationsContext.Period context, EnergyFlow energyFlow,
				int essInitialEnergy) {
			return new Period(context, energyFlow, essInitialEnergy);
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
	 * @param gsc      the {@link GlobalSimulationsContext}
	 * @param schedule the schedule as defined by {@link EshCodec}
	 * @return the {@link SimulationResult}
	 */
	private static SimulationResult from(GlobalSimulationsContext gsc, int[][] schedule) {
		var allPeriods = ImmutableSortedMap.<ZonedDateTime, Period>naturalOrder();
		var allEshToStates = new ArrayList<EshToState>();
		var cost = Simulator.simulate(gsc, schedule, new Simulator.BestScheduleCollector(//
				p -> allPeriods.put(p.context().time(), p), //
				allEshToStates::add));

		var schedules = allEshToStates.stream() //
				.collect(toImmutableMap(EshToState::esh, //
						eshToState -> ImmutableSortedMap.of(eshToState.period().context.time(),
								new EnergyScheduleHandler.WithDifferentStates.Period.Transition(
										eshToState.postProcessedStateIndex(), eshToState.period().context.price(),
										eshToState.period().energyFlow, eshToState.period().essInitialEnergy)),
						(a, b) -> ImmutableSortedMap.<ZonedDateTime, EnergyScheduleHandler.WithDifferentStates.Period.Transition>naturalOrder()
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
	 * @param gsc      the {@link GlobalSimulationsContext}
	 * @param schedule the schedule as defined by {@link EshCodec}
	 * @return the {@link SimulationResult}
	 */
	public static SimulationResult fromQuarters(GlobalSimulationsContext gsc, int[][] schedule) {
		if (gsc == null || schedule.length == 0) {
			return EMPTY_SIMULATION_RESULT;
		}

		// Convert to Quarters
		final var quarterPeriods = gsc.periods().stream() //
				.flatMap(period -> switch (period) {
				case GlobalSimulationsContext.Period.Hour ph //
					-> ph.quarterPeriods().stream();
				case GlobalSimulationsContext.Period.Quarter pq //
					-> Stream.of(period);
				}) //
				.collect(ImmutableList.<GlobalSimulationsContext.Period>toImmutableList());
		final GlobalSimulationsContext quarterGsc = new GlobalSimulationsContext(gsc.clock(), gsc.riskLevel(),
				gsc.startTime(), gsc.eshs(), gsc.eshsWithDifferentStates(), gsc.grid(), gsc.ess(), gsc.evcss(),
				quarterPeriods);
		final var quarterSchedule = IntStream.range(0, gsc.periods().size()) //
				.flatMap(periodIndex -> switch (gsc.periods().get(periodIndex)) {
				case GlobalSimulationsContext.Period.Hour ph //
					-> ph.quarterPeriods().stream().mapToInt(ignore -> periodIndex); // repeat
				case GlobalSimulationsContext.Period.Quarter pq //
					-> IntStream.of(periodIndex);
				}) //
				.mapToObj(periodIndex //
				-> IntStream.range(0, gsc.eshsWithDifferentStates().size()) //
						.map(eshIndex -> {
							if (periodIndex < schedule.length && eshIndex < schedule[periodIndex].length) {
								return schedule[periodIndex][eshIndex];
							}
							if (periodIndex < gsc.eshsWithDifferentStates().size()) {
								return gsc.eshsWithDifferentStates().get(periodIndex).getDefaultStateIndex();
							}
							return 0;
						}) //
						.toArray()) //
				.toArray(int[][]::new);

		return from(quarterGsc, quarterSchedule);
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
			final var c = p.context;
			final var ef = p.energyFlow;
			log(b, "%s", prefix);
			log(b, "%s ", time.format(TIME_FORMATTER));
			log(b, "%6.2f ", c.price());
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
				log(b, "%-15s ", esh.toStateString(schedule.get(time).stateIndex()));
			});
			b.append("\n");
		});
		b.append(prefix).append("cost=").append(this.cost);
		return b.toString();
	}
}
