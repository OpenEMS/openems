package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import io.jenetics.engine.Limits;
import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.test.DummyGlobalSimulationsContext;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class SimulatorTest {

	public static final EnergyScheduleHandler.WithOnlyOneState<Integer> ESH0 = EnergyScheduleHandler.<Integer>of(//
			simContext -> simContext.ess().totalEnergy(), //
			(simContext, period, energyFlow, ctrlContext) -> {
				var minEnergy = socToEnergy(simContext.global.ess().totalEnergy(), 10 /* [%] */);
				energyFlow.setEssMaxDischarge(Math.max(0, simContext.getEssInitial() - minEnergy));
			});

	public static final ManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(10_000) //
			.withAllowedChargePower(8_000) //
			.withAllowedDischargePower(8_000) //
			.withCapacity(22_000);
	public static final EnergyScheduleHandler.WithDifferentStates<?, ?> ESH_TIME_OF_USE_TARIFF_CTRL = TimeOfUseTariffControllerImpl
			.buildEnergyScheduleHandler(//
					() -> ESS, //
					() -> ControlMode.CHARGE_CONSUMPTION, //
					() -> 20_000 /* maxChargePowerFromGrid */, //
					() -> false /* limitChargePowerFor14aEnWG */);

	private static enum Esh2State {
		FOO, BAR;
	}

	public static final EnergyScheduleHandler.WithDifferentStates<Esh2State, Object> ESH2 = EnergyScheduleHandler.of(//
			Esh2State.BAR, //
			() -> Esh2State.values(), //
			simContext -> null, //
			(simContext, period, energyFlow, ctrlContext, state) -> {
			});

	public static final GlobalSimulationsContext DUMMY_GSC = DummyGlobalSimulationsContext.fromHandlers(//
			ESH0, ESH_TIME_OF_USE_TARIFF_CTRL, ESH2);

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
		final var gsc = DUMMY_GSC;
		gsc.initializeEnergyScheduleHandlers();

		return Simulator.getBestSchedule(gsc, SimulationResult.EMPTY, //
				engine -> engine //
						.populationSize(1), //
				stream -> stream //
						.limit(Limits.byFixedGeneration(1)));
	}

	@Test
	public void testGetBestSchedule() {
		var simulationResult = generateDummySimulationResult();

		assertEquals(2, simulationResult.schedules().size());

		simulationResult.schedules().forEach((esh, schedule) -> {
			esh.applySchedule(schedule);
		});

		assertEquals("BALANCING", ESH_TIME_OF_USE_TARIFF_CTRL.getCurrentPeriod().state().toString());
		assertEquals("BAR", ESH2.getCurrentPeriod().state().toString());
	}
}