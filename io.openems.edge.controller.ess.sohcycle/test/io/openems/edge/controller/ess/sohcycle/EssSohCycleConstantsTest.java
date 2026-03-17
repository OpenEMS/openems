package io.openems.edge.controller.ess.sohcycle;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EssSohCycleConstantsTest {

	@Test
	public void testDefaultValues() {
		assertEquals(100, EssSohCycleConstants.MAX_SOC);
		assertEquals(0, EssSohCycleConstants.MIN_SOC);
		assertEquals(100, EssSohCycleConstants.MAX_CELL_VOLTAGE_DIFFERENCE_MV);
	}
}

