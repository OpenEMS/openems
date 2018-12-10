package io.openems.edge.pvinverter.solarlog;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.pvinverter.solarlog.SolarLog;

/*
 * Example JUNit test case
 *
 */

public class SolarLogTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		SolarLog impl = new SolarLog();
		assertNotNull(impl);
	}

}
