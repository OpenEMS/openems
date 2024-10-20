package io.openems.edge.energy.optimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.LogVerbosity;
import io.openems.edge.energy.api.EnergyScheduleHandler;

public class OptimizerTest {

	@Test
	public void test() {
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

}
