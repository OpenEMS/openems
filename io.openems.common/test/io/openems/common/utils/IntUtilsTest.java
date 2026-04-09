package io.openems.common.utils;

import static io.openems.common.utils.IntUtils.fitWithin;
import static io.openems.common.utils.IntUtils.maxInt;
import static io.openems.common.utils.IntUtils.maxInteger;
import static io.openems.common.utils.IntUtils.minInt;
import static io.openems.common.utils.IntUtils.minInteger;
import static io.openems.common.utils.IntUtils.roundToPrecision;
import static io.openems.common.utils.IntUtils.sumInt;
import static io.openems.common.utils.IntUtils.sumInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;

import org.junit.Test;

import io.openems.common.utils.IntUtils.Round;

public class IntUtilsTest {

	@Test
	public void testRoundToPrecision() {
		assertEquals(1300, roundToPrecision(1300, Round.AWAY_FROM_ZERO, 100));
		assertEquals(1400, roundToPrecision(1301, Round.AWAY_FROM_ZERO, 100));
		assertEquals(1300, roundToPrecision(1300, Round.AWAY_FROM_ZERO, 10));
		assertEquals(1300, roundToPrecision(1300, Round.TOWARDS_ZERO, 100));
		assertEquals(1200, roundToPrecision(1299, Round.TOWARDS_ZERO, 100));
		assertEquals(10000, roundToPrecision(1, Round.AWAY_FROM_ZERO, 10000));
		assertEquals(0, roundToPrecision(9999, Round.TOWARDS_ZERO, 10000));
		assertEquals(4992, roundToPrecision(5000, Round.TOWARDS_ZERO, 52));
		assertEquals(5044, roundToPrecision(5000, Round.AWAY_FROM_ZERO, 52));
		assertEquals(-300, roundToPrecision(-310, Round.TOWARDS_ZERO, 100));
	}

	@Test
	public void testMinInt() {
		assertEquals(1, minInt(1, 2, null, 3, null));
		assertEquals(1, minInt(1, null, null));
	}

	@Test
	public void testMinInteger() {
		assertEquals(25, (int) minInteger(null, 25, null, 40, null));
		assertEquals(17, (int) minInteger(null, 17, 25, 40));
		assertEquals(34, (int) minInteger(null, 34, 40));
		assertNull(minInteger(null, null));
	}

	@Test
	public void testMaxInt() {
		assertEquals(3, maxInt(1, 2, null, 3, null));
		assertEquals(1, maxInt(1, null, null));
	}

	@Test
	public void testMaxInteger() {
		assertEquals(40, (int) maxInteger(null, 25, null, 40, null));
		assertEquals(40, (int) maxInteger(null, 17, 25, 40));
		assertEquals(40, (int) maxInteger(null, 34, 40));
		assertNull(maxInteger(null, null));
	}

	@Test
	public void testSumInt() {
		assertEquals(6, sumInt(1, 2, null, 3, null));
		assertEquals(1, sumInt(1, null, null));
	}

	@Test
	public void testSumInteger() {
		assertEquals(65, (int) sumInteger(null, 25, null, 40, null));
		assertEquals(65, (int) sumInteger(Arrays.asList(null, 25, null, 40, null)));

		assertEquals(82, (int) sumInteger(null, 17, 25, 40));
		assertEquals(74, (int) sumInteger(null, 34, 40));

		assertNull(sumInteger(null, null));
		assertNull(sumInteger(Arrays.asList(null, null)));
	}

	@Test
	public void testFitWithin() {
		assertThrows(IllegalArgumentException.class, () -> fitWithin(21, 20, 10));
		assertEquals(10, fitWithin(5, 15, 10));
	}
}
