package io.openems.edge.energy.api;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class Utils {
//
//	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
//
	private Utils() {

	}

	/**
	 * Gets the {@link ZonedDateTime} truncated to future hour.
	 * 
	 * @param now  input {@link ZonedDateTime}
	 * @param hour the hour
	 * @return truncated {@link ZonedDateTime}
	 */
	public static ZonedDateTime toZonedDateTime(ZonedDateTime now, int hour) {
		var result = now.truncatedTo(ChronoUnit.HOURS).withHour(hour);
		if (now.getHour() <= hour) {
			return result;
		} else {
			return result.plusDays(1);
		}
	}

//	/**
//	 * Gets value of array.
//	 * 
//	 * @param <T>    type of value
//	 * @param array  the array
//	 * @param index  the index
//	 * @param orElse or else value
//	 * @return value
//	 */
//	// TODO move to ArrayUtils
//	public static <T> T orElse(T[] array, int index, T orElse) {
//		if (index < array.length) {
//			return array[index];
//		}
//		return orElse;
//	}
//
//	/**
//	 * Gets the best {@link ExecutionPlan}.
//	 * 
//	 * @param forecast             the {@link Forecast}
//	 * @param components           all {@link OpenemsComponent}s, filtered for
//	 *                             {@link Simulatable}s
//	 * @param scheduledControllers the scheduled {@link Controller}s
//	 * @return the {@link ExecutionPlan}; or null
//	 */
//	public static ExecutionPlan getBestExecutionPlan(Forecast forecast, OpenemsComponent[] components,
//			Controller[] scheduledControllers) throws OpenemsException {
//		LOG.info("### getBestExecutionPlan");
//		LOG.info("Components: " + Stream.of(components).map(OpenemsComponent::id).collect(joining(", ")));
//		LOG.info("Scheduler: " + Stream.of(scheduledControllers).map(Controller::id).collect(joining(", ")));
//		LOG.info("Controllers that are not Simulatable: " + Stream.of(scheduledControllers) //
//				.filter(c -> !(c instanceof Simulatable)) //
//				.map(OpenemsComponent::id) //
//				.collect(joining(", ")));
//		var simulatables = Stream.of(components) //
//				.filter(Simulatable.class::isInstance) //
//				.map(Simulatable.class::cast) //
//				.toArray(Simulatable[]::new);
//		LOG.info("Simulatables: " + Stream.of(simulatables).map(Simulatable::id).collect(joining(", ")));
//		var scheduleables = Stream.of(components) //
//				.filter(Schedulable.class::isInstance) //
//				.map(Schedulable.class::cast) //
//				.toArray(Schedulable[]::new);
//		LOG.info("Scheduleables: " + Stream.of(scheduleables).map(Schedulable<?>::id).collect(joining(", ")));
//
//		if (simulatables.length == 0 && scheduleables.length == 0) {
//			throw new OpenemsException("Unable to find any Simulateables or Schedulables");
//		}
//
//		// Jenetics
//		var gtf = Genotype.of(//
//				Stream.of(scheduleables) //
//						.map(s -> IntegerChromosome.of(0, s.getAvailableModes().size() - 1, NO_OF_PERIODS)) //
//						.collect(Collectors.toUnmodifiableList()) //
//		);
//
//		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
//			var executionPlan = simulateGenotype(simulatables, scheduleables, scheduledControllers, forecast, gt);
//			var cost = executionPlan.getTotalGridCost();
//			// TODO consider further function costs, e.g. target SoC
//			return cost;
//		};
//		var engine = Engine //
//				.builder(eval, gtf) //
//				.executor(Runnable::run) // current thread
//				.optimize(Optimize.MINIMUM) //
//				.build();
//		var statistics = EvolutionStatistics.<Double>ofNumber();
//		var bestGt = RandomRegistry.with(new Random(315 /* this is reproducible */), r -> engine.stream() //
//				.limit(Limits.bySteadyFitness(1000)) //
//				.limit(Limits.byExecutionTime(Duration.ofMinutes(5))) //
//				.peek(statistics) //
//				.collect(EvolutionResult.toBestGenotype()) //
//		);
//		LOG.info(statistics.toString());
//
//		// Recalculate and print best plan
//		return simulateGenotype(simulatables, scheduleables, scheduledControllers, forecast, bestGt);
//	}
//
//	private static ExecutionPlan simulateGenotype(Simulatable[] simulatables, Schedulable<?>[] scheduleables,
//			Controller[] controllers, Forecast forecast, Genotype<IntegerGene> gt) {
//		var executionPlan = buildExecutionPlan(scheduleables, forecast, gt);
//		var simulators = Stream.of(simulatables) //
//				.collect(Collectors.toUnmodifiableMap(Simulatable::id, Simulatable::getSimulator));
//		var remainingSimulators = Stream.of(simulatables) //
//				.map(Simulatable::id) //
//				.collect(Collectors.toSet());
//		executionPlan.periods().forEach(period -> {
//			// Simulate in order of Scheduler
//			Stream.of(controllers).forEach(controller -> {
//				remainingSimulators.remove(controller.id());
//				simulatePeriod(simulators, controller.id(), period);
//			});
//			// Simulate remaining simulators (i.e. Devices)
//			for (var id : remainingSimulators) {
//				simulatePeriod(simulators, id, period);
//			}
//		});
//		return executionPlan;
//	}
//
//	/**
//	 * Converts the {@link Genotype} to an {@link ExecutionPlan}.
//	 * 
//	 * @param scheduleables the {@link Schedulable} components
//	 * @param forecast      the {@link Forecast}
//	 * @param gt            the current {@link Genotype}
//	 * @return the {@link ExecutionPlan}, ready for simulation
//	 */
//	private static ExecutionPlan buildExecutionPlan(Schedulable<? extends Schedule.Mode>[] scheduleables,
//			Forecast forecast, Genotype<IntegerGene> gt) {
//		var result = ExecutionPlan.create(forecast);
//		IntStream.range(0, scheduleables.length).forEach(i -> {
//			var schedulable = scheduleables[i];
//			var modes = schedulable.getAvailableModes().asList();
//			result.add(schedulable.id(), //
//					IntStream.range(0, NO_OF_PERIODS) //
//							.mapToObj(period -> modes.get(gt.get(i).get(period).intValue())) //
//							.toArray(Schedule.Mode[]::new));
//		});
//		return result.build();
//	}
//
//	private static void simulatePeriod(Map<String, Simulator> simulators, String simulatorId,
//			ExecutionPlan.Period period) {
//		var simulator = simulators.get(simulatorId);
//		if (simulator == null) {
//			return;
//		}
//		if (simulator instanceof ModeSimulator<?>) {
//			((ModeSimulator<?>) simulator).simulate(period, simulatorId);
//		} else {
//			simulator.simulate(period);
//		}
//	}
}
