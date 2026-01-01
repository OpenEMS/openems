package io.openems.edge.energy.api.test;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation.Transition;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public class EnergyScheduleTester {

	/**
	 * Builds an {@link EnergyScheduleTester}.
	 * 
	 * @param eshs the {@link EnergyScheduleHandler}s
	 * @return the {@link EnergyScheduleTester}
	 */
	public static EnergyScheduleTester from(EnergyScheduleHandler... eshs) {
		var perEsh = ImmutableList.<EshContainer>builder();
		var goc = DummyGlobalOptimizationContext.fromHandlers(eshs);
		for (var esh : eshs) {
			final var csc = ((AbstractEnergyScheduleHandler<?, ?>) esh /* this is safe */).initialize(goc);
			final var initialPopulation = (esh instanceof EshWithDifferentModes<?, ?, ?> ewdf) //
					? ewdf.getInitialPopulation(goc) //
					: null;
			perEsh.add(new EshContainer(initialPopulation, csc));
		}
		return new EnergyScheduleTester(goc, perEsh.build());
	}

	public final GlobalOptimizationContext goc;

	public record EshContainer(ImmutableList<Transition> initialPopulation, Object csc) {
	}

	public final ImmutableList<EshContainer> perEsh;
	public final ImmutableMap<EnergyScheduleHandler, Object> cscs;

	private final Logger log = LoggerFactory.getLogger(EnergyScheduleTester.class);

	private int nextPeriod;

	private EnergyScheduleTester(GlobalOptimizationContext goc, ImmutableList<EshContainer> perEsh) {
		this.goc = goc;
		this.perEsh = perEsh;

		final var cscsBuilder = ImmutableMap.<EnergyScheduleHandler, Object>builder();
		for (var esh : goc.eshs()) {
			var csc = esh.createScheduleContext();
			if (csc != null) {
				cscsBuilder.put(esh, csc);
			}
		}
		this.cscs = cscsBuilder.build();
	}

	public static record SimulatedPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc,
			EnergyFlow.Model ef, Fitness fitness) {
	}

	/**
	 * Test next Period.
	 * 
	 * @param fitness the {@link Fitness} result
	 * @param modes   simulated Modes
	 * @return a {@link SimulatedPeriod} record
	 */
	public SimulatedPeriod simulatePeriod(Fitness fitness, int... modes) {
		var index = this.nextPeriod++;
		var gsc = GlobalScheduleContext.from(this.goc);
		var period = this.goc.periods().get(index);

		final EnergyFlow.Model ef;
		try {
			ef = EnergyFlow.Model.from(gsc, period);
		} catch (OpenemsException e) {
			this.log.error("Error while simulating period [" + index + "]", e);
			fitness.addHardConstraintViolation();
			return new SimulatedPeriod(period, gsc, null, fitness);
		}

		var eshIndex = 0;
		for (var esh : this.goc.eshs()) {
			var csc = this.cscs.get(esh);
			switch (esh) {
			case EnergyScheduleHandler.WithDifferentModes e -> {
				final var modeIndex = e.modes().hasForOptimizer() //
						? -1 // none available
						: modes[eshIndex++];
				final var preProcessedMode = e.preProcessPeriod(period, gsc, modeIndex);
				e.simulate(period, gsc, csc, ef, preProcessedMode, fitness);
			}
			case EnergyScheduleHandler.WithOnlyOneMode e //
				-> e.simulate(period, gsc, csc, ef, fitness);
			}
		}

		return new SimulatedPeriod(period, gsc, ef, fitness);
	}

	/**
	 * Test next Period.
	 * 
	 * @param modes simulated Modes
	 * @return a {@link SimulatedPeriod} record
	 */
	public SimulatedPeriod simulatePeriod(int... modes) {
		var fitness = new Fitness();
		return this.simulatePeriod(fitness, modes);
	}

	/**
	 * Test Period with given index.
	 * 
	 * @param index the index of the period
	 * @param modes simulated Modes
	 * @return a {@link SimulatedPeriod} record
	 */
	public SimulatedPeriod simulatePeriodIndex(int index, int... modes) {
		return this.simulatePeriodIndex(index, new Fitness(), modes);
	}

	/**
	 * Test Period with given index.
	 * 
	 * @param index   the index of the period
	 * @param fitness the {@link Fitness} result
	 * @param modes   simulated Modes
	 * @return a {@link SimulatedPeriod} record
	 */
	public SimulatedPeriod simulatePeriodIndex(int index, Fitness fitness, int... modes) {
		this.nextPeriod = index;
		return this.simulatePeriod(fitness, modes);
	}

	@Override
	public String toString() {
		return toStringHelper(this) //
				.addValue(this.goc) //
				.add("perEsh", this.perEsh) //
				.add("cscs", this.cscs) //
				.toString();
	}
}
