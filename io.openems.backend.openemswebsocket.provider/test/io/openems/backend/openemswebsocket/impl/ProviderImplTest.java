package io.openems.backend.openemswebsocket.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.openemswebsocket.provider.OpenemsWebsocketProvider;

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
		OpenemsWebsocketProvider impl = new OpenemsWebsocketProvider();
		assertNotNull(impl);
	}

}
