package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.test.DummyGlobalSimulationsContext;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class SimulatorTest {

	public static final EnergyScheduleHandler.WithOnlyOneState<Integer> ESH0 = //
			EnergyScheduleHandler.WithOnlyOneState.<Integer>create() //
					.setContextFunction(simContext -> simContext.ess().totalEnergy()) //
					.setSimulator((simContext, period, energyFlow, ctrlContext) -> {
						var minEnergy = socToEnergy(simContext.global.ess().totalEnergy(), 10 /* [%] */);
						energyFlow.setEssMaxDischarge(Math.max(0, simContext.ess.getInitialEnergy() - minEnergy));
					}) //
					.build();

	public static final ManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(10_000) //
			.withAllowedChargePower(8_000) //
			.withAllowedDischargePower(8_000) //
			.withCapacity(22_000);
	public static final EnergyScheduleHandler.WithDifferentStates<?, ?> ESH_TIME_OF_USE_TARIFF_CTRL = TimeOfUseTariffControllerImpl
			.buildEnergyScheduleHandler(//
					() -> ESS, //
					() -> ControlMode.CHARGE_CONSUMPTION, //
					() -> 20_000 /* maxChargePowerFromGrid */);

	protected static enum Esh2State {
		FOO, BAR;
	}

	public static final EnergyScheduleHandler.WithDifferentStates<Esh2State, Object> ESH2 = //
			EnergyScheduleHandler.WithDifferentStates.<Esh2State, Object>create() //
					.setDefaultState(Esh2State.BAR) //
					.setAvailableStates(() -> Esh2State.values()) //
					.setContextFunction(simContext -> null) //
					.setSimulator((simContext, period, energyFlow, ctrlContext, state) -> {
						return 0.;
					}) //
					.build();

	public static final Simulator DUMMY_SIMULATOR = new Simulator(//
			DummyGlobalSimulationsContext.fromHandlers(ESH0, ESH_TIME_OF_USE_TARIFF_CTRL, ESH2));

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

		return simulator.getBestSchedule(SimulationResult.EMPTY, true /* isCurrentPeriodFixed */, //
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

		assertEquals("BALANCING", ESH_TIME_OF_USE_TARIFF_CTRL.getCurrentPeriod().state().toString());
	}
}