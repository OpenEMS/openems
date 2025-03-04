package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.test.DummyGlobalOptimizationContext;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class SimulatorTest {

	public static final EnergyScheduleHandler.WithOnlyOneMode ESH0 = //
			EnergyScheduleHandler.WithOnlyOneMode.<Integer, Void>create() //
					.setOptimizationContext(goc -> goc.ess().totalEnergy()) //
					.setSimulator((period, gsc, coc, csc, ef) -> {
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
					.buildEnergyScheduleHandler(//
							() -> "ctrlEssTimeOfUseTariff0", //
							() -> new io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.Config(
									ControlMode.CHARGE_CONSUMPTION));

	protected static enum Esh2State {
		FOO, BAR;
	}

	public static final EnergyScheduleHandler.WithDifferentModes ESH2 = //
			EnergyScheduleHandler.WithDifferentModes.<Esh2State, Void, Void>create() //
					.setDefaultMode(Esh2State.BAR) //
					.setAvailableModes(() -> Esh2State.values()) //
					.build();

	public static final Simulator DUMMY_SIMULATOR = new Simulator(//
			DummyGlobalOptimizationContext.fromHandlers(ESH0, ESH_TIME_OF_USE_TARIFF_CTRL, ESH2));

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
	}
}