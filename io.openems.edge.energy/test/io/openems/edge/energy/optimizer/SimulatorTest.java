package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.test.DummyGlobalOptimizationContext;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class SimulatorTest {

	public static final EnergyScheduleHandler.WithOnlyOneMode ESH0 = //
			new OneMode.Builder<Integer, Void>("Controller.Dummy", "esh0") //
					.setOptimizationContext(goc -> goc.ess().totalEnergy()) //
					.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
						var minEnergy = socToEnergy(gsc.goc.ess().totalEnergy(), 10 /* [%] */);
						ef.setEssMaxDischarge(Math.max(0, gsc.ess.getInitialEnergy() - minEnergy));
					}) //
					.build();

	public static final ManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(10_000) //
			.withAllowedChargePower(8_000) //
			.withAllowedDischargePower(8_000) //
			.withCapacity(22_000);

	public static final EshWithDifferentModes<StateMachine, OptimizationContext, Void> ESH_TIME_OF_USE_TARIFF_CTRL = //
			io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler //
					.buildEnergyScheduleHandler(new DummyController("ctrlEssTimeOfUseTariff0"), //
							() -> new io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.Config(
									ControlMode.CHARGE_CONSUMPTION));

	protected static enum Esh2State {
		FOO, BAR;
	}

	public static final EshWithDifferentModes<Esh2State, Void, Void> ESH2 = //
			new DifferentModes.Builder<Esh2State, Void, Void>("Controller.Dummy", "esh2") //
					.setDefaultMode(Esh2State.BAR) //
					.setAvailableModes(() -> Esh2State.values()) //
					.setInitialPopulationsProvider((goc, coc, availableModes) -> {
						return ImmutableList.of(new InitialPopulation<Esh2State>(goc.periods().stream() //
								.map(p -> p.index() % 3 == 0 //
										? Esh2State.FOO // set FOO mode
										: Esh2State.BAR) // default
								.toArray(Esh2State[]::new)));
					}) //
					.build();

	public static final GlobalOptimizationContext GOC = DummyGlobalOptimizationContext.fromHandlers(ESH0,
			ESH_TIME_OF_USE_TARIFF_CTRL, ESH2);

	public static final Simulator DUMMY_SIMULATOR = new Simulator(GOC);

	public static final SimulationResult DUMMY_PREVIOUS_RESULT = SimulationResult.fromQuarters(GOC,
			new int[] { 3, 2, 1 });

	@Before
	public void before() {
		// Make reproducible results
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	/**
	 * Generates a dummy {@link SimulationResult}.
	 * 
	 * @return the {@link SimulationResult}
	 */
	public static SimulationResult generateDummySimulationResult() {
		final var simulator = DUMMY_SIMULATOR;

		return simulator.getBestSchedule(EMPTY_SIMULATION_RESULT, true /* isCurrentPeriodFixed */, //
				engine -> engine //
						.populationSize(1), //
				stream -> stream //
						.limit(byFixedGeneration(1)));
	}

	@Test
	public void testGetBestSchedule() {
		var simulationResult = generateDummySimulationResult();

		assertEquals(2, simulationResult.schedules().size());

		simulationResult.schedules().forEach((esh, schedule) -> {
			esh.applySchedule(schedule);
		});

		assertEquals("BALANCING", ESH_TIME_OF_USE_TARIFF_CTRL.getCurrentPeriod().mode().toString());
		assertEquals("BAR", ESH2.getCurrentPeriod().mode().toString());
	}
}