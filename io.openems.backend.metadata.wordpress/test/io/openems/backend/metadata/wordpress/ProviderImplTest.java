package io.openems.backend.metadata.energydepot;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.metadata.energydepot.EnergyDepot;

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
		EnergyDepot impl = new EnergyDepot();
		assertNotNull(impl);
	}

}
