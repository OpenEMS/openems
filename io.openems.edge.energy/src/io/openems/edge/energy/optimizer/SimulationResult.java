package io.openems.edge.energy.optimizer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period.Hour;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period.Quarter;

public record SimulationResult(//
		double cost, //
		ImmutableMap<ZonedDateTime, Period> periods, //
		ImmutableMap<//
				EnergyScheduleHandler.WithDifferentStates<?, ?>, //
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
		 * @param energyFlow       the {@link EnergyFlow.Solution}
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
	public static final SimulationResult EMPTY = new SimulationResult(0., ImmutableMap.of(), ImmutableMap.of());

	/**
	 * Re-Simulate a {@link Genotype} to create a {@link SimulationResult}.
	 * 
	 * @param gsc the {@link GlobalSimulationsContext}
	 * @param gt  the {@link Genotype}
	 * @return the {@link SimulationResult}
	 */
	private static SimulationResult from(GlobalSimulationsContext gsc, Genotype<IntegerGene> gt) {
		// Re-Simulate and keep best Schedule
		var collectedPeriodsBuilder = ImmutableMap.<ZonedDateTime, Period>builder();
		var cost = Simulator.simulate(gsc, gt, p -> collectedPeriodsBuilder.put(p.context().time(), p));
		var collectedPeriods = collectedPeriodsBuilder.build();

		// Prepare individual Schedules for EnergyScheduleHandlers
		var result = ImmutableMap.<//
				EnergyScheduleHandler.WithDifferentStates<?, ?>, //
				ImmutableSortedMap<ZonedDateTime, EnergyScheduleHandler.WithDifferentStates.Period.Transition>>builder();
		var i = 0;
		for (var esh : gsc.handlers()) {
			if (esh instanceof EnergyScheduleHandler.WithDifferentStates<?, ?> e) {
				var g = gt.get(i++);
				var schedule = ImmutableSortedMap.<ZonedDateTime, EnergyScheduleHandler.WithDifferentStates.Period.Transition>naturalOrder();
				var j = 0;
				for (var period : gsc.periods()) {
					var stateIndex = g.get(j++).intValue();
					var collectedPeriod = collectedPeriods.get(period.time());
					var context = collectedPeriod.context;
					schedule.put(period.time(), new EnergyScheduleHandler.WithDifferentStates.Period.Transition(//
							stateIndex, context.price(), collectedPeriod.energyFlow, collectedPeriod.essInitialEnergy));
				}
				result.put(e, schedule.build());
			}
		}

		return new SimulationResult(cost, collectedPeriods, result.build());
	}

	/**
	 * Re-Simulate a {@link Genotype} to create a {@link SimulationResult}.
	 * 
	 * <p>
	 * This method re-simulates using the {@link Quarter} periods and not (only) the
	 * {@link Hour} periods.
	 * 
	 * @param gsc the {@link GlobalSimulationsContext}
	 * @param gt  the {@link Genotype}
	 * @return the {@link SimulationResult}
	 */
	public static SimulationResult fromQuarters(GlobalSimulationsContext gsc, Genotype<IntegerGene> gt) {
		// Convert to Quarters
		final GlobalSimulationsContext quarterGsc;
		final Genotype<IntegerGene> quarterGt;
		{
			final var quarterPeriods = ImmutableList.<GlobalSimulationsContext.Period>builder();
			final var quarterGenes = gt.stream().map(ignore -> ImmutableList.<IntegerGene>builder()).toList();
			final BiConsumer<Integer, GlobalSimulationsContext.Period.Quarter> add = (j, p) -> {
				quarterPeriods.add(p);
				for (var i = 0; i < quarterGenes.size(); i++) {
					quarterGenes.get(i).add(gt.get(i).get(j));
				}
			};
			for (var i = 0; i < gsc.periods().size(); i++) {
				var p = gsc.periods().get(i);
				if (p instanceof GlobalSimulationsContext.Period.Quarter pq) {
					add.accept(i, pq);
				} else if (p instanceof GlobalSimulationsContext.Period.Hour ph) {
					for (var j = 0; j < ph.quarterPeriods().size(); j++) {
						var pq = ph.quarterPeriods().get(j);
						add.accept(i, pq);
					}
				}
			}
			quarterGsc = new GlobalSimulationsContext(gsc.clock(), gsc.simulationCounter(), gsc.startTime(),
					gsc.handlers(), gsc.grid(), gsc.ess(), quarterPeriods.build());
			quarterGt = Genotype.of(quarterGenes.stream() //
					.map(gs -> IntegerChromosome.of(gs.build())) //
					.toList());
		}
		return from(quarterGsc, quarterGt);
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
				.append(" Time   Price Production Consumption   Ess   Grid ProdToCons ProdToGrid ProdToEss GridToCons GridToEss EssToCons EssInitial\n");
		this.periods.entrySet().forEach(e -> {
			final var time = e.getKey();
			final var p = e.getValue();
			final var c = p.context;
			final var ef = p.energyFlow;
			log(b, "%s ", prefix);
			log(b, "%s ", time.format(TIME_FORMATTER));
			log(b, "%6.2f ", c.price());
			log(b, "%10d ", ef.getProd());
			log(b, "%10d ", ef.getCons());
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
		return b.toString();
	}
}
