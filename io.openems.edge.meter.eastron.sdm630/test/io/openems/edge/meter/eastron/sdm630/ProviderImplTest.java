package io.openems.edge.meter.eastron.sdm630;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.meter.eastron.sdm630.MeterEastronSDM630;

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
		MeterEastronSDM630 impl = new MeterEastronSDM630();
		assertNotNull(impl);
	}

}
