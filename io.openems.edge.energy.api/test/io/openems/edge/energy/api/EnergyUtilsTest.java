package io.openems.edge.energy.api;

import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static io.openems.edge.energy.api.EnergyUtils.toPower;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EnergyUtilsTest {

	@Test
	public void testFindFirstPeakIndex() {
		assertEquals(0, findFirstPeakIndex(0, new double[0]));
		assertEquals(0, findFirstPeakIndex(0, new double[] { 1 }));
		assertEquals(0, findFirstPeakIndex(0, new double[] { 1, 0 }));
		assertEquals(1, findFirstPeakIndex(0, new double[] { 0, 1, 0 }));
		assertEquals(1, findFirstPeakIndex(0, new double[] { 0, 1, 0, 1 }));
		assertEquals(5, findFirstPeakIndex(5, new double[0]));
	}

	@Test
	public void testFindFirstValleyIndex() {
		assertEquals(0, findFirstValleyIndex(0, new double[0]));
		assertEquals(0, findFirstValleyIndex(0, new double[] { 1 }));
		assertEquals(1, findFirstValleyIndex(0, new double[] { 1, 0 }));
		assertEquals(0, findFirstValleyIndex(0, new double[] { 0, 1, 0 }));
		assertEquals(2, findFirstValleyIndex(1, new double[] { 0, 1, 0, 1 }));
		assertEquals(5, findFirstValleyIndex(5, new double[0]));
	}

	@Test
	public void testToPower() {
		assertEquals(2000, (int) toPower(500));
		assertNull(toPower(null));
	}
}
