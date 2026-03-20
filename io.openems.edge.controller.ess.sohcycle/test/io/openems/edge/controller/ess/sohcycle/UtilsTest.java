package io.openems.edge.controller.ess.sohcycle;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testRound2() {
		float value = 100.20238494873047f;
		var roundedValue = Utils.round2(value);
		assertEquals(100.2f, roundedValue, 0f);
	}

}