package io.openems.edge.io.kmtronic;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.io.kmtronic.KmtronicRelayOutput;

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
		KmtronicRelayOutput impl = new KmtronicRelayOutput();
		assertNotNull(impl);
	}

}
