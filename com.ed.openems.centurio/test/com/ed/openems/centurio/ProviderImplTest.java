package com.ed.openems.centurio;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.ed.openems.centurio.ess.CenturioEss;

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
		CenturioEss impl = new CenturioEss();
		assertNotNull(impl);
	}

}
