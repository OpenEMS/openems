package io.openems.edge.energy.task.smart;

import static io.openems.edge.energy.api.simulatable.ExecutionPlan.NO_OF_PERIODS;
import static java.util.stream.Collectors.joining;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Optimize;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import io.jenetics.util.RandomRegistry;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.energy.api.simulatable.ExecutionPlan;
import io.openems.edge.energy.api.simulatable.Forecast;
import io.openems.edge.energy.api.simulatable.Simulatable;
import io.openems.edge.energy.api.simulatable.Simulator;

public class Optimizer {

	private static final Logger LOG = LoggerFactory.getLogger(Optimizer.class);

	private Optimizer() {
	}

	/**
	 * Gets the best {@link ExecutionPlan}.
	 * 
	 * @param forecast     the {@link Forecast}
	 * @param controllers  the {@link Controller}s, sorted by OpenEMS Scheduler
	 * @param simulatables the {@link Simulatable} {@link OpenemsComponent}s
	 * @return the {@link ExecutionPlan}; or null
	 */
	protected static ExecutionPlan getBestExecutionPlan(Forecast forecast, Controller[] controllers,
			Simulatable[] simulatables) {
		var schedulables = Stream.of(controllers) //
				.filter(Schedulable.class::isInstance) //
				.map(Schedulable.class::cast) //
				.toArray(Schedulable[]::new);

		LOG.info("### getBestExecutionPlan");
		LOG.info("Schedulables: " + Stream.of(schedulables).map(Controller::id).collect(joining(", ")));
		LOG.info("Simulatables: " + Stream.of(simulatables).map(OpenemsComponent::id).collect(joining(", ")));

		// if (schedulables.length == 0 && simulateables.length == 0) {
		// throw new OpenemsException("Unable to find any Simulateables or
		// Schedulables");
		// }

		// Jenetics
		var gtf = Genotype.of(//
				Stream.of(schedulables) //
						.map(s -> IntegerChromosome.of(0, s.getScheduleHandler().presets.length - 1, NO_OF_PERIODS)) //
						.collect(Collectors.toUnmodifiableList()));

		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
			// var executionPlan = simulateGenotype(simulatables, scheduleables,
			// scheduledControllers, forecast, gt);
			// var cost = executionPlan.getTotalGridCost();
			// TODO consider further function costs, e.g. target SoC
			// return cost;
			return Math.random();
		};
		var engine = Engine //
				.builder(eval, gtf) //
				.executor(Runnable::run) // current thread
				.optimize(Optimize.MINIMUM) //
				.build();
		var statistics = EvolutionStatistics.<Double>ofNumber();
		var bestGt = RandomRegistry.with(new Random(315 /* this is reproducible */), r -> engine.stream() //
				.limit(Limits.bySteadyFitness(1000)) //
				.limit(Limits.byExecutionTime(Duration.ofMinutes(5))) //
				.peek(statistics) //
				.collect(EvolutionResult.toBestGenotype()) //
		);
		LOG.info(statistics.toString());

		// Recalculate and print best plan
		return simulateGenotype(simulatables, schedulables, controllers, forecast, bestGt);
	}

	private static ExecutionPlan simulateGenotype(Simulatable[] simulatables, Schedulable[] schedulables,
			Controller[] controllers, Forecast forecast, Genotype<IntegerGene> gt) {
		var executionPlan = buildExecutionPlan(schedulables, forecast, gt);
		var simulators = Stream.of(simulatables) //
				.collect(Collectors.toUnmodifiableMap(Simulatable::id, Simulatable::getSimulator));
		var remainingSimulators = Stream.of(simulatables) //
				.map(Simulatable::id) //
				.collect(Collectors.toSet());
		executionPlan.periods().forEach(period -> {
			// Simulate in order of Scheduler
			Stream.of(controllers).forEach(controller -> {
				remainingSimulators.remove(controller.id());
				simulatePeriod(simulators, controller.id(), period);
			});
			// Simulate remaining simulators (i.e. Devices)
			for (var id : remainingSimulators) {
				simulatePeriod(simulators, id, period);
			}
		});
		return executionPlan;
	}

	/**
	 * Converts the {@link Genotype} to an {@link ExecutionPlan}.
	 * 
	 * @param schedulables the {@link Schedulable} components
	 * @param forecast     the {@link Forecast}
	 * @param gt           the current {@link Genotype}
	 * @return the {@link ExecutionPlan}, ready for simulation
	 */
	protected static ExecutionPlan buildExecutionPlan(Schedulable[] schedulables, Forecast forecast,
			Genotype<IntegerGene> gt) {
		var result = ExecutionPlan.create(forecast);
		IntStream.range(0, schedulables.length).forEach(i -> {
			var schedulable = schedulables[i];
			var presets = schedulable.getScheduleHandler().presets;
			result.add(schedulable.id(), //
					IntStream.range(0, NO_OF_PERIODS) //
							.mapToObj(period -> presets[gt.get(i).get(period).intValue()]) //
							.toArray(Schedule.Preset[]::new));
		});
		return result.build();
	}

	private static void simulatePeriod(Map<String, Simulator> simulators, String simulatorId,
			ExecutionPlan.Period period) {
		// var simulator = simulators.get(simulatorId);
		// if (simulator == null) {
		// return;
		// }
		// if (simulator instanceof PresetSimulator<?>) {
		// ((PresetSimulator<?>) simulator).simulate(period, simulatorId);
		// } else {
		// simulator.simulate(period);
		// }
	}
}
