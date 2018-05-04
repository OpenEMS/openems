package io.openems.edge.simulator;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.simulator.meter.grid.acting.GridMeter;

/*
 * Example JUNit test case
 *
 */

public class ProviderImplTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		GridMeter impl = new GridMeter();
		assertNotNull(impl);
	}

}
