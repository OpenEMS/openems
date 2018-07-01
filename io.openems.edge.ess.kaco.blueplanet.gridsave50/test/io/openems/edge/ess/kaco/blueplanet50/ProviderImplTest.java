package io.openems.edge.ess.kaco.blueplanet50;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.ess.kaco.blueplanet.gridsave50.EssKacoBlueplanetGridsave50;

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
		EssKacoBlueplanetGridsave50 impl = new EssKacoBlueplanetGridsave50();
		assertNotNull(impl);
	}

}
