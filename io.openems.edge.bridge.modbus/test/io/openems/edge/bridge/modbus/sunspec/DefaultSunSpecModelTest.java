package io.openems.edge.bridge.modbus.sunspec;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultSunSpecModelTest {

	@Test
	public void test() {
		// This is just to test initialization of the enum
		var e = DefaultSunSpecModel.S_1;
		assertEquals("Common", e.label);
	}

}
