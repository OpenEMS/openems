package io.openems.edge.energy.v1.optimizer;

import static io.openems.edge.energy.v1.EnergySchedulerImplTest.CLOCK;
import static io.openems.edge.energy.v1.EnergySchedulerImplTest.getOptimizer;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.energy.v1.EnergySchedulerImplTest;

public class OptimizerV1Test {

	@Test
	public void testEmpty() throws Exception {
		var sut = getOptimizer(EnergySchedulerImplTest.create(CLOCK));
		assertNull(sut.getParams());
		assertTrue(sut.getSchedule().isEmpty());
	}

}
