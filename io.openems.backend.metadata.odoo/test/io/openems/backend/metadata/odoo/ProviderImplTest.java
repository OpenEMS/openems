package io.openems.backend.metadata.odoo;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.metadata.odoo.OdooProvider;

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
		OdooProvider impl = new OdooProvider();
		assertNotNull(impl);
	}

}
