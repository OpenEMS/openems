package io.openems.common.utils;

import org.junit.Assert;
import org.junit.Test;

import io.openems.common.utils.IntUtils.Round;

public class IntUtilsTest {

	@Test
	public void testRoundToPrecision() {
		Assert.assertEquals(1300, IntUtils.roundToPrecision(1300, Round.AWAY_FROM_ZERO, 100));

		Assert.assertEquals(1400, IntUtils.roundToPrecision(1301, Round.AWAY_FROM_ZERO, 100));

		Assert.assertEquals(1300, IntUtils.roundToPrecision(1300, Round.AWAY_FROM_ZERO, 10));

		Assert.assertEquals(1300, IntUtils.roundToPrecision(1300, Round.TOWARDS_ZERO, 100));

		Assert.assertEquals(1200, IntUtils.roundToPrecision(1299, Round.TOWARDS_ZERO, 100));

		Assert.assertEquals(10000, IntUtils.roundToPrecision(1, Round.AWAY_FROM_ZERO, 10000));

		Assert.assertEquals(0, IntUtils.roundToPrecision(9999, Round.TOWARDS_ZERO, 10000));

		Assert.assertEquals(4992, IntUtils.roundToPrecision(5000, Round.TOWARDS_ZERO, 52));

		Assert.assertEquals(5044, IntUtils.roundToPrecision(5000, Round.AWAY_FROM_ZERO, 52));

		Assert.assertEquals(-300, IntUtils.roundToPrecision(-310, Round.TOWARDS_ZERO, 100));
	}

}
