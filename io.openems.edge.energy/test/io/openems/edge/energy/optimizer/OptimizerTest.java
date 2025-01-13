package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.energy.EnergySchedulerImplTest.getOptimizer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.common.test.DummyChannel;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.EnergySchedulerImplTest;
import io.openems.edge.energy.LogVerbosity;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.optimizer.SimulatorTest.Esh2State;

public class OptimizerTest {

	@Test
	public void testRunQuickOptimization() throws Exception {
		var sut = EnergySchedulerImplTest.create(createDummyClock());
		var optimizer = getOptimizer(sut);
		assertEquals("No Schedule available|PerQuarter:UNDEFINED", optimizer.debugLog());

		var simulationResult = optimizer.runQuickOptimization();
		optimizer.applySimulationResult(simulationResult);

		assertTrue(optimizer.debugLog().startsWith("ScheduledPeriods:96|SimulationCounter:"));

		var sr = optimizer.getSimulationResult();
		assertTrue(sr.cost() < 1100000);
		assertEquals(96, sr.periods().size());
	}

	@Test
	public void test2() throws InterruptedException, ExecutionException {
		var simulator = SimulatorTest.DUMMY_SIMULATOR;
		var channel = DummyChannel.of("DummyChannel");
		var optimizer = new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> simulator.gsc, //
				channel);
		var simulationResult = optimizer.runSimulation(simulator, //
				false, // current period can get adjusted
				byFixedGeneration(1) // simulate only two generations
		).get();
		optimizer.applySimulationResult(simulationResult);

		assertEquals(0., simulationResult.cost(), 0.001);
		{
			var schedule = simulator.gsc.eshsWithDifferentStates().get(0).getSchedule();
			assertEquals(52, schedule.size());
			assertTrue(schedule.values().stream() //
					.allMatch(p -> p.state() == StateMachine.BALANCING));
		}
		{
			var schedule = simulator.gsc.eshsWithDifferentStates().get(1).getSchedule();
			assertEquals(52, schedule.size());
			assertTrue(schedule.values().stream() //
					.allMatch(p -> p.state() == Esh2State.FOO || p.state() == Esh2State.BAR));
		}
	}

	/**
	 * Gets the {@link GlobalSimulationsContext} {@link ThrowingSupplier} via Java
	 * Reflection.
	 * 
	 * @param optimizer the {@link Optimizer}
	 * @return the object
	 * @throws ReflectionException on error
	 */
	public static ThrowingSupplier<GlobalSimulationsContext, OpenemsException> getGlobalSimulationContextSupplier(
			Optimizer optimizer) throws ReflectionException {
		return getValueViaReflection(optimizer, "gscSupplier");
	}

}
