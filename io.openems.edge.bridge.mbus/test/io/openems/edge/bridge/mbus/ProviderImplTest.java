package io.openems.edge.bridge.mbus;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.bridge.mbus.BridgeMbusImpl;

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
		BridgeMbusImpl impl = new BridgeMbusImpl();
		assertNotNull(impl);
	}

}
