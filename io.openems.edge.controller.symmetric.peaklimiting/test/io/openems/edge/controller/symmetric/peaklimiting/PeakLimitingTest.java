package io.openems.edge.controller.symmetric.peaklimiting;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.controller.symmetric.peaklimiting.PeakLimiting;

/*
 * Example JUNit test case
 *
 */

public class PeakLimitingTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		PeakLimiting impl = new PeakLimiting();
		assertNotNull(impl);
	}

}
