package io.openems.backend.browserwebsocket.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.browserwebsocket.provider.BrowserWebsocketProvider;

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
		BrowserWebsocketProvider impl = new BrowserWebsocketProvider();
		assertNotNull(impl);
	}

}
