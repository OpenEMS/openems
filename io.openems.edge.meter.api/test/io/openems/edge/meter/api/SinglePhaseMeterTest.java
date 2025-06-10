package io.openems.edge.meter.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.test.TestUtils;
import io.openems.edge.meter.test.DummySinglePhaseElectricityMeter;

public class SinglePhaseMeterTest {

	@Test
	public void testCalculateSinglePhaseFromActivePower() {
		var sut = new DummySinglePhaseElectricityMeter("meter0") //
				.withPhase(SinglePhase.L1);

		// Without calculateSinglePhaseFromActivePower
		sut.withActivePower(4000);
		assertEquals(4000, sut.getActivePower().get().intValue());
		assertEquals(null, sut.getActivePowerL1().get());
		assertEquals(null, sut.getActivePowerL2().get());
		assertEquals(null, sut.getActivePowerL3().get());

		// Phase 1
		sut.withPhase(SinglePhase.L1);
		SinglePhaseMeter.calculateSinglePhaseFromActivePower(sut);
		sut.withActivePower(3000);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(3000, sut.getActivePower().get().intValue());
		assertEquals(3000, sut.getActivePowerL1().get().intValue());
		assertEquals(null, sut.getActivePowerL2().get());
		assertEquals(null, sut.getActivePowerL3().get());

		// Phase 2
		sut.withPhase(SinglePhase.L2);
		sut.withActivePower(2000);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(2000, sut.getActivePower().get().intValue());
		assertEquals(null, sut.getActivePowerL1().get());
		assertEquals(2000, sut.getActivePowerL2().get().intValue());
		assertEquals(null, sut.getActivePowerL3().get());

		// Phase 3
		sut.withPhase(SinglePhase.L3);
		sut.withActivePower(1000);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(1000, sut.getActivePower().get().intValue());
		assertEquals(null, sut.getActivePowerL1().get());
		assertEquals(null, sut.getActivePowerL2().get());
		assertEquals(1000, sut.getActivePowerL3().get().intValue());
	}
}
