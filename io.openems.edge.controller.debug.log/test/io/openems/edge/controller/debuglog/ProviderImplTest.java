package io.openems.edge.controller.debuglog;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.controller.debuglog.DebugLog;

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
		DebugLog impl = new DebugLog();
		assertNotNull(impl);
	}

}
