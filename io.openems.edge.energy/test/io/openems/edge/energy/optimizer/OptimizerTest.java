package io.openems.edge.energy.optimizer;

import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.energy.EnergySchedulerImplTest.CLOCK;
import static io.openems.edge.energy.EnergySchedulerImplTest.getOptimizer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.EnergySchedulerImplTest;
import io.openems.edge.energy.LogVerbosity;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;

public class OptimizerTest {

	@Test
	public void test() throws Exception {
		var sut = EnergySchedulerImplTest.create(CLOCK);
		var optimizer = getOptimizer(sut);
		assertEquals("No Schedule available|PerQuarter:UNDEFINED", optimizer.debugLog());

		var gscSupplier = getGlobalSimulationContextSupplier(optimizer);
		var simulator = new Simulator(gscSupplier.get());
		optimizer.runOnce(simulator);

		assertEquals("ScheduledPeriods:96|PerQuarter:UNDEFINED", optimizer.debugLog());

		var sr = optimizer.getSimulationResult();
		assertEquals(1375977.5150000001, sr.cost(), 0.001);
		assertEquals(96, sr.periods().size());

		var ctrlEssTimeOfUseTariff0 = sr.schedules().entrySet().asList().get(0);
		var p = ctrlEssTimeOfUseTariff0.getKey().getCurrentPeriod();
		assertEquals(StateMachine.CHARGE_GRID, p.state());
	}

	@Test
	public void test2() {
		var simulator = SimulatorTest.DUMMY_SIMULATOR;
		var o = new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> simulator.gsc, //
				null);
		o.applyBestQuickSchedule(simulator);

		var schedule = ((EnergyScheduleHandler.WithDifferentStates<?, ?>) simulator.gsc.handlers().get(1))
				.getSchedule();

		assertEquals(52, schedule.size());

		assertTrue(schedule.values().stream() //
				.allMatch(p -> p.state() == StateMachine.BALANCING));
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
