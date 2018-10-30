package io.openems.backend.metadata.wordpress;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.metadata.wordpress.Wordpress;

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
		Wordpress impl = new Wordpress();
		assertNotNull(impl);
	}

}
