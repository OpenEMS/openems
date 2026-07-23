package io.openems.edge.bridge.modbus.sunspec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DefaultSunSpecModelTest {

	@Test
	public void test() {
		// This is just to test initialization of the enum
		var e = DefaultSunSpecModel.S_1;
		assertEquals("Common", e.label);
	}

}
