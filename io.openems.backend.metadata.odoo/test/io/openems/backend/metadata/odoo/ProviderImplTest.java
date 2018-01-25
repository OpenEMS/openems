package io.openems.backend.metadata.odoo;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.metadata.odoo.OdooImpl;

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
		OdooImpl impl = new OdooImpl();
		assertNotNull(impl);
	}

}
