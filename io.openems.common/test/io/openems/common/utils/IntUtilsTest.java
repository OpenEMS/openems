package io.openems.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.utils.IntUtils.Round;

public class IntUtilsTest {

	@Test
	public void testRoundToPrecision() {
		assertEquals(1300, IntUtils.roundToPrecision(1300, Round.UP, 100));

		assertEquals(1400, IntUtils.roundToPrecision(1301, Round.UP, 100));

		assertEquals(1300, IntUtils.roundToPrecision(1300, Round.UP, 10));

		assertEquals(1300, IntUtils.roundToPrecision(1300, Round.DOWN, 100));

		assertEquals(1200, IntUtils.roundToPrecision(1299, Round.DOWN, 100));
		
		assertEquals(10000, IntUtils.roundToPrecision(1, Round.UP, 10000));
		
		assertEquals(0, IntUtils.roundToPrecision(9999, Round.DOWN, 10000));
	}

}
