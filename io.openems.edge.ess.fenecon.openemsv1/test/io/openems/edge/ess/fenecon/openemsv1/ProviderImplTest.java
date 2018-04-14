package io.openems.edge.ess.fenecon.openemsv1;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.ess.fenecon.openemsv1.OpenemsV1;

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
		OpenemsV1 impl = new OpenemsV1();
		assertNotNull(impl);
	}

}
