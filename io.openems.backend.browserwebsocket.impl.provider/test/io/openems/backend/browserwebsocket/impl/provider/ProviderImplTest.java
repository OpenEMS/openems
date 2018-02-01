package io.openems.backend.browserwebsocket.impl.provider;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.browserwebsocket.impl.provider.BrowserWebsocket;

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
		BrowserWebsocket impl = new BrowserWebsocket();
		assertNotNull(impl);
	}

}
