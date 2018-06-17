package io.openems.edge.ess.kaco.blueplanet50;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.ess.kaco.blueplanet50.EssKacoBlueplanet50;

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
		EssKacoBlueplanet50 impl = new EssKacoBlueplanet50();
		assertNotNull(impl);
	}

}
