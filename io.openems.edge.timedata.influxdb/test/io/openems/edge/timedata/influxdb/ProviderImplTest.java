package io.openems.edge.timedata.influxdb;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.timedata.influxdb.InfluxTimedata;

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
		InfluxTimedata impl = new InfluxTimedata();
		assertNotNull(impl);
	}

}
