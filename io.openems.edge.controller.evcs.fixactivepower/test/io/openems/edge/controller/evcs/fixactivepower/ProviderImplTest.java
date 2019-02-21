package io.openems.edge.controller.evcs.fixactivepower;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.controller.evcs.fixactivepower.EvcsFixActivePower;

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
		EvcsFixActivePower impl = new EvcsFixActivePower();
		assertNotNull(impl);
	}

}
