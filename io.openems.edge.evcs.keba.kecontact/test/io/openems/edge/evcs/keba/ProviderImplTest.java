package io.openems.edge.evcs.keba;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.evcs.keba.kecontact.KebaKeContact;

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
		KebaKeContact impl = new KebaKeContact();
		assertNotNull(impl);
	}

}
