package io.openems.backend.browserwebsocket.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.browserwebsocket.impl.BrowserWebsocketImpl;

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
		BrowserWebsocketImpl impl = new BrowserWebsocketImpl();
		assertNotNull(impl);
	}

}
