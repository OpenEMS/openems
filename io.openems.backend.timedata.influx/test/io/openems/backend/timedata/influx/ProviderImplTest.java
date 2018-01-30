package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.backend.timedata.influx.InfluxProvider;

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
		InfluxProvider impl = new InfluxProvider();
		assertNotNull(impl);
	}

}
