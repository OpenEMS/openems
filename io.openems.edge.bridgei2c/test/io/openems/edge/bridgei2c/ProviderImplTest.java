package io.openems.edge.bridgei2c;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.bridgei2c.ProviderImpl;

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
		ProviderImpl impl = new ProviderImpl();
		assertNotNull(impl);
	}

}
