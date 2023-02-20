package io.openems.edge.io.hal.pi4j.pi4j;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class Pi4JTest extends AbstractHardwareTest {

	@Test
	public void pi4jTest() {
		assertNotNull(this.pi4j);
	}
}
