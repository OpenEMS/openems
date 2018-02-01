package io.openems.backend.openemswebsocket.provider;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.openemswebsocket.impl.provider.OpenemsWebsocket;

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
		OpenemsWebsocket impl = new OpenemsWebsocket();
		assertNotNull(impl);
	}

}
