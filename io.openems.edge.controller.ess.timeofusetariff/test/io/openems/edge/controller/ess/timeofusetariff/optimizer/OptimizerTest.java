package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.CLOCK;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getOptimizer;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest;

public class OptimizerTest {

	@Test
	public void testEmpty() throws Exception {
		var sut = getOptimizer(TimeOfUseTariffControllerImplTest.create(CLOCK));
		assertNull(sut.getParams());
		assertNull(sut.getCurrentStateMachine());
		assertTrue(sut.getSchedule().isEmpty());
	}

}
