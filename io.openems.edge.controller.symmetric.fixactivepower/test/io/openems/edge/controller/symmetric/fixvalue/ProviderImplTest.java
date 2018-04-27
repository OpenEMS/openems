package io.openems.edge.controller.symmetric.fixvalue;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.controller.symmetric.fixactivepower.SymmetricFixActivePower;

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
		SymmetricFixActivePower impl = new SymmetricFixActivePower();
		assertNotNull(impl);
	}

}
