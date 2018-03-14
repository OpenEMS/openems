package io.openems.backend.edgewebsocket.provider;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.edgewebsocket.impl.provider.EdgeWebsocket;

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
		EdgeWebsocket impl = new EdgeWebsocket();
		assertNotNull(impl);
	}

}
