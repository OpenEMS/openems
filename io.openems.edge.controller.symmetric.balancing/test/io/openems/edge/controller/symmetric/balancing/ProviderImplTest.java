package io.openems.edge.controller.symmetric.balancing;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.controller.symmetric.balancing.Balancing;

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
		Balancing impl = new Balancing();
		assertNotNull(impl);
	}

}
