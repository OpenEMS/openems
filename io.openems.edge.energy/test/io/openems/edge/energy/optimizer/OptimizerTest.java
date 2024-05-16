package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.EnergySchedulerImplTest.CLOCK;
import static io.openems.edge.energy.EnergySchedulerImplTest.getOptimizer;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.energy.EnergySchedulerImplTest;

public class OptimizerTest {

	@Test
	public void testEmpty() throws Exception {
		var sut = getOptimizer(EnergySchedulerImplTest.create(CLOCK));
		assertNull(sut.getParams());
		assertTrue(sut.getSchedule().isEmpty());
	}

}
