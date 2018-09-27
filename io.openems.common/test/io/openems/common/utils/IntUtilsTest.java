package io.openems.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.utils.IntUtils.Round;

public class IntUtilsTest {

	@Test
	public void testRoundToPrecision() {
		assertEquals(1300, IntUtils.roundToPrecision(1300, Round.AWAY_FROM_ZERO, 100));

		assertEquals(1400, IntUtils.roundToPrecision(1301, Round.AWAY_FROM_ZERO, 100));

		assertEquals(1300, IntUtils.roundToPrecision(1300, Round.AWAY_FROM_ZERO, 10));

		assertEquals(1300, IntUtils.roundToPrecision(1300, Round.TOWARDS_ZERO, 100));

		assertEquals(1200, IntUtils.roundToPrecision(1299, Round.TOWARDS_ZERO, 100));

		assertEquals(10000, IntUtils.roundToPrecision(1, Round.AWAY_FROM_ZERO, 10000));

		assertEquals(0, IntUtils.roundToPrecision(9999, Round.TOWARDS_ZERO, 10000));

		assertEquals(4992, IntUtils.roundToPrecision(5000, Round.TOWARDS_ZERO, 52));

		assertEquals(5044, IntUtils.roundToPrecision(5000, Round.AWAY_FROM_ZERO, 52));

		assertEquals(-300, IntUtils.roundToPrecision(-310, Round.TOWARDS_ZERO, 100));
	}

}
