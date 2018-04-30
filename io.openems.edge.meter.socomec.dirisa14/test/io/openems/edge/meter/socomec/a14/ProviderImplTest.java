package io.openems.edge.meter.socomec.a14;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.meter.socomec.dirisa14.MeterSocomecDirisA14;

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
		MeterSocomecDirisA14 impl = new MeterSocomecDirisA14();
		assertNotNull(impl);
	}

}
