package io.openems.edge.timedata.influxdb;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.timedata.influxdb.Influxdb;

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
		Influxdb impl = new Influxdb();
		assertNotNull(impl);
	}

}
