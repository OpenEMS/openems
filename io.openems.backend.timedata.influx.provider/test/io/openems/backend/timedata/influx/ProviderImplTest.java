package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.timedata.influx.Influx;

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
		Influx impl = new Influx();
		assertNotNull(impl);
	}

}
