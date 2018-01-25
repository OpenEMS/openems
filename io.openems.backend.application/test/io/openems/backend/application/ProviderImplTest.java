package io.openems.backend.application;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.application.BackendApp;

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
		BackendApp impl = new BackendApp();
		assertNotNull(impl);
	}

}
