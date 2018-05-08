package io.openems.edge.controller.api.websocket;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.controller.api.websocket.WebsocketApi;

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
		WebsocketApi impl = new WebsocketApi();
		assertNotNull(impl);
	}

}
